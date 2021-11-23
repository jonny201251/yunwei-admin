package com.sss.yunweiadmin.controller;


import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.SecureUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.base.Strings;
import com.sss.yunweiadmin.common.operate.OperateLog;
import com.sss.yunweiadmin.common.result.ResponseResult;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.common.utils.TreeUtil;
import com.sss.yunweiadmin.model.entity.*;
import com.sss.yunweiadmin.model.excel.ExcelListener;
import com.sss.yunweiadmin.model.excel.SysUserExcel;
import com.sss.yunweiadmin.model.vo.RoleGiveVO;
import com.sss.yunweiadmin.model.vo.UserVO;
import com.sss.yunweiadmin.model.vo.ValueLabelVO;
import com.sss.yunweiadmin.service.*;
import lombok.SneakyThrows;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * 用户表 前端控制器
 * </p>
 *
 * @author 任勇林
 * @since 2021-04-09
 */
@RestController
@RequestMapping("/sysUser")
@ResponseResultWrapper
public class SysUserController {
    @Autowired
    SysUserService sysUserService;
    @Autowired
    SysRoleUserService sysRoleUserService;
    @Autowired
    SysRolePermissionService sysRolePermissionService;
    @Autowired
    SysDeptService sysDeptService;
    @Autowired
    SysRoleService sysRoleService;
    @Autowired
    SysPermissionService sysPermissionService;
    @Autowired
    HttpSession httpSession;
    @Autowired
    ProcessDefinitionService processDefinitionService;

    @GetMapping("list")
    public IPage<SysUser> list(int currentPage, int pageSize, String loginName, String displayName, Integer deptId) {
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        if (!Strings.isNullOrEmpty(loginName)) {
            queryWrapper.like("login_name", loginName);
        }
        if (!Strings.isNullOrEmpty(displayName)) {
            queryWrapper.like("display_name", displayName);
        }
        if (deptId != null) {
            queryWrapper.eq("dept_id", deptId);
        }
        return sysUserService.page(new Page<>(currentPage, pageSize), queryWrapper);
    }

    @OperateLog(module = "用户模块", type = "添加用户")
    @PostMapping("add")
    public boolean add(@RequestBody SysUser sysUser) {
        return sysUserService.add(sysUser);
    }

    @OperateLog(module = "用户模块", type = "编辑用户")
    @PostMapping("edit")
    public boolean edit(@RequestBody SysUser sysUser) {
        return sysUserService.updateById(sysUser);
    }

    @GetMapping("get")
    public SysUser getById(String id) {
        return sysUserService.getById(id);
    }

    @OperateLog(module = "用户模块", type = "删除用户")
    @GetMapping("delete")
    public boolean delete(Integer[] idArr) {
        return sysUserService.delete(idArr);
    }

