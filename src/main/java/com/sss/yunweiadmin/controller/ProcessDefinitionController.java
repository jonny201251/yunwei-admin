package com.sss.yunweiadmin.controller;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.common.utils.ProcessFormCustomTypeUtil;
import com.sss.yunweiadmin.model.entity.*;
import com.sss.yunweiadmin.model.vo.ProcessDefinitionVO;
import com.sss.yunweiadmin.model.vo.TreeSelectVO;
import com.sss.yunweiadmin.model.vo.ValueLabelVO;
import com.sss.yunweiadmin.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 功能：用于保存、编辑、浏览 流程定义信息，自定义表单，流程设计
 *
 * @author 任勇林
 * @since 2021-03-21
 */
@RestController
@RequestMapping("/processDefinition")
@ResponseResultWrapper
public class ProcessDefinitionController {
    @Autowired
    ProcessDefinitionService processDefinitionService;
    @Autowired
    SysDicService sysDicService;
    @Autowired
    ProcessFormCustomTypeService processFormCustomTypeService;
    @Autowired
    ProcessFormTemplateService processFormTemplateService;
    @Autowired
    ProcessDefinitionTaskService processDefinitionTaskService;
    @Autowired
    ProcessDefinitionEdgeService processDefinitionEdgeService;

    //获取所有下拉类型
    @GetMapping("getTypeVL")
    public List<ValueLabelVO> getType() {
        List<SysDic> list1 = sysDicService.list(new QueryWrapper<SysDic>().eq("flag", "下拉类型").orderByAsc("sort"));
        List<ProcessFormCustomType> list2 = processFormCustomTypeService.list(new QueryWrapper<ProcessFormCustomType>().eq("status", "正常").orderByAsc("sort"));
        List<ValueLabelVO> result1 = list1.stream().map(item -> new ValueLabelVO(item.getName(), item.getName())).collect(Collectors.toList());
        List<ValueLabelVO> result2 = list2.stream().map(item -> new ValueLabelVO(item.getId() + "." + item.getName(), item.getName())).collect(Collectors.toList());
        result1.addAll(result2);
        return result1;
    }

    //获取基本类型
    @GetMapping("getBaseTypeVL")
    public List<ValueLabelVO> getBaseType() {
        List<SysDic> list = sysDicService.list(new QueryWrapper<SysDic>().eq("flag", "下拉类型").orderByAsc("sort"));
        List<ValueLabelVO> result = list.stream().map(item -> new ValueLabelVO(item.getName(), item.getName())).collect(Collectors.toList());
        return result;
    }

    //tableNameArr=[计算机类型，设备通用类型]
    @GetMapping("getTreeByTableNames")
    public List<TreeSelectVO> getTreeByTableNames(String[] tableNameArr) {
        List<TreeSelectVO> treeList = Lists.newArrayList();
        List<Integer> tableIdList = Arrays.stream(tableNameArr).map(item -> Integer.parseInt(item.split("\\.")[0])).collect(Collectors.toList());
        List<ProcessFormCustomType> list = processFormCustomTypeService.list(new QueryWrapper<ProcessFormCustomType>().in("id", tableIdList).eq("status", "正常").orderByAsc("sort"));
        for (ProcessFormCustomType processFormCustomType : list) {
            TreeSelectVO parent = new TreeSelectVO();
            parent.setTitle(processFormCustomType.getName());
            parent.setValue(processFormCustomType.getName());
            parent.setKey(processFormCustomType.getName());

            String props = processFormCustomType.getProps();
            Map<String, List<AsConfig>> map = ProcessFormCustomTypeUtil.parseProps2(props);
            for (Map.Entry<String, List<AsConfig>> entry : map.entrySet()) {
                String enTableName = entry.getKey();
                for (AsConfig asConfig : entry.getValue()) {
                    TreeSelectVO child = new TreeSelectVO();
                    child.setTitle(asConfig.getZhColumnName());
                    child.setValue(processFormCustomType.getName() + "." + asConfig.getZhColumnName());
                    child.setKey(processFormCustomType.getId() + "." + processFormCustomType.getName() + "." + enTableName + "." + asConfig.getEnColumnName() + "," + asConfig.getType());
                    if (parent.getChildren() != null) {
                        parent.getChildren().add(child);
                    } else {
                        parent.setChildren(Lists.newArrayList(child));
                    }
                }
            }
            treeList.add(parent);
        }

        return treeList;
    }

