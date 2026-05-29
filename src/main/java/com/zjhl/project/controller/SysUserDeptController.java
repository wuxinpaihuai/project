package com.zjhl.project.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zjhl.project.entity.SysDept;
import com.zjhl.project.entity.SysUserDept;
import com.zjhl.project.service.SysDeptService;
import com.zjhl.project.service.SysUserDeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/sys/userDept")
public class SysUserDeptController {

    @Autowired
    private SysUserDeptService sysUserDeptService;
    
    @Autowired
    private SysDeptService sysDeptService;

    /**
     * 获取用户的所有部门
     */
    @GetMapping("/listByUser/{userId}")
    public Map<String, Object> listByUser(@PathVariable Long userId) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        
        QueryWrapper<SysUserDept> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        List<SysUserDept> userDepts = sysUserDeptService.list(wrapper);
        
        // 获取部门信息
        if (!userDepts.isEmpty()) {
            List<Long> deptIds = userDepts.stream()
                    .map(SysUserDept::getDeptId)
                    .collect(Collectors.toList());
            List<SysDept> depts = sysDeptService.listByIds(deptIds);
            result.put("data", depts);
        } else {
            result.put("data",  Collections.EMPTY_LIST);
        }
        
        result.put("code", 200);
        result.put("msg", "查询成功");
        return result;
    }

    /**
     * 分配部门给用户
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
        List<Long> deptIds = (List<Long>) params.get("deptIds");
        
        // 先删除用户原有的部门
        QueryWrapper<SysUserDept> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        sysUserDeptService.remove(wrapper);
        
        // 添加新的部门
        for (Long deptId : deptIds) {
            SysUserDept userDept = new SysUserDept();
            userDept.setUserId(userId);
            userDept.setDeptId(deptId);
            sysUserDeptService.save(userDept);
        }
        
        result.put("code", 200);
        result.put("msg", "分配成功");
        return result;
    }
}
