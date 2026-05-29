package com.zjhl.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjhl.project.entity.SysRole;
import com.zjhl.project.mapper.SysRoleMapper;
import com.zjhl.project.service.SysRoleService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

	@Autowired
    private SysRoleMapper sysRoleMapper;
	
    @Override
    public SysRole getByRoleCode(String roleCode) {
        QueryWrapper<SysRole> wrapper = new QueryWrapper<>();
        wrapper.eq("role_code", roleCode);
        return sysRoleMapper.selectOne(wrapper);
    }
}
