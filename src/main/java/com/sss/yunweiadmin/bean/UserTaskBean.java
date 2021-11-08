package com.sss.yunweiadmin.bean;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sss.yunweiadmin.model.entity.ProcessDefinitionTask;
import com.sss.yunweiadmin.model.entity.SysRoleUser;
import com.sss.yunweiadmin.model.entity.SysUser;
import com.sss.yunweiadmin.model.vo.NextUserVO;
import com.sss.yunweiadmin.service.ProcessDefinitionEdgeService;
import com.sss.yunweiadmin.service.ProcessDefinitionTaskService;
import com.sss.yunweiadmin.service.SysRoleUserService;
import com.sss.yunweiadmin.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserTaskBean {
    @Autowired
    ProcessDefinitionTaskService processDefinitionTaskService;
    @Autowired
    ProcessDefinitionEdgeService processDefinitionEdgeService;
    @Autowired
    HttpSession httpSession;
    @Autowired
    SysUserService sysUserService;
    @Autowired
    SysRoleUserService sysRoleUserService;

    public List<SysUser> getUserList(Integer processDefinitionId, String preTaskId, String currentTaskId) {
        ProcessDefinitionTask preTask = processDefinitionTaskService.getOne(new QueryWrapper<ProcessDefinitionTask>().eq("process_definition_id", processDefinitionId).eq("task_id", preTaskId));
        ProcessDefinitionTask currentTask = processDefinitionTaskService.getOne(new QueryWrapper<ProcessDefinitionTask>().eq("process_definition_id", processDefinitionId).eq("task_id", currentTaskId));
        if (preTask.getHaveNextUser().equals("是")) {
            NextUserVO nextUserVO = (NextUserVO) httpSession.getAttribute("nextUserVO");
            return getUserList(nextUserVO.getType(), nextUserVO.getTypeValue(), nextUserVO.getHaveStarterDept());
        } else {
            return getUserList(currentTask.getType(), currentTask.getTypeValue(), currentTask.getHaveStarterDept());
        }
    }

    public List<SysUser> getUserList(String type, String typeValue, String haveStarterDept) {
        List<SysUser> userList;
        if (type.equals("角色")) {
            if (ObjectUtil.isNotEmpty(haveStarterDept)) {
                /*
                    提交人部门
                    根据提交人获取部门所有用户，接着查询出所有用户的角色，最后根据页面角色，帅选出有效用户
                 */
                SysUser currentUser = (SysUser) httpSession.getAttribute("user");
                List<SysUser> userTmp = sysUserService.list(new QueryWrapper<SysUser>().eq("dept_id", currentUser.getDeptId()));
                List<SysRoleUser> roleUserList = sysRoleUserService.list(new QueryWrapper<SysRoleUser>().in("role_id", Arrays.asList(typeValue.split(","))).in("user_id", userTmp.stream().map(SysUser::getId).collect(Collectors.toList())));
                userList = sysUserService.listByIds(roleUserList.stream().map(SysRoleUser::getUserId).collect(Collectors.toList()));
            } else {
                List<SysRoleUser> roleUserList = sysRoleUserService.list(new QueryWrapper<SysRoleUser>().in("role_id", Arrays.asList(typeValue.split(","))));
                userList = sysUserService.listByIds(roleUserList.stream().map(SysRoleUser::getUserId).collect(Collectors.toList()));
            }
        } else {
            List<SysRoleUser> roleUserList = sysRoleUserService.list(new QueryWrapper<SysRoleUser>().in("user_id", Arrays.asList(typeValue.split(","))));
            userList = sysUserService.listByIds(roleUserList.stream().map(SysRoleUser::getUserId).collect(Collectors.toList()));
        }
        return userList;
    }
}
