package com.sss.yunweiadmin.controller;


import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.common.utils.TreeUtil;
import com.sss.yunweiadmin.model.entity.SysDept;
import com.sss.yunweiadmin.model.vo.TreeSelectVO;
import com.sss.yunweiadmin.model.vo.TreeTransferVO;
import com.sss.yunweiadmin.service.SysDeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * 部门表 前端控制器
 * </p>
 *
 * @author 任勇林
 * @since 2021-04-09
 */
@RestController
@RequestMapping("/sysDept")
@ResponseResultWrapper
public class SysDeptController extends BaseController<SysDept> {
    @Autowired
    private SysDeptService sysDeptService;

    @Override
    @GetMapping("list")
    public IPage<SysDept> list(int currentPage, int pageSize) {
        //取出pid=0的数据
        IPage<SysDept> page = sysDeptService.page(new Page<>(currentPage, pageSize), new QueryWrapper<SysDept>().eq("pid", 0).orderByAsc("sort"));
        List<SysDept> list = page.getRecords();
        //取出pid！=0的数据
        List<SysDept> otherList = sysDeptService.list(new QueryWrapper<SysDept>().ne("pid", 0).orderByAsc("sort"));

        TreeUtil.setTableTree(list, otherList);
        return page;
    }

    @PostMapping("add")
    public boolean add(@RequestBody SysDept sysDept) {
        return sysDeptService.save(sysDept);
    }

    @GetMapping("get")
    public SysDept getById(String id) {
        return sysDeptService.getById(id);
    }

    @PostMapping("edit")
    public boolean edit(@RequestBody SysDept sysDept) {
        return sysDeptService.updateById(sysDept);
    }

    @GetMapping("delete")
    public boolean delete(Integer[] idArr) {
        List<Integer> idList = Stream.of(idArr).collect(Collectors.toList());
        //根据idList，取出所有的子节点
        List<Integer> list = Lists.newArrayList(idList);
        while (true) {
            List<SysDept> tmp = sysDeptService.list(new QueryWrapper<SysDept>().in("pid", idList));
            if (CollUtil.isEmpty(tmp)) {
                break;
            } else {
                idList = tmp.stream().map(SysDept::getId).collect(Collectors.toList());
                list.addAll(idList);
            }
        }
        return sysDeptService.remove(new QueryWrapper<SysDept>().in("id", list));
    }

    @GetMapping("getDeptTree")
    public List<TreeSelectVO> getDeptTree() {
        List<SysDept> list = sysDeptService.list(new QueryWrapper<SysDept>().orderByAsc("sort"));
        return TreeUtil.getTreeSelectVO(list);
    }

    @GetMapping("getDeptUserTree")
    public List<TreeTransferVO> getDeptUserTree() {
        List<SysDept> list = sysDeptService.list(new QueryWrapper<SysDept>().notIn("id", 1, 2).orderByAsc("sort"));
        return TreeUtil.getSelectDeptUserTree(list);
    }

}