    @OperateLog(module = "用户模块", type = "登录")
    @GetMapping("login")
    public UserVO login(String loginName, String password) {
        UserVO userVO = new UserVO();
        //根据 登录账号 查询出用户
        List<SysUser> userList = sysUserService.list(new QueryWrapper<SysUser>().eq("login_name", loginName));
        if (userList.size() != 1) {
            throw new RuntimeException("用户名错误");
        }
        SysUser dbUser = userList.get(0);
        //校验 登录密码
        String dbPassword = dbUser.getPassword();
        String pagePassword = SecureUtil.md5(password);
        if (!dbPassword.equals(pagePassword)) {
            throw new RuntimeException("密码错误");
        }
        //根据用户获取角色
        List<SysRoleUser> roleUserList = sysRoleUserService.list(new QueryWrapper<SysRoleUser>().eq("user_id", dbUser.getId()));
        if (ObjectUtil.isEmpty(roleUserList)) {
            throw new RuntimeException("用户没有分配角色");
        }
        List<Integer> roleIdList = roleUserList.stream().map(SysRoleUser::getRoleId).collect(Collectors.toList());
        //根据角色获取权限
        List<SysRolePermission> rolePermissionList = sysRolePermissionService.list(new QueryWrapper<SysRolePermission>().in("role_id", roleIdList));
        if (ObjectUtil.isEmpty(rolePermissionList)) {
            throw new RuntimeException("用户没有分配菜单");
        }
        //根据权限获取完整的权限
        List<Integer> permissionIdList = rolePermissionList.stream().map(SysRolePermission::getPermissionId).collect(Collectors.toList());
        List<SysPermission> permissionList = sysPermissionService.list(new QueryWrapper<SysPermission>().in("id", permissionIdList));
        List<SysPermission> allPermissionList = new ArrayList<SysPermission>();
        while (true) {
            if (ObjectUtil.isEmpty(permissionList)) {
                break;
            }
            allPermissionList.addAll(permissionList);
            //
            List<Integer> parentPermissionIdList = permissionList.stream().map(SysPermission::getPid).collect(Collectors.toList());
            permissionList = sysPermissionService.list(new QueryWrapper<SysPermission>().in("id", parentPermissionIdList));
        }
        //
        List<Integer> allPermissionIdList = allPermissionList.stream().map(SysPermission::getId).collect(Collectors.toList());
        List<SysPermission> permissionList1 = sysPermissionService.list(new QueryWrapper<SysPermission>().in("id", allPermissionIdList).orderByAsc("sort"));
        List<SysPermission> permissionList2 = sysPermissionService.list(new QueryWrapper<SysPermission>().in("id", allPermissionIdList).orderByAsc("sort"));
        List<SysPermission> permissionList3 = sysPermissionService.list(new QueryWrapper<SysPermission>().in("id", allPermissionIdList).orderByAsc("sort"));
        List<SysPermission> permissionList4 = sysPermissionService.list(new QueryWrapper<SysPermission>().in("id", allPermissionIdList).orderByAsc("sort"));
        //导航菜单
        List<SysPermission> menuList = TreeUtil.getTreeSelect(permissionList1.stream().filter(item -> item.getType().equals("菜单") || item.getType().equals("叶子菜单")).collect(Collectors.toList()));
        //操作按钮-按钮组
        Map<String, List<SysPermission>> operateButtonMap = new HashMap<>();
        List<SysPermission> operateButtonList = TreeUtil.getTreeSelect(permissionList2.stream().filter(item -> item.getType().equals("叶子菜单") || "按钮组".equals(item.getPosition())).collect(Collectors.toList()));
        for (SysPermission sysPermission : operateButtonList) {
            if (ObjectUtil.isNotEmpty(sysPermission.getChildren())) {
                operateButtonMap.put(sysPermission.getPath(), sysPermission.getChildren());
            }
        }
        //数据列表-按钮
        Map<String, List<SysPermission>> dataListButtonMap = new HashMap<>();
        List<SysPermission> dataListButtonList = TreeUtil.getTreeSelect(permissionList3.stream().filter(item -> item.getType().equals("叶子菜单") || ("数据列表".equals(item.getPosition()) && !item.getPermissionType().equals("startProcess"))).collect(Collectors.toList()));
        for (SysPermission sysPermission : dataListButtonList) {
            if (ObjectUtil.isNotEmpty(sysPermission.getChildren())) {
                dataListButtonMap.put(sysPermission.getPath(), sysPermission.getChildren());
            }
        }
        //数据列表-发起流程按钮
        Map<Integer, SysPermission> startProcessButtonMap = new HashMap<>();
        SysPermission permission = sysPermissionService.getOne(new QueryWrapper<SysPermission>().eq("permission_type", "startProcess"));
        /*
            1.根据用户取出角色
            2.根据角色获取流程定义
         */
        List<SysRole> roleList = sysRoleService.listByIds(roleIdList);
        List<String> roleNameList = roleList.stream().map(SysRole::getName).collect(Collectors.toList());
        List<ProcessDefinition> definitionList = processDefinitionService.list();
        for (ProcessDefinition processDefinition : definitionList) {
            List<String> definitionRoleNameList = Stream.of(processDefinition.getRoleName().split(",")).collect(Collectors.toList());
            //判断
            definitionRoleNameList.retainAll(roleNameList);
            if (ObjectUtil.isNotEmpty(definitionRoleNameList)) {
                startProcessButtonMap.put(processDefinition.getId(), permission);
            }
        }
        //查询
        Map<String, SysPermission> queryMap = new HashMap<>();
        List<SysPermission> queryList = TreeUtil.getTreeSelect(permissionList4.stream().filter(item -> item.getType().equals("叶子菜单") || "query".equals(item.getPermissionType())).collect(Collectors.toList()));
        for (SysPermission sysPermission : queryList) {
            if (ObjectUtil.isNotEmpty(sysPermission.getChildren())) {
                queryMap.put(sysPermission.getPath(), sysPermission.getChildren().get(0));
            }
        }
        //
        httpSession.removeAttribute("user");
        httpSession.setAttribute("user", dbUser);
        userVO.setUser(dbUser);
        userVO.setMenuList(menuList);
        userVO.setOperateButtonMap(operateButtonMap);
        userVO.setDataListButtonMap(dataListButtonMap);
        userVO.setStartProcessButtonMap(startProcessButtonMap);
        userVO.setQueryMap(queryMap);
        return userVO;
    }

    @GetMapping("logout")
    public boolean logout() {
        httpSession.removeAttribute("user");
        return true;
    }