    @GetMapping("list")
    public IPage<ProcessDefinition> list(int currentPage, int pageSize, String processName, String processType) {
        QueryWrapper<ProcessDefinition> queryWrapper = new QueryWrapper<ProcessDefinition>().eq("have_display", "是").orderByDesc("id");
        if (ObjectUtil.isNotEmpty(processName)) {
            queryWrapper.like("process_name", processName);
        }
        if (ObjectUtil.isNotEmpty(processType)) {
            queryWrapper.like("process_type", processType);
        }
        IPage<ProcessDefinition> page = processDefinitionService.page(new Page<>(currentPage, pageSize), queryWrapper);
        page.getRecords().forEach(item -> item.setBpmnXml(""));
        return page;
    }

    @GetMapping("get")
    public ProcessDefinition getById(String processDefinitionId) {
        return processDefinitionService.getById(processDefinitionId);
    }

    @PostMapping("add")
    public boolean add(@RequestBody ProcessDefinitionVO processDefinitionVO) {
        return processDefinitionService.add(processDefinitionVO);
    }

    //获取流程定义时的3个表单数据
    @GetMapping("getProcessDefinitionVO")
    public ProcessDefinitionVO getProcessDefinitionVO(String processDefinitionId) {
        ProcessDefinitionVO processDefinitionVO = new ProcessDefinitionVO();

        ProcessDefinition processDefinition = processDefinitionService.getById(processDefinitionId);

        List<ProcessFormTemplate> formList = processFormTemplateService.list(new QueryWrapper<ProcessFormTemplate>().eq("process_definition_id", processDefinitionId));
        LinkedHashMap<String, ArrayList<ProcessFormTemplate>> formTemplateMap = new LinkedHashMap<>();
        formTemplateMap.put("firstData", new ArrayList<>());
        for (ProcessFormTemplate processFormTemplate : formList) {
            if ((processFormTemplate.getFlag().equals("基本类型") || processFormTemplate.getFlag().equals("表类型") || processFormTemplate.getFlag().equals("字段变更类型")) && Strings.isNullOrEmpty(processFormTemplate.getGroupParentLabel())) {
                formTemplateMap.get("firstData").add(processFormTemplate);
            } else if (processFormTemplate.getFlag().equals("字段组类型")) {
                formTemplateMap.get("firstData").add(processFormTemplate);
            } else {
                if (formTemplateMap.get(processFormTemplate.getGroupParentLabel()) != null) {
                    formTemplateMap.get(processFormTemplate.getGroupParentLabel()).add(processFormTemplate);
                } else {
                    ArrayList<ProcessFormTemplate> list = Lists.newArrayList();
                    list.add(processFormTemplate);
                    formTemplateMap.put(processFormTemplate.getGroupParentLabel(), list);
                }
            }
        }
        List<ProcessDefinitionTask> taskList = processDefinitionTaskService.list(new QueryWrapper<ProcessDefinitionTask>().eq("process_definition_id", processDefinitionId));
        List<ProcessDefinitionEdge> edgeList = processDefinitionEdgeService.list(new QueryWrapper<ProcessDefinitionEdge>().eq("process_definition_id", processDefinitionId));

        processDefinitionVO.setProcessDefinition(processDefinition);
        processDefinitionVO.setFormTemplateMap(formTemplateMap);
        processDefinitionVO.setTaskList(taskList);
        processDefinitionVO.setEdgeList(edgeList);

        return processDefinitionVO;
    }

    @PostMapping("edit")
    public boolean edit(@RequestBody ProcessDefinitionVO processDefinitionVO) {
        return processDefinitionService.edit(processDefinitionVO);
    }

    @GetMapping("delete")
    public boolean delete(Integer processDefinitionId) {
        return processDefinitionService.delete(processDefinitionId);
    }

    @GetMapping("copy")
    public boolean copy(Integer processDefinitionId) {
        return processDefinitionService.copy(processDefinitionId);
    }
}