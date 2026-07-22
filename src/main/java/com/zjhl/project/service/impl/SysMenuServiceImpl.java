package com.zjhl.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjhl.project.entity.SysMenu;
import com.zjhl.project.entity.SysRoleMenu;
import com.zjhl.project.entity.SysUserRole;
import com.zjhl.project.mapper.SysMenuMapper;
import com.zjhl.project.service.SysMenuService;
import com.zjhl.project.service.SysRoleMenuService;
import com.zjhl.project.service.SysUserRoleService;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SysMenuServiceImpl 
        extends ServiceImpl<SysMenuMapper, SysMenu> 
        implements SysMenuService {

    @Resource
    private SysMenuMapper sysMenuMapper;

    @Resource
    private SysUserRoleService sysUserRoleService;

    @Resource
    private SysRoleMenuService sysRoleMenuService;

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

    @Override
    public List<SysMenu> getMenuTreeByUserId(Long userId) {
        // 1. 查询当前用户的所有角色
        QueryWrapper<SysUserRole> userRoleWrapper = new QueryWrapper<>();
        userRoleWrapper.eq("user_id", userId);
        List<Long> roleIds = sysUserRoleService.list(userRoleWrapper).stream()
                .map(SysUserRole::getRoleId)
                .distinct()
                .collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 查询这些角色拥有的菜单ID
        QueryWrapper<SysRoleMenu> roleMenuWrapper = new QueryWrapper<>();
        roleMenuWrapper.in("role_id", roleIds);
        List<Long> menuIds = sysRoleMenuService.list(roleMenuWrapper).stream()
                .map(SysRoleMenu::getMenuId)
                .distinct()
                .collect(Collectors.toList());
        if (menuIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. 查询状态正常的菜单
        QueryWrapper<SysMenu> menuWrapper = new QueryWrapper<>();
        menuWrapper.in("id", menuIds)
                .eq("status", 1)
                .orderByAsc("sort_num");
        List<SysMenu> menus = list(menuWrapper);

        // 4. 若子菜单已授权但父菜单未授权，把父菜单补出来用于展示
        Set<Long> missingParentIds = menus.stream()
                .map(SysMenu::getParentId)
                .filter(pid -> pid != null && pid != 0L && !menuIds.contains(pid))
                .collect(Collectors.toSet());
        if (!missingParentIds.isEmpty()) {
            QueryWrapper<SysMenu> parentWrapper = new QueryWrapper<>();
            parentWrapper.in("id", missingParentIds)
                    .eq("status", 1)
                    .orderByAsc("sort_num");
            menus.addAll(list(parentWrapper));
        }

        // 5. 组装两级树
        Map<Long, List<SysMenu>> childrenMap = new HashMap<>();
        List<SysMenu> rootList = new ArrayList<>();
        for (SysMenu menu : menus) {
            Long pid = menu.getParentId() == null ? 0L : menu.getParentId();
            if (pid == 0L) {
                rootList.add(menu);
                menu.setChildren(new ArrayList<>());
            } else {
                childrenMap.computeIfAbsent(pid, k -> new ArrayList<>()).add(menu);
            }
        }

        rootList.sort(Comparator.comparing(SysMenu::getSortNum, Comparator.nullsLast(Integer::compareTo)));
        for (SysMenu root : rootList) {
            List<SysMenu> children = childrenMap.getOrDefault(root.getId(), new ArrayList<>());
            children.sort(Comparator.comparing(SysMenu::getSortNum, Comparator.nullsLast(Integer::compareTo)));
            root.setChildren(children);
        }
        return rootList;
    }
}