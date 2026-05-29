package com.zjhl.project.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zjhl.project.entity.SysUser;
import com.zjhl.project.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

@RestController
public class LoginController {

    @Autowired
    private SysUserService sysUserService;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestParam String username, @RequestParam String password,HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        SysUser user = sysUserService.getByUsername(username);

        if (user == null) {
            result.put("code", 400);
            result.put("msg", "用户不存在");
            return result;
        }
        if (user.getStatus() == 0) {
            result.put("code", 400);
            result.put("msg", "账号已被禁用");
            return result;
        }

        // 明文对比
        if (!password.equals(user.getPassword())) {
            result.put("code", 400);
            result.put("msg", "密码错误");
            return result;
        }

        StpUtil.login(user.getId());
        result.put("code", 200);
        result.put("msg", "登录成功");
        result.put("token", StpUtil.getTokenValue());
        
        session.setAttribute("loginUser", user); // 加这一行
        return result;
    }

    @GetMapping("/logout")
    public Map<String, Object> logout() {
        StpUtil.logout();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "退出成功");
        return result;
    }
}