package com.zjhl.project.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjhl.project.entity.ProjectTask;
import com.zjhl.project.entity.SysMessage;
import com.zjhl.project.service.ProjectTaskService;
import com.zjhl.project.service.SysMessageService;

import cn.dev33.satoken.stp.StpUtil;

@RestController
@RequestMapping("/workplat")
public class ProjectWorkPlatController {

    @Autowired
    private SysMessageService sysMessageService;

    @Autowired
    private ProjectTaskService projectTaskService;

    /**
     * 消息通知列表（分页）
     * @param page 页码
     * @param limit 每页条数
     * @param readStatus 已读/未读筛选：0=未读，1=已读，空=全部
     */
    @GetMapping("/message/list")
    public Map<String, Object> messageList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(required = false) Integer readStatus) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        Long userId = StpUtil.getLoginIdAsLong();

        QueryWrapper<SysMessage> wrapper = new QueryWrapper<>();
        wrapper.eq("receive_user_id", userId);
        if (readStatus != null) {
            wrapper.eq("read_status", readStatus);
        }
        // 未读排前（read_status=0），已读排后（read_status=1），各自按时间倒序
        wrapper.orderByAsc("read_status");
        wrapper.orderByDesc("create_time");

        IPage<SysMessage> pageResult = sysMessageService.page(new Page<>(page, limit), wrapper);

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("total", pageResult.getTotal());
        result.put("records", pageResult.getRecords());
        return result;
    }

    /**
     * 标记消息为已读
     */
    @PostMapping("/message/read/{id}")
    public Map<String, Object> markRead(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        SysMessage msg = new SysMessage();
        msg.setId(id);
        msg.setReadStatus(1);
        msg.setReadTime(LocalDateTime.now());
        boolean success = sysMessageService.updateById(msg);

        result.put("code", success ? 200 : 500);
        result.put("msg", success ? "标记成功" : "标记失败");
        return result;
    }

    /**
     * 待办事项列表（分页）
     * 查询当前用户执行中(task_status=1)和延时执行中(task_status=2)的任务
     */
    @GetMapping("/todo/list")
    public Map<String, Object> todoList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        Long userId = StpUtil.getLoginIdAsLong();

        QueryWrapper<ProjectTask> wrapper = new QueryWrapper<>();
        wrapper.eq("exec_user_id", userId);
        wrapper.in("task_status", 1, 2);
        wrapper.orderByAsc("task_end_time");

        IPage<ProjectTask> pageResult = projectTaskService.page(new Page<>(page, limit), wrapper);

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("total", pageResult.getTotal());
        result.put("records", pageResult.getRecords());
        return result;
    }

    /**
     * 日历模块 - 查询当月有未完成任务的日期及任务列表
     * @param year 年份
     * @param month 月份(1-12)
     */
    @GetMapping("/calendar/tasks")
    public Map<String, Object> calendarTasks(
            @RequestParam Integer year,
            @RequestParam Integer month) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        Long userId = StpUtil.getLoginIdAsLong();

        // 当月起止时间
        LocalDateTime monthStart = LocalDateTime.of(year, month, 1, 0, 0, 0);
        LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);

        // 查询当前用户在当月有截止日期的未完成任务（task_status=1,2）
        QueryWrapper<ProjectTask> wrapper = new QueryWrapper<>();
        wrapper.eq("exec_user_id", userId);
        wrapper.in("task_status", 1, 2);
        wrapper.isNotNull("task_end_time");
        wrapper.ge("task_end_time", monthStart);
        wrapper.le("task_end_time", monthEnd);

        List<ProjectTask> tasks = projectTaskService.list(wrapper);

        // 按日期分组
        Map<String, List<Map<String, Object>>> dateTaskMap = new LinkedHashMap<>();
        for (ProjectTask task : tasks) {
            if (task.getTaskEndTime() != null) {
                String dateKey = task.getTaskEndTime().toLocalDate().toString(); // yyyy-MM-dd
                Map<String, Object> taskInfo = new HashMap<>();
                taskInfo.put("content", task.getTaskContent());
                taskInfo.put("taskEndTime", task.getTaskEndTime().toString());
                dateTaskMap.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(taskInfo);
            }
        }

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", dateTaskMap);
        return result;
    }
}
