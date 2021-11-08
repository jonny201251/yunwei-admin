package com.sss.yunweiadmin.common.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sss.yunweiadmin.model.entity.ProcessFormTemplate;
import com.sss.yunweiadmin.model.entity.SysDept;
import com.sss.yunweiadmin.model.entity.SysUser;
import com.sss.yunweiadmin.model.vo.FormTemplateVO;
import com.sss.yunweiadmin.model.vo.TreeSelectVO;
import com.sss.yunweiadmin.model.vo.TreeTransferVO;
import com.sss.yunweiadmin.service.SysUserService;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//前后端都可以实现结构树的方法，https://blog.csdn.net/bertZuo/article/details/107200165
public class TreeUtil {
    //下拉树
    public static <T> List<T> getTreeSelect(List<T> initList) {
        if (CollUtil.isEmpty(initList)) throw new RuntimeException("集合为空！");
        List<T> list = Lists.newArrayList();
        Map<Integer, T> map = Maps.newHashMap();
        for (T obj : initList) {
            Integer id = (Integer) ReflectUtil.getFieldValue(obj, "id");
            map.put(id, obj);
        }
        for (T obj : initList) {
            Integer id = (Integer) ReflectUtil.getFieldValue(obj, "id");
            Integer pid = (Integer) ReflectUtil.getFieldValue(obj, "pid");
            T parent = map.get(pid);
            if (parent != null) {
                Object childen = ReflectUtil.getFieldValue(parent, "children");
                if (childen != null) {
                    ReflectUtil.invoke(childen, "add", map.get(id));
                } else {
                    ReflectUtil.setFieldValue(parent, "children", Lists.newArrayList(map.get(id)));
                }
            } else {
                list.add(map.get(id));
            }
        }
        return list;
    }

    //下拉树
    public static <T> List<TreeSelectVO> getTreeSelectVO(List<T> initList) {
        if (CollUtil.isEmpty(initList)) throw new RuntimeException("集合为空！");
        List<TreeSelectVO> treeList = Lists.newArrayList();
        Map<Integer, TreeSelectVO> map = Maps.newHashMap();

        for (T obj : initList) {
            Integer id = (Integer) ReflectUtil.getFieldValue(obj, "id");
            String name = (String) ReflectUtil.getFieldValue(obj, "name");
            TreeSelectVO treeSelectVO = new TreeSelectVO();
            treeSelectVO.setTitle(name);
            treeSelectVO.setKey(id);
            treeSelectVO.setValue(id);
            map.put(id, treeSelectVO);
        }

        for (T obj : initList) {
            Integer id = (Integer) ReflectUtil.getFieldValue(obj, "id");

            Integer pid = (Integer) ReflectUtil.getFieldValue(obj, "pid");
            TreeSelectVO parent = map.get(pid);
            if (parent != null) {
                if (parent.getChildren() != null) {
                    parent.getChildren().add(map.get(id));
                } else {
                    parent.setChildren(Lists.newArrayList(map.get(id)));
                }
            } else {
                treeList.add(map.get(id));
            }
        }
        return treeList;
    }

    //下拉 部门-用户 树
    public static List<TreeTransferVO> getSelectDeptUserTree(List<SysDept> initList) {
        if (CollUtil.isEmpty(initList)) throw new RuntimeException("集合为空！");
        List<TreeTransferVO> treeList = Lists.newArrayList();
        Map<Integer, TreeTransferVO> map = Maps.newHashMap();

        //获取所有用户
        SysUserService sysUserService = SpringUtil.getBean(SysUserService.class);
        List<SysUser> userList = sysUserService.list(new QueryWrapper<SysUser>().orderByAsc("sort"));
        Map<Integer, List<SysUser>> userMap = userList.stream().collect(Collectors.groupingBy(SysUser::getDeptId, Collectors.toList()));

        for (SysDept dept : initList) {
            Integer id = dept.getId();
            String deptName = dept.getName();
            TreeTransferVO treeTransferVO = new TreeTransferVO();
            treeTransferVO.setTitle(deptName);
            treeTransferVO.setKey("dept" + id);
            treeTransferVO.setCheckable(false);

            //设置用户
            if (userMap.get(id) != null) {
                List<TreeTransferVO> userTreeList = userMap.get(id).stream().map(user -> {
                    TreeTransferVO userTreeTransferVO = new TreeTransferVO();
                    userTreeTransferVO.setTitle(user.getDisplayName());
                    userTreeTransferVO.setKey("user" + user.getId());
                    userTreeTransferVO.setTmp(deptName);
                    return userTreeTransferVO;
                }).collect(Collectors.toList());
                treeTransferVO.setChildren(userTreeList);
            }

            map.put(id, treeTransferVO);
        }

        for (SysDept dept : initList) {
            Integer id = dept.getId();

            Integer pid = dept.getPid();
            TreeTransferVO parent = map.get(pid);
            if (parent != null) {
                if (parent.getChildren() != null) {
                    parent.getChildren().add(map.get(id));
                } else {
                    parent.setChildren(Lists.newArrayList(map.get(id)));
                }
            } else {
                treeList.add(map.get(id));
            }
        }
        return treeList;
    }

    //表格树
    public static <T> void setTableTree(List<T> list, List<T> otherList) {
        Map<Integer, T> map = Maps.newHashMap();
        for (T obj : list) {
            Integer id = (Integer) ReflectUtil.getFieldValue(obj, "id");
            map.put(id, obj);
        }
        for (T obj : otherList) {
            Integer id = (Integer) ReflectUtil.getFieldValue(obj, "id");
            map.put(id, obj);
        }

        for (T obj : otherList) {
            Integer id = (Integer) ReflectUtil.getFieldValue(obj, "id");
            Integer pid = (Integer) ReflectUtil.getFieldValue(obj, "pid");
            T parent = map.get(pid);
            if (parent != null) {
                Object childen = ReflectUtil.getFieldValue(parent, "children");
                if (childen != null) {
                    ReflectUtil.invoke(childen, "add", map.get(id));
                } else {
                    ReflectUtil.setFieldValue(parent, "children", Lists.newArrayList(map.get(id)));
                }
            }
        }
    }

    //自定义表单树
    public static List<FormTemplateVO> getFormTemplateTree(List<ProcessFormTemplate> initList) {
        if (CollUtil.isEmpty(initList)) throw new RuntimeException("集合为空！");
        List<FormTemplateVO> treeList = Lists.newArrayList();
        Map<Integer, FormTemplateVO> map = Maps.newHashMap();

        Map<String, Integer> groupMap = Maps.newHashMap();

        for (ProcessFormTemplate processFormTemplate : initList) {
            FormTemplateVO formTemplateVO = new FormTemplateVO();
            BeanUtils.copyProperties(processFormTemplate, formTemplateVO);
            map.put(formTemplateVO.getId(), formTemplateVO);

            if (formTemplateVO.getFlag().equals("字段组类型")) {
                groupMap.put(formTemplateVO.getLabel(), formTemplateVO.getId());
            }
        }

        for (ProcessFormTemplate processFormTemplate : initList) {
            Integer id = processFormTemplate.getId();
            String groupParentLabel = processFormTemplate.getGroupParentLabel();
            Integer groupParentId = groupMap.get(groupParentLabel);
            if (groupParentId != null) {
                FormTemplateVO parent = map.get(groupParentId);
                if (parent != null) {
                    if (parent.getChildren() != null) {
                        parent.getChildren().add(map.get(id));
                    } else {
                        parent.setChildren(Lists.newArrayList(map.get(id)));
                    }
                }
            } else {
                treeList.add(map.get(id));
            }
        }
        return treeList;
    }
}
