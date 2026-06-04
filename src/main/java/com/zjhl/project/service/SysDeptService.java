package com.zjhl.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zjhl.project.entity.SysDept;
import java.util.List;

public interface SysDeptService extends IService<SysDept> {
    List<SysDept> getDeptTree();
    List<SysDept> getDeptList(SysDept dept);
    // 假设你的 SysDeptService 里有这个方法，如果没有请加上
    public List<Long> getAllChildDeptIds(Long deptId);
}
