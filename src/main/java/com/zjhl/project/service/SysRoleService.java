package com.zjhl.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zjhl.project.entity.SysRole;

public interface SysRoleService extends IService<SysRole> {
    SysRole getByRoleCode(String roleCode);
}
