package com.zjhl.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjhl.project.entity.SysMenu;
import com.zjhl.project.mapper.SysMenuMapper;
import com.zjhl.project.service.SysMenuService;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SysMenuServiceImpl 
        extends ServiceImpl<SysMenuMapper, SysMenu> 
        implements SysMenuService {

    @Resource
    private SysMenuMapper sysMenuMapper;

    @Override
    public List<SysMenu> getMenuTree() {
        List<SysMenu> allMenu = sysMenuMapper.selectAllMenu();

        Map<Long, List<SysMenu>> map = new HashMap<>();
        for (SysMenu menu : allMenu) {
            Long pid = menu.getParentId();
            if (!map.containsKey(pid)) {
                map.put(pid, new ArrayList<SysMenu>());
            }
            map.get(pid).add(menu);
        }

        List<SysMenu> rootList = new ArrayList<>();
        if (map.containsKey(0L)) {
            rootList = map.get(0L);
        }

        for (SysMenu root : rootList) {
            if (map.containsKey(root.getId())) {
                root.setChildren(map.get(root.getId()));
            } else {
                root.setChildren(new ArrayList<SysMenu>());
            }
        }
        return rootList;
    }
}