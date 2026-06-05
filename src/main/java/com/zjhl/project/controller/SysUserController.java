package com.zjhl.project.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjhl.project.entity.SysUser;
import com.zjhl.project.entity.SysUserDept;
import com.zjhl.project.entity.SysUserRole;
import com.zjhl.project.service.SysDeptService;
import com.zjhl.project.service.SysUserDeptService;
import com.zjhl.project.service.SysUserRoleService;
import com.zjhl.project.service.SysUserService;

import cn.dev33.satoken.stp.StpUtil;

@RestController
@RequestMapping("/sys/user")
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysUserDeptService sysUserDeptService;
    
    @Autowired
    private SysDeptService sysDeptService;
    
    @Autowired
    private SysUserRoleService sysUserRoleService;
    
    

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
     * 新增用户（同时保存部门、角色关联）
     */
    @PostMapping("/add")
    public Map<String, Object> add(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        
        String username = (String) params.get("username");
        String password = (String) params.get("password");
        String realName = (String) params.get("realName");
        String phone = (String) params.get("phone");
        String sysPosition = params.get("sysPosition") != null ? params.get("sysPosition").toString() : null;
        List<Integer> deptIds = params.get("deptIds") != null ? (List<Integer>) params.get("deptIds") : null;
        List<Integer> roleIds = params.get("roleIds") != null ? (List<Integer>) params.get("roleIds") : null;
        Integer status = params.get("status") != null ? Integer.parseInt(params.get("status").toString()) : null;
        
        // 检查用户名是否已存在
        SysUser existUser = sysUserService.getByUsername(username);
        if (existUser != null) {
            result.put("code", 400);
            result.put("msg", "用户名已存在");
            return result;
        }
        
        // 保存用户基本信息
        SysUser sysUser = new SysUser();
        sysUser.setUsername(username);
        sysUser.setPassword(password);
        sysUser.setRealName(realName);
        sysUser.setPhone(phone);
        sysUser.setSysPosition(sysPosition);
        sysUser.setStatus(status); 
        sysUser.setCreateTime(LocalDateTime.now());
        
        boolean success = sysUserService.save(sysUser);
        
        if (success) {
            // 保存用户-部门关联
            if (deptIds != null && !deptIds.isEmpty()) {
                saveUserDepts(sysUser.getId(), deptIds);
            }
            // 保存用户-角色关联
            if (roleIds != null && !roleIds.isEmpty()) {
                saveUserRoles(sysUser.getId(), roleIds);
            }
            result.put("code", 200);
            result.put("msg", "新增成功");
        } else {
            result.put("code", 500);
            result.put("msg", "新增失败");
        }
        return result;
    }

    /**
     * 更新用户（同时更新部门、角色关联）
     */
    @PutMapping("/update")
    public Map<String, Object> update(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        
        Long id = Long.parseLong(params.get("id").toString());
        String username = (String) params.get("username");
        String password = (String) params.get("password");
        String realName = (String) params.get("realName");
        String phone = (String) params.get("phone");
        String sysPosition = params.get("sysPosition") != null ? params.get("sysPosition").toString() : null;
        Integer status = params.get("status") != null ? Integer.parseInt(params.get("status").toString()) : null;
        List<Integer> deptIds = params.get("deptIds") != null ? (List<Integer>) params.get("deptIds") : null;
        List<Integer> roleIds = params.get("roleIds") != null ? (List<Integer>) params.get("roleIds") : null;
        
        // 检查用户名是否已被其他用户占用
        if (username != null && !username.isEmpty()) {
            SysUser existUser = sysUserService.getByUsername(username);
            if (existUser != null && !existUser.getId().equals(id)) {
                result.put("code", 400);
                result.put("msg", "用户名已存在");
                return result;
            }
        }
        
        SysUser sysUser = new SysUser();
        sysUser.setId(id);
        sysUser.setUsername(username);
        if (password != null && !password.isEmpty()) {
            sysUser.setPassword(password);
        }
        sysUser.setRealName(realName);
        sysUser.setPhone(phone);
        sysUser.setSysPosition(sysPosition);
        if (status != null) {
            sysUser.setStatus(status);
        }
        
        boolean success = sysUserService.updateById(sysUser);
        if (success) {
            // 先删除旧的部门关联，再保存新的
            QueryWrapper<SysUserDept> delDeptWrapper = new QueryWrapper<>();
            delDeptWrapper.eq("user_id", id);
            sysUserDeptService.remove(delDeptWrapper);
            if (deptIds != null && !deptIds.isEmpty()) {
                saveUserDepts(id, deptIds);
            }
            
            // 先删除旧的角色关联，再保存新的
            QueryWrapper<SysUserRole> delRoleWrapper = new QueryWrapper<>();
            delRoleWrapper.eq("user_id", id);
            sysUserRoleService.remove(delRoleWrapper);
            if (roleIds != null && !roleIds.isEmpty()) {
                saveUserRoles(id, roleIds);
            }
        }
        
        result.put("code", success ? 200 : 500);
        result.put("msg", success ? "更新成功" : "更新失败");
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
    
    /**
     * 获取用户已分配的角色ID列表
     */
    @GetMapping("/roles/{userId}")
    public Map<String, Object> getUserRoles(@PathVariable Long userId) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        QueryWrapper<SysUserRole> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        List<SysUserRole> list = sysUserRoleService.list(wrapper);
        List<Long> roleIds = list.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", roleIds);
        return result;
    }

    /**
     * 保存用户-部门关联
     */
    private void saveUserDepts(Long userId, List<Integer> deptIds) {
        for (Integer deptId : deptIds) {
            SysUserDept ud = new SysUserDept();
            ud.setUserId(userId);
            ud.setDeptId(Long.valueOf(deptId));
            ud.setCreateTime(LocalDateTime.now());
            sysUserDeptService.save(ud);
        }
    }

    /**
     * 保存用户-角色关联
     */
    private void saveUserRoles(Long userId, List<Integer> roleIds) {
        for (Integer roleId : roleIds) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(Long.valueOf(roleId));
            ur.setCreateTime(LocalDateTime.now());
            sysUserRoleService.save(ur);
        }
    }
}
