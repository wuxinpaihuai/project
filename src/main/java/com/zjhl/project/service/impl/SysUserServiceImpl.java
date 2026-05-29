package com.zjhl.project.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjhl.project.entity.SysUser;
import com.zjhl.project.mapper.SysUserMapper;
import com.zjhl.project.service.SysUserService;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Override
    public SysUser getByUsername(String username) {
        return this.getOne(new QueryWrapper<SysUser>().eq("username", username));
    }
}