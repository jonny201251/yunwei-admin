package com.sss.yunweiadmin.controller;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sss.yunweiadmin.bean.WorkFlowBean;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.model.entity.*;
import com.sss.yunweiadmin.model.vo.*;
import com.sss.yunweiadmin.service.*;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 流程实例数据 前端控制器
 * </p>
 *
 * @author 任勇林
 * @since 2021-04-17
 */
@RestController
@RequestMapping("/processInstanceData")
@ResponseResultWrapper
public class ProcessInstanceDataController {
    @Autowired
    ProcessFormValue1Service processFormValue1Service;
    @Autowired
    ProcessFormValue2Service processFormValue2Service;
    @Autowired
    WorkFlowBean workFlowBean;
    @Autowired
    ProcessInstanceDataService processInstanceDataService;
    @Autowired
    ProcessInstanceNodeService processInstanceNodeService;
    @Autowired
    HttpSession httpSession;
    @Autowired
    SysDeptService sysDeptService;
    @Autowired
    ProcessDefinitionService processDefinitionService;
    @Autowired
    ProcessDefinitionTaskService processDefinitionTaskService;
    @Autowired
    ProcessDefinitionEdgeService processDefinitionEdgeService;
    @Autowired
    AsDeviceCommonService asDeviceCommonService;

    //流程实例
    @GetMapping("list")
    public IPage<ProcessInstanceData> list(int currentPage, int pageSize, String processName, String processStatus, String processType, String displayName, String deptName, String handleName, String no, String startDate, String endDate) {
        QueryWrapper<ProcessInstanceData> queryWrapper = new QueryWrapper<ProcessInstanceData>().orderByDesc("id");
        if (ObjectUtil.isNotEmpty(processName)) {
            queryWrapper.like("processName", processName);
        }
        if (ObjectUtil.isNotEmpty(processStatus)) {
            queryWrapper.eq("process_status", processStatus);
        }
        if (ObjectUtil.isNotEmpty(processType)) {
            List<ProcessDefinition> definitionList = processDefinitionService.list(new QueryWrapper<ProcessDefinition>().eq("process_type", processType));
            if (ObjectUtil.isNotEmpty(definitionList)) {
                queryWrapper.in("process_definition_id", definitionList.stream().map(ProcessDefinition::getId).collect(Collectors.toList()));
            } else {
                queryWrapper.in("process_definition_id", new ArrayList<>());
            }
        }
        if (ObjectUtil.isNotEmpty(displayName)) {
            queryWrapper.eq("display_name", displayName);
        }
        if (ObjectUtil.isNotEmpty(deptName)) {
            queryWrapper.like("dept_name", deptName);
        }
        if (ObjectUtil.isNotEmpty(handleName)) {
            List<ProcessInstanceNode> nodeList = processInstanceNodeService.list(new QueryWrapper<ProcessInstanceNode>().like("display_name", handleName));
            if (ObjectUtil.isNotEmpty(nodeList)) {
                List<Integer> processInstanceIdList = nodeList.stream().map(ProcessInstanceNode::getProcessInstanceDataId).collect(Collectors.toList());
                queryWrapper.in("id", processInstanceIdList);
            } else {
                queryWrapper.in("id", new ArrayList<>());
            }
        }
        if (ObjectUtil.isNotEmpty(no)) {
            List<AsDeviceCommon> asDeviceCommonList = asDeviceCommonService.list(new QueryWrapper<AsDeviceCommon>().like("no", no));
            if (ObjectUtil.isNotEmpty(asDeviceCommonList)) {
                List<Integer> asIdList = asDeviceCommonList.stream().map(AsDeviceCommon::getId).collect(Collectors.toList());
                List<ProcessFormValue2> value2List = processFormValue2Service.list(new QueryWrapper<ProcessFormValue2>().in("as_id", asIdList));
                if (ObjectUtil.isNotEmpty(value2List)) {
                    queryWrapper.in("act_process_instance_id", value2List.stream().map(ProcessFormValue2::getActProcessInstanceId).collect(Collectors.toList()));
                }
            } else {
                queryWrapper.in("act_process_instance_id", new ArrayList<>());
            }
        }
        if (ObjectUtil.isNotEmpty(startDate)) {
            String[] dateArr = startDate.split(",");
            queryWrapper.ge("start_datetime", dateArr[0] + " 00:00:00");
            queryWrapper.le("start_datetime", dateArr[1] + " 00:00:00");

        }
        if (ObjectUtil.isNotEmpty(endDate)) {
            String[] dateArr = endDate.split(",");
            queryWrapper.ge("end_datetime", dateArr[0] + " 00:00:00");
            queryWrapper.le("end_datetime", dateArr[1] + " 00:00:00");

        }

        return processInstanceDataService.page(new Page<>(currentPage, pageSize), queryWrapper);
    }

