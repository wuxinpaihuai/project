package com.zjhl.project.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.zjhl.project.entity.SysMenu;
import com.zjhl.project.service.SysMenuService;

import cn.dev33.satoken.stp.StpUtil;

@RestController
@RequestMapping("/sys/menu")
public class SysMenuController {

    @Autowired
    private SysMenuService sysMenuService;

    /**
     * 获取菜单树
     */
    @GetMapping("/tree")
    public Map<String, Object> getMenuTree() {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        List<SysMenu> tree = sysMenuService.getMenuTree();
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", tree);
        return result;
    }

    /**
     * 菜单列表（分页+条件查询）
     */
    @GetMapping("/list")
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String menuName,
            @RequestParam(required = false) Integer status) {
        
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        
        Page<SysMenu> page = new Page<>(pageNum, pageSize);
        QueryWrapper<SysMenu> wrapper = new QueryWrapper<>();
        if (menuName != null && !menuName.isEmpty()) {
            wrapper.like("menu_name", menuName);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByAsc("sort_num");
        
        Page<SysMenu> resultPage = sysMenuService.page(page, wrapper);
        
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("total", resultPage.getTotal());
        result.put("records", resultPage.getRecords());
        return result;
    }

    /**
     * 获取所有菜单（不分页，用于下拉框和树形）
     */
    @GetMapping("/all")
    public Map<String, Object> getAllMenus() {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        QueryWrapper<SysMenu> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        wrapper.orderByAsc("sort_num");
        List<SysMenu> list = sysMenuService.list(wrapper);
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", list);
        return result;
    }

    /**
     * 获取一级菜单（parentId=0，用于新增菜单时的父菜单下拉框）
     */
    @GetMapping("/parents")
    public Map<String, Object> getParentMenus() {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        QueryWrapper<SysMenu> wrapper = new QueryWrapper<>();
        wrapper.eq("parent_id", 0);
        wrapper.eq("status", 1);
        wrapper.orderByAsc("sort_num");
        List<SysMenu> list = sysMenuService.list(wrapper);
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", list);
        return result;
    }

    /**
     * 新增菜单
     */
    @PostMapping("/add")
    public Map<String, Object> add(@RequestBody SysMenu menu) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        boolean success = sysMenuService.save(menu);
        result.put("code", success ? 200 : 500);
        result.put("msg", success ? "新增成功" : "新增失败");
        return result;
    }

    /**
     * 更新菜单
     */
    @PutMapping("/update")
    public Map<String, Object> update(@RequestBody SysMenu menu) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        boolean success = sysMenuService.updateById(menu);
        result.put("code", success ? 200 : 500);
        result.put("msg", success ? "更新成功" : "更新失败");
        return result;
    }

    /**
     * 删除菜单
     */
    @DeleteMapping("/delete/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        
        // 检查是否有子菜单
        QueryWrapper<SysMenu> wrapper = new QueryWrapper<>();
        wrapper.eq("parent_id", id);
        long count = sysMenuService.count(wrapper);
        if (count > 0) {
            result.put("code", 400);
            result.put("msg", "该菜单下存在子菜单，请先删除子菜单");
            return result;
        }
        
        boolean success = sysMenuService.removeById(id);
        result.put("code", success ? 200 : 500);
        result.put("msg", success ? "删除成功" : "删除失败");
        return result;
    }

    /**
     * 根据ID获取菜单详情
     */
    @GetMapping("/get/{id}")
    public Map<String, Object> get(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        SysMenu menu = sysMenuService.getById(id);
        if (menu != null) {
            result.put("code", 200);
            result.put("msg", "查询成功");
            result.put("data", menu);
        } else {
            result.put("code", 404);
            result.put("msg", "菜单不存在");
        }
        return result;
    }
}
