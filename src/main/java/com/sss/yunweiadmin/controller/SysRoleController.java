package com.sss.yunweiadmin.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sss.yunweiadmin.common.result.ResponseResult;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.common.utils.TreeUtil;
import com.sss.yunweiadmin.model.entity.SysPermission;
import com.sss.yunweiadmin.model.entity.SysRole;
import com.sss.yunweiadmin.model.entity.SysRolePermission;
import com.sss.yunweiadmin.model.entity.SysRoleUser;
import com.sss.yunweiadmin.model.vo.KeyTitleVO;
import com.sss.yunweiadmin.model.vo.PermissionGiveVO;
import com.sss.yunweiadmin.model.vo.TreeSelectVO;
import com.sss.yunweiadmin.model.vo.ValueLabelVO;
import com.sss.yunweiadmin.service.SysPermissionService;
import com.sss.yunweiadmin.service.SysRolePermissionService;
import com.sss.yunweiadmin.service.SysRoleService;
import com.sss.yunweiadmin.service.SysRoleUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * 角色表 前端控制器
 * </p>
 *
 * @author 任勇林
 * @since 2021-04-09
 */
@RestController
@RequestMapping("/sysRole")
@ResponseResultWrapper
public class SysRoleController {
    @Autowired
    SysRoleService sysRoleService;
    @Autowired
    SysPermissionService sysPermissionService;
    @Autowired
    SysRolePermissionService sysRolePermissionService;
    @Autowired
    SysRoleUserService sysRoleUserService;

    @GetMapping("list")
    public IPage<SysRole> list(int currentPage, int pageSize) {
        return sysRoleService.page(new Page<>(currentPage, pageSize), new QueryWrapper<SysRole>().ne("name", "提交人领导"));
    }

    @PostMapping("add")
    public boolean add(@RequestBody SysRole sysRole) {
        return sysRoleService.save(sysRole);
    }

    @GetMapping("get")
    public SysRole getById(String id) {
        return sysRoleService.getById(id);
    }

    @PostMapping("edit")
    public boolean edit(@RequestBody SysRole sysRole) {
        return sysRoleService.updateById(sysRole);
    }

    @GetMapping("delete")
    public boolean delete(Integer[] idArr) {
        List<Integer> idList = Stream.of(idArr).collect(Collectors.toList());
        return sysRoleService.removeByIds(idList);
    }

    @GetMapping("getRoleVL")
    public List<ValueLabelVO> getRoleVL() {
        List<SysRole> list = sysRoleService.list();
        return list.stream().map(item -> new ValueLabelVO(item.getId(), item.getName())).collect(Collectors.toList());
    }

    @GetMapping("getRoleNameVL")
    public List<ValueLabelVO> getRoleNameVL() {
        List<SysRole> list = sysRoleService.list();
        return list.stream().map(item -> new ValueLabelVO(item.getName(), item.getName())).collect(Collectors.toList());
    }

    @GetMapping("getRoleKT")
    public List<KeyTitleVO> getRoleKT() {
        List<SysRole> list = sysRoleService.list();
        return list.stream().map(item -> new KeyTitleVO(item.getId(), item.getName())).collect(Collectors.toList());
    }

    @GetMapping("getRoleNameStr")
    public ResponseResult getRoleNameStr(Integer[] idArr) {
        List<Integer> idList = Stream.of(idArr).collect(Collectors.toList());
        List<SysRole> list = sysRoleService.list(new QueryWrapper<SysRole>().in("id", idList));
        List<SysRoleUser> roleUserList = sysRoleUserService.list(new QueryWrapper<SysRoleUser>().in("role_id", list.stream().map(SysRole::getId).collect(Collectors.toList())));
        Set<Integer> roleIdSet = roleUserList.stream().map(SysRoleUser::getRoleId).collect(Collectors.toSet());
        if (list.size() != roleIdSet.size()) {
            throw new RuntimeException("所选角色没有绑定用户");
        }
        String roleName = list.stream().map(SysRole::getName).collect(Collectors.joining(","));
        return ResponseResult.success(roleName);
    }

    //反显-权限分配
    @GetMapping("getPermissionGiveVO")
    public PermissionGiveVO getPermissionGiveVO(Integer roleId) {
        List<SysPermission> list = sysPermissionService.list(new QueryWrapper<SysPermission>().orderByAsc("sort"));
        List<TreeSelectVO> permissionList = TreeUtil.getTreeSelectVO(list);
        List<Integer> checkPermissionIdList = sysRolePermissionService.list(new QueryWrapper<SysRolePermission>().eq("role_id", roleId)).stream().map(SysRolePermission::getPermissionId).collect(Collectors.toList());

        PermissionGiveVO permissionGiveVO = new PermissionGiveVO();
        permissionGiveVO.setPermissionList(permissionList);
        permissionGiveVO.setCheckPermissionIdList(checkPermissionIdList);
        return permissionGiveVO;
    }

    //权限分配
    @GetMapping("permissionGive")
    public boolean permissionGive(Integer roleId, Integer[] permissionIdArr) {
        List<Integer> permissionIdList = Stream.of(permissionIdArr).collect(Collectors.toList());
        return sysRoleService.updateRolePermission(roleId, permissionIdList);
    }
}
