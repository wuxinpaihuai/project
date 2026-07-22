package com.zjhl.project.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zjhl.project.entity.SysMenu;
import com.zjhl.project.service.SysMenuService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class MenuController {
    @Resource
    private SysMenuService sysMenuService;

    // 获取树形菜单接口
    @GetMapping("/getMenuTree")
    public Map<String,Object> getMenuTree(){
        Map<String,Object> res = new HashMap<>();
        List<SysMenu> tree = sysMenuService.getMenuTree();
        res.put("code",200);
        res.put("data",tree);
        return res;
    }

    // 获取当前登录用户的角色菜单树
    @GetMapping("/getUserMenuTree")
    public Map<String,Object> getUserMenuTree(){
        Map<String,Object> res = new HashMap<>();
        if (!StpUtil.isLogin()) {
            res.put("code",401);
            res.put("msg","未登录");
            return res;
        }
        Long userId = StpUtil.getLoginIdAsLong();
        List<SysMenu> tree = sysMenuService.getMenuTreeByUserId(userId);
        res.put("code",200);
        res.put("data",tree);
        return res;
    }
}