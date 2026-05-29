package com.zjhl.project.controller;

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
}