    @GetMapping("getNameStr")
    public ResponseResult getNameStr(Integer[] idArr) {
        List<Integer> idList = Stream.of(idArr).collect(Collectors.toList());
        //查询部门
        List<SysDept> deptList = sysDeptService.list();
        Map<Integer, String> deptMap = deptList.stream().collect(Collectors.toMap(SysDept::getId, SysDept::getName));

        List<SysUser> userList = sysUserService.listByIds(idList);
        String nameStr = userList.stream().map(user -> deptMap.get(user.getDeptId()) + "[" + user.getDisplayName() + "]").collect(Collectors.joining(","));
        return ResponseResult.success(nameStr);
    }

    @GetMapping("getUserVL")
    public List<ValueLabelVO> getUserVL() {
        List<ValueLabelVO> list = new ArrayList<>();
        List<SysUser> userList = sysUserService.list();
        List<SysDept> deptList = sysDeptService.list();
        Map<Integer, String> deptMap = deptList.stream().collect(Collectors.toMap(SysDept::getId, SysDept::getName));
        return userList.stream().map(user -> new ValueLabelVO(user.getId() + "." + user.getDisplayName() + "." + deptMap.get(user.getDeptId()), user.getDisplayName() + "." + deptMap.get(user.getDeptId()))).collect(Collectors.toList());
    }


    //超级管理员，重置密码

    //用户自己，修改密码
    @GetMapping("changePassword")
    public boolean changePassword(String oldPassword, String newPassword) {
        SysUser user = (SysUser) httpSession.getAttribute("user");
        if (!user.getPassword().equals(SecureUtil.md5(oldPassword))) {
            throw new RuntimeException("旧密码输入错误");
        }
        user.setPassword(SecureUtil.md5(newPassword));
        return sysUserService.updateById(user);
    }


    //反显-角色分配
    @GetMapping("getRoleGiveVO")
    public RoleGiveVO getRoleGiveVO(Integer userId) {
        List<ValueLabelVO> roleList = sysRoleService.list().stream().map(item -> new ValueLabelVO(item.getId(), item.getName())).collect(Collectors.toList());
        List<Integer> checkRoleIdList = sysRoleUserService.list(new QueryWrapper<SysRoleUser>().eq("user_id", userId)).stream().map(SysRoleUser::getRoleId).collect(Collectors.toList());

        RoleGiveVO roleGiveVO = new RoleGiveVO();
        roleGiveVO.setRoleList(roleList);
        roleGiveVO.setCheckRoleIdList(checkRoleIdList);
        return roleGiveVO;
    }

    //修改-角色分配
    @GetMapping("roleGive")
    public boolean roleGive(Integer userId, Integer[] roleIdArr) {
        List<Integer> roleIdList = Stream.of(roleIdArr).collect(Collectors.toList());
        return sysUserService.updateRoleUser(userId, roleIdList);
    }

    //检查是否已登录,或者登录过期
    @GetMapping("/checkUser")
    public SysUser checkUser() {
        //取出登录用户
        SysUser user = (SysUser) httpSession.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }
        return user;
    }


    //下载用户模板
    @GetMapping("download1")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("用户模板", "UTF-8");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xls");
        //
        ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).useDefaultStyle(false).excelType(ExcelTypeEnum.XLS).build();
        //
        List<SysUserExcel> data0List = new ArrayList<>();
        WriteSheet sheet0 = EasyExcel.writerSheet(0, "用户信息").head(SysUserExcel.class).build();
        //
        excelWriter.write(data0List, sheet0);
        //
        excelWriter.finish();
    }

    //上传用户
    @PostMapping("upload1")
    @SneakyThrows
    public List<String> importAsset(MultipartFile[] files, String formValue) {
        List<String> resultList = new ArrayList<>();
        //
        MultipartFile file = files[0];
        InputStream inputStream = file.getInputStream();
        //
        ExcelReader excelReader = EasyExcel.read(inputStream).build();
        //
        ExcelListener<SysUserExcel> listener0 = new ExcelListener<>();
        //获取sheet对象
        ReadSheet sheet0 = EasyExcel.readSheet(0).head(SysUserExcel.class).registerReadListener(listener0).build();
        //读取数据
        excelReader.read(sheet0);
        //获取数据
        List<SysUserExcel> list0 = listener0.getData();
        //
        if (ObjectUtil.isNotEmpty(list0)) {
            List<SysUser> userList = new ArrayList<>();
            for (SysUserExcel sysUserExcel : list0) {
                SysUser user = new SysUser();
                BeanUtils.copyProperties(sysUserExcel, user);
                //设置默认密码
                user.setPassword(SecureUtil.md5("123"));
                userList.add(user);
            }
            for (SysUser user : userList) {
                sysUserService.add(user);
            }
            resultList.add(userList.size() + "条用户被导入");
        } else {
            resultList.add("无用户被导入");
        }
        return resultList;
    }
}
