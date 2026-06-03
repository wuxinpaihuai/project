package com.zjhl.project.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjhl.project.entity.SysDept;
import com.zjhl.project.service.SysDeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sys/dept")
public class SysDeptController {

    @Autowired
    private SysDeptService sysDeptService;

    /**
     * 获取部门树
     */
    @GetMapping("/tree")
    public Map<String, Object> getDeptTree() {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        List<SysDept> tree = sysDeptService.getDeptTree();
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", tree);
        return result;
    }

    /**
     * 部门列表（分页+条件查询）
     */
    @GetMapping("/list")
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String deptName,
            @RequestParam(required = false) String deptCode,
            @RequestParam(required = false) Integer status) {
        
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        
        SysDept dept = new SysDept();
        dept.setDeptName(deptName);
        dept.setDeptCode(deptCode);
        dept.setStatus(status);
        
        Page<SysDept> page = new Page<>(pageNum, pageSize);
        QueryWrapper<SysDept> wrapper = new QueryWrapper<>();
        if (deptName != null && !deptName.isEmpty()) {
            wrapper.like("dept_name", deptName);
        }
        if (deptCode != null && !deptCode.isEmpty()) {
            wrapper.like("dept_code", deptCode);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByAsc("sort_num");
        
        Page<SysDept> resultPage = sysDeptService.page(page, wrapper);
        
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("total", resultPage.getTotal());
        result.put("records", resultPage.getRecords());
        return result;
    }

    /**
     * 新增部门
     */
    @PostMapping("/add")
    public Map<String, Object> add(@RequestBody SysDept dept) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        dept.setCreateTime(LocalDateTime.now());
        dept.setUpdateTime(LocalDateTime.now());
        boolean success = sysDeptService.save(dept);
        result.put("code", success ? 200 : 500);
        result.put("msg", success ? "新增成功" : "新增失败");
        return result;
    }

    /**
     * 更新部门
     */
    @PutMapping("/update")
    public Map<String, Object> update(@RequestBody SysDept dept) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        dept.setUpdateTime(LocalDateTime.now());
        boolean success = sysDeptService.updateById(dept);
        result.put("code", success ? 200 : 500);
        result.put("msg", success ? "更新成功" : "更新失败");
        return result;
    }

    /**
     * 删除部门
     */
    @DeleteMapping("/delete/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        // 检查是否存在子部门
        QueryWrapper<SysDept> childWrapper = new QueryWrapper<>();
        childWrapper.eq("parent_id", id);
        long childCount = sysDeptService.count(childWrapper);
        if (childCount > 0) {
            result.put("code", 500);
            result.put("msg", "该部门下存在子部门，无法删除");
            return result;
        }
        boolean success = sysDeptService.removeById(id);
        result.put("code", success ? 200 : 500);
        result.put("msg", success ? "删除成功" : "删除失败");
        return result;
    }


    /**
     * 根据ID获取部门详情
     */
    @GetMapping("/get/{id}")
    public Map<String, Object> get(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        SysDept dept = sysDeptService.getById(id);
        if (dept != null) {
            result.put("code", 200);
            result.put("msg", "查询成功");
            result.put("data", dept);
        } else {
            result.put("code", 404);
            result.put("msg", "部门不存在");
        }
        return result;
    }
}
