package com.sss.yunweiadmin.common.activiti;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sss.yunweiadmin.bean.UserTaskBean;
import com.sss.yunweiadmin.common.utils.SpringUtil;
import com.sss.yunweiadmin.model.entity.ProcessDefinitionTask;
import com.sss.yunweiadmin.model.entity.ProcessInstanceData;
import com.sss.yunweiadmin.model.entity.ProcessInstanceNode;
import com.sss.yunweiadmin.model.entity.SysUser;
import com.sss.yunweiadmin.model.vo.NextUserVO;
import com.sss.yunweiadmin.service.ProcessDefinitionTaskService;
import com.sss.yunweiadmin.service.ProcessInstanceDataService;
import com.sss.yunweiadmin.service.ProcessInstanceNodeService;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.delegate.event.ActivitiEntityEvent;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import java.util.List;

//给userTask设置处理人
@Component
@Slf4j
public class ActEventListener implements ActivitiEventListener {
    @Autowired
    HttpSession httpSession;
    //这里无法注入自己定义的service,所以使用了SpringUtil
    //@Autowired
    //ProcessInstanceDataService processInstanceDataService

    @Override
    public void onEvent(ActivitiEvent activitiEvent) {
        if (activitiEvent.getType().equals(ActivitiEventType.TASK_CREATED)) {
            ActivitiEntityEvent entityEvent = (ActivitiEntityEvent) activitiEvent;
            TaskEntity taskEntity = (TaskEntity) entityEvent.getEntity();
            //入网流程_10:1:5003
            String actProcessDefinitionId = taskEntity.getProcessDefinitionId();
            //10
            Integer processDefinitionId = Integer.parseInt(actProcessDefinitionId.split(":")[0].split("_")[1]);
            //
            String actProcessInstanceId = taskEntity.getProcessInstanceId();
            //Task_3in0qiu
            String taskId = taskEntity.getTaskDefinitionKey();
            //
            ProcessInstanceDataService processInstanceDataService = SpringUtil.getBean(ProcessInstanceDataService.class);
            ProcessInstanceNodeService processInstanceNodeService = SpringUtil.getBean(ProcessInstanceNodeService.class);
            ProcessDefinitionTaskService processDefinitionTaskService = SpringUtil.getBean(ProcessDefinitionTaskService.class);
            UserTaskBean userTaskBean = SpringUtil.getBean(UserTaskBean.class);
            //历史处理节点
            ProcessInstanceNode processInstanceNode = null;
            ProcessInstanceData processInstanceData = processInstanceDataService.getOne(new QueryWrapper<ProcessInstanceData>().eq("process_definition_id", processDefinitionId).eq("act_process_instance_id", actProcessInstanceId));
            if (processInstanceData != null) {
                List<ProcessInstanceNode> list = processInstanceNodeService.list(new QueryWrapper<ProcessInstanceNode>().eq("process_instance_data_id", processInstanceData.getId()).eq("task_id", taskId));
                if (ObjectUtil.isNotEmpty(list)) {
                    processInstanceNode = list.get(0);
                }
            }
            if (processInstanceNode != null) {
                //存在历史节点，使用历史处理人
                taskEntity.addCandidateUser(processInstanceNode.getLoginName());
            } else {
                ProcessDefinitionTask processDefinitionTask = processDefinitionTaskService.getOne(new QueryWrapper<ProcessDefinitionTask>().eq("process_definition_id", processDefinitionId).eq("task_id", taskId));
                if (processDefinitionTask.getTaskType().equals("bpmn:startTask")) {
                    SysUser currentUser = (SysUser) httpSession.getAttribute("user");
                    taskEntity.addCandidateUser(currentUser.getLoginName());
                } else {
                    List<SysUser> userList;
                    NextUserVO nextUserVO = (NextUserVO) httpSession.getAttribute("nextUserVO");
                    if (nextUserVO != null) {
                        userList = userTaskBean.getUserList(nextUserVO.getType(), nextUserVO.getTypeValue(), nextUserVO.getHaveStarterDept());
                    } else {
                        userList = userTaskBean.getUserList(processDefinitionTask.getType(), processDefinitionTask.getTypeValue(), processDefinitionTask.getHaveStarterDept());
                    }
                    if (ObjectUtil.isNotEmpty(userList)) {
                        userList.forEach(user -> taskEntity.addCandidateUser(user.getLoginName()));
                    } else {
                        log.error(processDefinitionTask.getTaskId() + "," + processDefinitionTask.getTaskName() + "没有处理人");
                    }
                }
            }
        }
    }

    @Override
    public boolean isFailOnException() {
        log.error("GlobalEventListener-isFailOnException处理人发生错误");
        return false;
    }
}
