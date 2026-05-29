package com.zjhl.project.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zjhl.project.entity.SysRole;
import com.zjhl.project.entity.SysUserRole;
import com.zjhl.project.service.SysRoleService;
import com.zjhl.project.service.SysUserRoleService;

import cn.dev33.satoken.stp.StpUtil;

@RestController
@RequestMapping("/sys/userRole")
public class SysUserRoleController {

    @Autowired
    private SysUserRoleService sysUserRoleService;
    
    @Autowired
    private SysRoleService sysRoleService;

    /**
     * 获取用户的所有角色
     */
    @GetMapping("/listByUser/{userId}")
    public Map<String, Object> listByUser(@PathVariable Long userId) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        
        QueryWrapper<SysUserRole> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        List<SysUserRole> userRoles = sysUserRoleService.list(wrapper);
        
        // 获取角色信息
        if (!userRoles.isEmpty()) {
            List<Long> roleIds = userRoles.stream()
                    .map(SysUserRole::getRoleId)
                    .collect(Collectors.toList());
            List<SysRole> roles = sysRoleService.listByIds(roleIds);
            result.put("data", roles);
        } else {
            result.put("data", Collections.EMPTY_LIST);
        }
        
        result.put("code", 200);
        result.put("msg", "查询成功");
        return result;
    }

    /**
     * 分配角色给用户
     */
    @PostMapping("/assign")
    public Map<String, Object> assign(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        
        Long userId = Long.valueOf(params.get("userId").toString());
        @SuppressWarnings("unchecked")
        List<Long> roleIds = (List<Long>) params.get("roleIds");
        
        // 先删除用户原有的角色
        QueryWrapper<SysUserRole> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        sysUserRoleService.remove(wrapper);
        
        // 添加新的角色
        for (Long roleId : roleIds) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            sysUserRoleService.save(userRole);
        }
        
        result.put("code", 200);
        result.put("msg", "分配成功");
        return result;
    }
}