    //待办任务
    @GetMapping("myList")
    public IPage<ProcessInstanceData> myList(int currentPage, int pageSize, String processName, String processType, String startDate) {
        SysUser user = (SysUser) httpSession.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }
        QueryWrapper<ProcessInstanceData> queryWrapper = new QueryWrapper<ProcessInstanceData>().ne("process_status", "完成").like("display_current_step", user.getDisplayName()).like("login_current_step", user.getLoginName()).orderByDesc("id");
        if (ObjectUtil.isNotEmpty(processName)) {
            queryWrapper.like("processName", processName);
        }
        if (ObjectUtil.isNotEmpty(processType)) {
            List<ProcessDefinition> definitionList = processDefinitionService.list(new QueryWrapper<ProcessDefinition>().eq("process_type", processType));
            if (ObjectUtil.isNotEmpty(definitionList)) {
                queryWrapper.in("process_definition_id", definitionList.stream().map(ProcessDefinition::getId).collect(Collectors.toList()));
            } else {
                queryWrapper.in("process_definition_id", new ArrayList<>());
            }
        }
        if (ObjectUtil.isNotEmpty(startDate)) {
            String[] dateArr = startDate.split(",");
            queryWrapper.ge("start_datetime", dateArr[0] + " 00:00:00");
            queryWrapper.le("start_datetime", dateArr[1] + " 00:00:00");

        }
        return processInstanceDataService.page(new Page<>(currentPage, pageSize), queryWrapper);
    }

    //已办任务
    @GetMapping("completeList")
    public IPage<ProcessInstanceData> completeList(int currentPage, int pageSize, String processName, String processType, String handleName, String no, String startDate, String endDate) {
        SysUser user = (SysUser) httpSession.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }
        QueryWrapper<ProcessInstanceData> queryWrapper = new QueryWrapper<ProcessInstanceData>().eq("process_status", "完成").eq("login_name", user.getLoginName()).orderByDesc("id");
        if (ObjectUtil.isNotEmpty(processName)) {
            queryWrapper.like("processName", processName);
        }
        if (ObjectUtil.isNotEmpty(processType)) {
            List<ProcessDefinition> definitionList = processDefinitionService.list(new QueryWrapper<ProcessDefinition>().eq("process_type", processType));
            if (ObjectUtil.isNotEmpty(definitionList)) {
                queryWrapper.in("process_definition_id", definitionList.stream().map(ProcessDefinition::getId).collect(Collectors.toList()));
            } else {
                queryWrapper.in("process_definition_id", new ArrayList<>());
            }
        }
        if (ObjectUtil.isNotEmpty(handleName)) {
            List<ProcessInstanceNode> nodeList = processInstanceNodeService.list(new QueryWrapper<ProcessInstanceNode>().like("display_name", handleName));
            if (ObjectUtil.isNotEmpty(nodeList)) {
                List<Integer> processInstanceIdList = nodeList.stream().map(ProcessInstanceNode::getProcessInstanceDataId).collect(Collectors.toList());
                queryWrapper.in("id", processInstanceIdList);
            } else {
                queryWrapper.in("id", new ArrayList<>());
            }
        }
        if (ObjectUtil.isNotEmpty(no)) {
            List<AsDeviceCommon> asDeviceCommonList = asDeviceCommonService.list(new QueryWrapper<AsDeviceCommon>().like("no", no));
            if (ObjectUtil.isNotEmpty(asDeviceCommonList)) {
                List<Integer> asIdList = asDeviceCommonList.stream().map(AsDeviceCommon::getId).collect(Collectors.toList());
                List<ProcessFormValue2> value2List = processFormValue2Service.list(new QueryWrapper<ProcessFormValue2>().in("as_id", asIdList));
                if (ObjectUtil.isNotEmpty(value2List)) {
                    queryWrapper.in("act_process_instance_id", value2List.stream().map(ProcessFormValue2::getActProcessInstanceId).collect(Collectors.toList()));
                }
            } else {
                queryWrapper.in("act_process_instance_id", new ArrayList<>());
            }
        }
        if (ObjectUtil.isNotEmpty(startDate)) {
            String[] dateArr = startDate.split(",");
            queryWrapper.ge("start_datetime", dateArr[0] + " 00:00:00");
            queryWrapper.le("start_datetime", dateArr[1] + " 00:00:00");

        }
        if (ObjectUtil.isNotEmpty(endDate)) {
            String[] dateArr = endDate.split(",");
            queryWrapper.ge("end_datetime", dateArr[0] + " 00:00:00");
            queryWrapper.le("end_datetime", dateArr[1] + " 00:00:00");

        }
        return processInstanceDataService.page(new Page<>(currentPage, pageSize), queryWrapper);
    }

    //当前工单
    @GetMapping("currentList")
    public IPage<ProcessInstanceData> currentList(int currentPage, int pageSize, int asId) {
        //
        List<ProcessFormValue2> value2List = processFormValue2Service.list(new QueryWrapper<ProcessFormValue2>().eq("as_id", asId));
        if (ObjectUtil.isNotEmpty(value2List)) {
            List<String> list = value2List.stream().map(ProcessFormValue2::getActProcessInstanceId).collect(Collectors.toList());
            return processInstanceDataService.page(new Page<>(currentPage, pageSize), new QueryWrapper<ProcessInstanceData>().ne("process_status", "完成").in("act_process_instance_id", list));
        }
        return null;
    }

    //历史工单
    @GetMapping("historyList")
    public IPage<ProcessInstanceData> historyList(int currentPage, int pageSize, int asId) {
        //
        List<ProcessFormValue2> value2List = processFormValue2Service.list(new QueryWrapper<ProcessFormValue2>().eq("as_id", asId));
        if (ObjectUtil.isNotEmpty(value2List)) {
            List<String> list = value2List.stream().map(ProcessFormValue2::getActProcessInstanceId).collect(Collectors.toList());
            return processInstanceDataService.page(new Page<>(currentPage, pageSize), new QueryWrapper<ProcessInstanceData>().eq("process_status", "完成").in("act_process_instance_id", list));
        }
        return null;
    }

    @PostMapping("start")
    public boolean start(@RequestBody StartProcessVO startProcessVO) {
        SysUser user = (SysUser) httpSession.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }
        return processInstanceDataService.start(startProcessVO);
    }

    @PostMapping("handle")
    public boolean handle(@RequestBody CheckProcessVO checkProcessVO) {
        SysUser user = (SysUser) httpSession.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }
        return processInstanceDataService.handle(checkProcessVO);
    }

    @PostMapping("modify")
    public boolean modify(@RequestBody ModifyProcessFormVO modifyProcessFormVO) {
        return processInstanceDataService.modifyProcessForm(modifyProcessFormVO);
    }

    @GetMapping("get")
    public ProcessInstanceData getById(String id) {
        return processInstanceDataService.getById(id);
    }

    @PostMapping("delete")
    public boolean delete(@RequestBody ProcessInstanceData processInstanceData) {
        return processInstanceDataService.delete(processInstanceData);
    }

    @GetMapping("getStartProcessConditionVO")
    public StartProcessConditionVO getStartProcessConditionVO(Integer processDefinitionId) {
        StartProcessConditionVO startProcessConditionVO = new StartProcessConditionVO();
        //取出流程定义中的第一个发起任务节点
        List<ProcessDefinitionTask> startEventList = processDefinitionTaskService.list(new QueryWrapper<ProcessDefinitionTask>().eq("process_definition_id", processDefinitionId).eq("task_type", "bpmn:startEvent"));
        List<ProcessDefinitionEdge> edgeList = processDefinitionEdgeService.list(new QueryWrapper<ProcessDefinitionEdge>().eq("process_definition_id", processDefinitionId).in("source_id", startEventList.stream().map(ProcessDefinitionTask::getTaskId).collect(Collectors.toList())));
        String startTaskId;
        if (ObjectUtil.isNotEmpty(edgeList)) {
            startTaskId = edgeList.get(0).getTargetId();
        } else {
            throw new RuntimeException("流程图错误,缺少开始节点");
        }
        if (startTaskId != null) {
            //获取多条连线
            List<String> buttonNameList = workFlowBean.getButtonNameList(processDefinitionId, startTaskId);
            if (ObjectUtil.isNotEmpty(buttonNameList)) {
                startProcessConditionVO.setButtonNameList(buttonNameList);
            }
            ProcessDefinitionTask processDefinitionTask = processDefinitionTaskService.getOne(new QueryWrapper<ProcessDefinitionTask>().eq("process_definition_id", processDefinitionId).eq("task_id", startTaskId));
            //是否有下一步处理人
            startProcessConditionVO.setHaveNextUser(processDefinitionTask.getHaveNextUser());
        }

        return startProcessConditionVO;
    }

    @GetMapping("getCheckProcessConditionVO")
    public CheckProcessConditionVO getUserTaskConditionVO(Integer processDefinitionId, String actProcessInstanceId) {
        SysUser user = (SysUser) httpSession.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }
        CheckProcessConditionVO checkProcessConditionVO = new CheckProcessConditionVO();
        //取出我的一个任务
        List<Task> taskList = workFlowBean.getMyTask(actProcessInstanceId);
        if (ObjectUtil.isEmpty(taskList)) {
            throw new RuntimeException("没有用户任务");
        }
        Task actTask = taskList.get(0);
        //获取多条连线
        List<String> buttonNameList = workFlowBean.getButtonNameList(processDefinitionId, actTask.getTaskDefinitionKey());
        if (ObjectUtil.isNotEmpty(buttonNameList)) {
            checkProcessConditionVO.setButtonNameList(buttonNameList);
        }
        //是否允许 意见，修改表单，下一步处理人
        ProcessDefinitionTask processDefinitionTask = processDefinitionTaskService.getOne(new QueryWrapper<ProcessDefinitionTask>().eq("process_definition_id", processDefinitionId).eq("task_id", actTask.getTaskDefinitionKey()));
        if (processDefinitionTask.getHaveComment().equals("是")) {
            if (processDefinitionTask.getTaskType().equals("bpmn:approvalTask")) {
                //审批任务
                checkProcessConditionVO.setCommentTitle("审批意见");
            } else {
                //处理任务
                checkProcessConditionVO.setCommentTitle("处理结果");
            }
        }
        checkProcessConditionVO.setHaveComment(processDefinitionTask.getHaveComment());
        checkProcessConditionVO.setHaveEditForm(processDefinitionTask.getHaveEditForm());
        checkProcessConditionVO.setHaveNextUser(processDefinitionTask.getHaveNextUser());
        checkProcessConditionVO.setHaveOperate(processDefinitionTask.getHaveOperate());
        return checkProcessConditionVO;
    }

    @GetMapping("getActiveTaskIdList")
    public List<String> getActiveTaskIdList(String actProcessInstanceId) {
        List<String> list = new ArrayList<>();
        List<Task> activeTaskList = workFlowBean.getActiveTask(actProcessInstanceId);
        if (ObjectUtil.isNotEmpty(activeTaskList)) {
            list = activeTaskList.stream().map(Task::getTaskDefinitionKey).collect(Collectors.toList());
        }
        return list;
    }
}
