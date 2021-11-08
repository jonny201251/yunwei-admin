package com.sss.yunweiadmin.controller;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.common.utils.ProcessFormCustomTypeUtil;
import com.sss.yunweiadmin.common.utils.SpringUtil;
import com.sss.yunweiadmin.common.utils.TreeUtil;
import com.sss.yunweiadmin.model.entity.AsConfig;
import com.sss.yunweiadmin.model.entity.ProcessFormCustomType;
import com.sss.yunweiadmin.model.entity.ProcessFormTemplate;
import com.sss.yunweiadmin.model.vo.FormTemplateVO;
import com.sss.yunweiadmin.model.vo.TableTypeVO;
import com.sss.yunweiadmin.model.vo.TreeDTO;
import com.sss.yunweiadmin.model.vo.TreeSelectVO;
import com.sss.yunweiadmin.service.AsConfigService;
import com.sss.yunweiadmin.service.ProcessFormCustomTypeService;
import com.sss.yunweiadmin.service.ProcessFormTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * 自定义表单模板 前端控制器
 * </p>
 *
 * @author 任勇林
 * @since 2021-04-14
 */
@RestController
@RequestMapping("/processFormTemplate")
@ResponseResultWrapper
public class ProcessFormTemplateController {
    @Autowired
    private ProcessFormTemplateService processFormTemplateService;
    @Autowired
    private ProcessFormCustomTypeService processFormCustomTypeService;
    @Autowired
    private AsConfigService asConfigService;

    @GetMapping("getFormTemplateTree")
    public List<FormTemplateVO> getFormTemplateTree(Integer processDefinitionId) {
        List<ProcessFormTemplate> list = processFormTemplateService.list(new QueryWrapper<ProcessFormTemplate>().eq("process_definition_id", processDefinitionId));
        return TreeUtil.getFormTemplateTree(list);
    }

    @GetMapping("getFormTemplateGroupTree")
    public List<TreeSelectVO> getFormTemplateGroupTree(Integer processDefinitionId) {
        List<ProcessFormTemplate> list1 = processFormTemplateService.list(new QueryWrapper<ProcessFormTemplate>().eq("process_definition_id", processDefinitionId).eq("type", "字段组"));
        Map<String, String> map1 = list1.stream().collect(Collectors.toMap(ProcessFormTemplate::getLabel, ProcessFormTemplate::getHaveGroupSelect));

        List<ProcessFormTemplate> list2 = processFormTemplateService.list(new QueryWrapper<ProcessFormTemplate>().eq("process_definition_id", processDefinitionId).eq("type", "字段组").eq("have_group_select", "是"));
        Map<String, Integer> map2 = list2.stream().collect(Collectors.toMap(ProcessFormTemplate::getLabel, ProcessFormTemplate::getId));
        //
        List<TreeDTO> list3 = Lists.newArrayList();
        for (ProcessFormTemplate processFormTemplate : list2) {
            if (!Strings.isNullOrEmpty(processFormTemplate.getGroupParentLabel())) {
                //有父字段组,判断父字段组的haveGroupSelect
                String groupParentLabel = processFormTemplate.getGroupParentLabel();
                String haveGroupSelect = map1.get(groupParentLabel);
                if (haveGroupSelect.equals("是")) {
                    TreeDTO treeDTO = new TreeDTO();
                    treeDTO.setId(processFormTemplate.getId());
                    treeDTO.setName(processFormTemplate.getLabel());
                    treeDTO.setPid(map2.get(processFormTemplate.getGroupParentLabel()));
                    list3.add(treeDTO);
                }
            } else {
                TreeDTO treeDTO = new TreeDTO();
                treeDTO.setId(processFormTemplate.getId());
                treeDTO.setName(processFormTemplate.getLabel());
                treeDTO.setPid(0);
                list3.add(treeDTO);
            }
        }
        System.out.println();
        if (CollUtil.isEmpty(list3)) {
            return Lists.newArrayList();
        } else {
            return TreeUtil.getTreeSelectVO(list3);
        }
    }

