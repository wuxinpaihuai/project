package com.zjhl.project.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zjhl.project.entity.SysMenu;

//这里必须继承 IService<SysMenu>
public interface SysMenuService extends IService<SysMenu> {
 List<SysMenu> getMenuTree();
}