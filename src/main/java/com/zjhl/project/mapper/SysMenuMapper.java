package com.zjhl.project.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zjhl.project.entity.SysMenu;

public interface SysMenuMapper extends BaseMapper<SysMenu> {

    @Select("SELECT id,parent_id,menu_name,menu_icon,menu_path,sort_num,status " +
            "FROM sys_menu ORDER BY sort_num ASC")
    List<SysMenu> selectAllMenu();
}