    //根据已选择的字段组id,筛选出完整需要被显示的字段组id
    @GetMapping("getSelectGroupIdList")
    public Set<Integer> getSelectGroupIdList(Integer processDefinitionId, Integer[] checkGroupIdArr) {
        List<ProcessFormTemplate> list = processFormTemplateService.list(new QueryWrapper<ProcessFormTemplate>().eq("process_definition_id", processDefinitionId).eq("type", "字段组"));
        Map<String, ProcessFormTemplate> map = list.stream().collect(Collectors.toMap(ProcessFormTemplate::getLabel, ProcessFormTemplate -> ProcessFormTemplate));

        Set<Integer> selectGroupIdSet = Stream.of(checkGroupIdArr).collect(Collectors.toSet());
        //先将 have_group_select=否 放入checkGroupIdArr
        selectGroupIdSet.addAll(list.stream().filter(item -> item.getHaveGroupSelect().equals("否")).map(ProcessFormTemplate::getId).collect(Collectors.toSet()));
        //根据checkGroupIdArr，继续找出有父子关系的需要显示的父id
        List<ProcessFormTemplate> list2 = list.stream().filter(item -> selectGroupIdSet.contains(item.getId())).collect(Collectors.toList());
        for (ProcessFormTemplate processFormTemplate : list2) {
            String groupParentLabel = processFormTemplate.getGroupParentLabel();
            if (!Strings.isNullOrEmpty(groupParentLabel)) {
                try {
                    ProcessFormTemplate tmp = map.get(groupParentLabel);
                    selectGroupIdSet.add(tmp.getId());
                    //继续向上寻找
                    String groupParentLabel2 = tmp.getGroupParentLabel();
                    while (true) {
                        ProcessFormTemplate tmp2 = map.get(groupParentLabel2);
                        if (tmp2 != null) {
                            selectGroupIdSet.add(tmp2.getId());
                            groupParentLabel2 = tmp2.getGroupParentLabel();
                        } else {
                            break;
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException("此处代码有错误。。。");
                }
            }
        }
        return selectGroupIdSet;
    }

    @GetMapping("getTableTypeVO")
    public Map<Integer, List<TableTypeVO>> getTableTypeVO(Integer processDefinitionId) {
        Map<Integer, List<TableTypeVO>> map = Maps.newTreeMap();
        //1.取出所有的表类型的名称
        List<ProcessFormTemplate> list = processFormTemplateService.list(new QueryWrapper<ProcessFormTemplate>().eq("process_definition_id", processDefinitionId));
        List<Integer> tableIdList = list.stream().filter(item -> item.getFlag().equals("表类型")).map(item -> {
            String tableId = item.getType().split("\\.")[0];
            return Integer.parseInt(tableId);
        }).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(list) && CollUtil.isNotEmpty(tableIdList)) {
            //2.根据表名称取出processFormCustomType
            List<ProcessFormCustomType> typeList = processFormCustomTypeService.list(new QueryWrapper<ProcessFormCustomType>().in("id", tableIdList));
            //3.
            for (ProcessFormCustomType processFormCustomType : typeList) {
                List<TableTypeVO> tmpList = Lists.newArrayList();

                String props = processFormCustomType.getProps();
                Map<String, List<AsConfig>> tmpMap = ProcessFormCustomTypeUtil.parseProps(props);
                tmpMap.entrySet().stream().forEach(item -> {
                    item.getValue().forEach(item2 -> {
                        TableTypeVO tableTypeVO = new TableTypeVO();
                        tableTypeVO.setLabel(processFormCustomType.getName() + "." + item2.getZhColumnName());
                        tableTypeVO.setName(processFormCustomType.getId() + "." + processFormCustomType.getName() + "." + item.getKey() + "." + item2.getEnColumnName() + "." + item2.getId());
                        tmpList.add(tableTypeVO);
                    });
                });

                map.put(processFormCustomType.getId(), tmpList);
            }
        }
        return map;
    }


    @GetMapping("getTableTypeDbData")
    public Map<String, String> getTableTypeDbData(Integer customTableId, Integer asDeviceCommonId, Integer processDefinitionId) {
        Map<String, String> map = Maps.newTreeMap();

        List<TableTypeVO> list = this.getTableTypeVO(processDefinitionId).get(customTableId);
        list.forEach(item -> {
            //1.资产哦.as_device_common.no.1
            String name = item.getName();
            String[] arr = name.split("\\.");
            String serviceName = StrUtil.toCamelCase(arr[2]) + "ServiceImpl";
            IService service = (IService) SpringUtil.getBean(serviceName);
            Object obj = null;
            if (arr[2].equals("as_device_common")) {
                obj = service.getById(asDeviceCommonId);
            } else {
                obj = service.getOne(new QueryWrapper<Object>().eq("as_id", asDeviceCommonId));
            }
            if (obj != null) {
                String value = ReflectUtil.getFieldValue(obj, StrUtil.toCamelCase(arr[3])).toString();
                if (!Strings.isNullOrEmpty(value)) {
                    map.put(name, value);
                }
            }
        });

        return map;
    }
}
