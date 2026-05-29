package com.zjhl.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zjhl.project.entity.SysDept;
import java.util.List;

public interface SysDeptService extends IService<SysDept> {
    List<SysDept> getDeptTree();
    List<SysDept> getDeptList(SysDept dept);
}
