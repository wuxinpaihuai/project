package com.zjhl.project.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjhl.project.entity.SysUser;
import com.zjhl.project.entity.SysUserDept;
import com.zjhl.project.service.SysUserService;
import com.zjhl.project.service.SysDeptService;
import com.zjhl.project.service.SysUserDeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/sys/user")
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysUserDeptService sysUserDeptService;
    
    @Autowired
    private SysDeptService sysDeptService;

    @GetMapping("/list")
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "5") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Integer disabled,
            @RequestParam(required = false) Long deptId) {
        
        Map<String, Object> result = new HashMap<>();
        
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
        
        // ========== 按部门过滤（包含子部门） ==========
        if (deptId != null) {
            // 1. 获取该部门及所有子部门ID
            List<Long> allDeptIds = sysDeptService.getAllChildDeptIds(deptId);
            
            // 2. 从中间表查出这些部门下的所有用户ID
            QueryWrapper<SysUserDept> udWrapper = new QueryWrapper<>();
            udWrapper.in("dept_id", allDeptIds);
            List<SysUserDept> udList = sysUserDeptService.list(udWrapper);
            
            List<Long> userIds = udList.stream()
                    .map(SysUserDept::getUserId)
                    .distinct()  // 去重，一个用户可能在多个子部门
                    .collect(Collectors.toList());
            
            if (userIds.isEmpty()) {
                result.put("code", 200);
                result.put("msg", "查询成功");
                result.put("total", 0);
                result.put("records", java.util.Collections.emptyList());
                return result;
            }
            
            wrapper.in("id", userIds);
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
