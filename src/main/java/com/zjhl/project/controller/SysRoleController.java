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
import com.zjhl.project.entity.SysRole;
import com.zjhl.project.entity.SysRoleMenu;
import com.zjhl.project.service.SysRoleMenuService;
import com.zjhl.project.service.SysRoleService;

import cn.dev33.satoken.stp.StpUtil;

@RestController
@RequestMapping("/sys/role")
public class SysRoleController {

    @Autowired
    private SysRoleService sysRoleService;

    @Autowired
    private SysRoleMenuService sysRoleMenuService;

    /**
     * 角色列表（分页+条件查询）
     */
    @GetMapping("/list")
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) Integer status) {
        
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        
        Page<SysRole> page = new Page<>(pageNum, pageSize);
        QueryWrapper<SysRole> wrapper = new QueryWrapper<>();
        if (roleName != null && !roleName.isEmpty()) {
            wrapper.like("role_name", roleName);
        }
        if (roleCode != null && !roleCode.isEmpty()) {
            wrapper.like("role_code", roleCode);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("create_time");
        
        Page<SysRole> resultPage = sysRoleService.page(page, wrapper);
        
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("total", resultPage.getTotal());
        result.put("records", resultPage.getRecords());
        return result;
    }

    /**
     * 获取所有角色（不分页，用于下拉框）
     */
    @GetMapping("/all")
    public Map<String, Object> getAllRoles() {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        QueryWrapper<SysRole> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        wrapper.orderByAsc("id");
        List<SysRole> list = sysRoleService.list(wrapper);
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", list);
        return result;
    }

    /**
     * 新增角色（同时保存菜单权限）
     */
    @PostMapping("/add")
    public Map<String, Object> add(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        
        String roleName = (String) params.get("roleName");
        String roleCode = (String) params.get("roleCode");
        String roleDesc = (String) params.get("roleDesc");
        Integer status = params.get("status") != null ? Integer.parseInt(params.get("status").toString()) : 1;
        List<Integer> menuIds = params.get("menuIds") != null ? (List<Integer>) params.get("menuIds") : null;
        
        // 检查角色编码是否已存在
        SysRole existRole = sysRoleService.getByRoleCode(roleCode);
        if (existRole != null) {
            result.put("code", 400);
            result.put("msg", "角色编码已存在");
            return result;
        }
        
        // 保存角色基本信息
        SysRole role = new SysRole();
        role.setRoleName(roleName);
        role.setRoleCode(roleCode);
        role.setRoleDesc(roleDesc);
        role.setStatus(status);
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        
        boolean success = sysRoleService.save(role);
        if (success && menuIds != null && !menuIds.isEmpty()) {
            // 保存角色菜单权限
            saveRoleMenus(role.getId(), menuIds);
        }
        
        result.put("code", success ? 200 : 500);
        result.put("msg", success ? "新增成功" : "新增失败");
        return result;
    }

    /**
     * 更新角色（同时更新菜单权限）
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
        String roleName = (String) params.get("roleName");
        String roleCode = (String) params.get("roleCode");
        String roleDesc = (String) params.get("roleDesc");
        Integer status = params.get("status") != null ? Integer.parseInt(params.get("status").toString()) : 1;
        List<Integer> menuIds = params.get("menuIds") != null ? (List<Integer>) params.get("menuIds") : null;
        
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleName(roleName);
        role.setRoleCode(roleCode);
        role.setRoleDesc(roleDesc);
        role.setStatus(status);
        role.setUpdateTime(LocalDateTime.now());
        
        boolean success = sysRoleService.updateById(role);
        if (success) {
            // 先删除旧权限，再保存新权限
            QueryWrapper<SysRoleMenu> delWrapper = new QueryWrapper<>();
            delWrapper.eq("role_id", id);
            sysRoleMenuService.remove(delWrapper);
            
            if (menuIds != null && !menuIds.isEmpty()) {
                saveRoleMenus(id, menuIds);
            }
        }
        
        result.put("code", success ? 200 : 500);
        result.put("msg", success ? "更新成功" : "更新失败");
        return result;
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/delete/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        if(id == null ) {
        	 result.put("code", 200);
             result.put("msg", "请选择要删除的记录！");
             return result;
        }
        else if(id.longValue() == 1){
        	result.put("code", 403);
            result.put("msg", "超级管理员不能被删除！");
            return result;
        }
        // 先删除角色菜单关联
        QueryWrapper<SysRoleMenu> delWrapper = new QueryWrapper<>();
        delWrapper.eq("role_id", id);
        sysRoleMenuService.remove(delWrapper);
        
        boolean success = sysRoleService.removeById(id);
        result.put("code", success ? 200 : 500);
        result.put("msg", success ? "删除成功" : "删除失败");
        return result;
    }

    /**
     * 根据ID获取角色详情
     */
    @GetMapping("/get/{id}")
    public Map<String, Object> get(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        SysRole role = sysRoleService.getById(id);
        if (role != null) {
            result.put("code", 200);
            result.put("msg", "查询成功");
            result.put("data", role);
        } else {
            result.put("code", 404);
            result.put("msg", "角色不存在");
        }
        return result;
    }

    /**
     * 获取角色已分配的菜单ID列表
     */
    @GetMapping("/menus/{roleId}")
    public Map<String, Object> getRoleMenus(@PathVariable Long roleId) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        QueryWrapper<SysRoleMenu> wrapper = new QueryWrapper<>();
        wrapper.eq("role_id", roleId);
        List<SysRoleMenu> list = sysRoleMenuService.list(wrapper);
        List<Long> menuIds = list.stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList());
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", menuIds);
        return result;
    }

    /**
     * 保存角色菜单关联
     */
    private void saveRoleMenus(Long roleId, List<Integer> menuIds) {
        for (Integer menuId : menuIds) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(Long.valueOf(menuId));
            rm.setCreateTime(LocalDateTime.now());
            sysRoleMenuService.save(rm);
        }
    }
}