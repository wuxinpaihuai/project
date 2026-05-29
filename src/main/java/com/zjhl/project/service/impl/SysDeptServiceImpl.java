package com.zjhl.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjhl.project.entity.SysDept;
import com.zjhl.project.mapper.SysDeptMapper;
import com.zjhl.project.service.SysDeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements SysDeptService {

    @Autowired
    private SysDeptMapper sysDeptMapper;

    @Override
    public List<SysDept> getDeptTree() {
        List<SysDept> allDepts = sysDeptMapper.selectList(null);
        return buildTree(allDepts);
    }

    @Override
    public List<SysDept> getDeptList(SysDept dept) {
        QueryWrapper<SysDept> wrapper = new QueryWrapper<>();
        if (dept != null) {
            if (dept.getDeptName() != null && !dept.getDeptName().isEmpty()) {
                wrapper.like("dept_name", dept.getDeptName());
            }
            if (dept.getDeptCode() != null && !dept.getDeptCode().isEmpty()) {
                wrapper.like("dept_code", dept.getDeptCode());
            }
            if (dept.getStatus() != null) {
                wrapper.eq("status", dept.getStatus());
            }
        }
        wrapper.orderByAsc("sort_num");
        return sysDeptMapper.selectList(wrapper);
    }

    private List<SysDept> buildTree(List<SysDept> depts) {
        List<SysDept> result = new ArrayList<>();
        Map<Long, List<SysDept>> groupByParentId = depts.stream()
                .collect(Collectors.groupingBy(SysDept::getParentId));
        buildChildren(result, groupByParentId, 0L);
        return result;
    }

    private void buildChildren(List<SysDept> result, Map<Long, List<SysDept>> groupByParentId, Long parentId) {
        List<SysDept> children = groupByParentId.get(parentId);
        if (children != null) {
            for (SysDept dept : children) {
                result.add(dept);
                List<SysDept> childList = new ArrayList<>();
                buildChildren(childList, groupByParentId, dept.getId());
                if (!childList.isEmpty()) {
                    dept.setChildren(childList);
                }
            }
        }
    }
}
