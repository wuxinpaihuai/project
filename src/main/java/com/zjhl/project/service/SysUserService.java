package com.zjhl.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zjhl.project.entity.SysUser;

public interface SysUserService extends IService<SysUser> {
    SysUser getByUsername(String username);
}