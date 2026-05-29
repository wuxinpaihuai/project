package com.zjhl.project.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjhl.project.entity.SysUser;
import com.zjhl.project.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/sys/user")
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    /**
     * 用户列表（分页+条件查询）
     */
    @GetMapping("/list")
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "5") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Integer disabled) {
        
        Map<String, Object> result = new HashMap<>();
        
        // 未登录则返回未授权
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        
        // 构建查询条件
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        if (username != null && !username.isEmpty()) {
            wrapper.like("username", username);
        }
        if (phone != null && !phone.isEmpty()) {
            wrapper.like("phone", phone);
        }
        if (disabled != null) {
            wrapper.eq("status", disabled);
        }
        wrapper.orderByDesc("create_time");
        
        // 分页查询
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        Page<SysUser> resultPage = sysUserService.page(page, wrapper);
        
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("total", resultPage.getTotal());
        result.put("records", resultPage.getRecords());
        
        return result;
    }

    /**
     * 新增用户
     */
    @PostMapping("/add")
    public Map<String, Object> add(@RequestBody SysUser sysUser) {
        Map<String, Object> result = new HashMap<>();
        
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        
        // 检查用户名是否已存在
        SysUser existUser = sysUserService.getByUsername(sysUser.getUsername());
        if (existUser != null) {
            result.put("code", 400);
            result.put("msg", "用户名已存在");
            return result;
        }
        
        sysUser.setStatus(0); // 默认启用
        boolean success = sysUserService.save(sysUser);
        
        if (success) {
            result.put("code", 200);
            result.put("msg", "新增成功");
        } else {
            result.put("code", 500);
            result.put("msg", "新增失败");
        }
        return result;
    }

    /**
     * 更新用户
     */
    @PutMapping("/update")
    public Map<String, Object> update(@RequestBody SysUser sysUser) {
        Map<String, Object> result = new HashMap<>();
        
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        
        boolean success = sysUserService.updateById(sysUser);
        
        if (success) {
            result.put("code", 200);
            result.put("msg", "更新成功");
        } else {
            result.put("code", 500);
            result.put("msg", "更新失败");
        }
        return result;
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/delete/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        
        boolean success = sysUserService.removeById(id);
        
        if (success) {
            result.put("code", 200);
            result.put("msg", "删除成功");
        } else {
            result.put("code", 500);
            result.put("msg", "删除失败");
        }
        return result;
    }

    /**
     * 根据ID获取用户详情
     */
    @GetMapping("/get/{id}")
    public Map<String, Object> get(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        
        SysUser user = sysUserService.getById(id);
        if (user != null) {
            result.put("code", 200);
            result.put("msg", "查询成功");
            result.put("data", user);
        } else {
            result.put("code", 404);
            result.put("msg", "用户不存在");
        }
        return result;
    }
}
