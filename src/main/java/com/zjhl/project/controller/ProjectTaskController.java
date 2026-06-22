package com.zjhl.project.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zjhl.project.entity.*;
import com.zjhl.project.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/project/task")
public class ProjectTaskController {

    @Autowired
    private ProjectTaskService projectTaskService;

    @Autowired
    private ProjectTaskAttachmentService projectTaskAttachmentService;

    @Autowired
    private SysMessageService sysMessageService;

    @Autowired
    private ProjectInfoService projectInfoService;

    @Autowired
    private ProjectFileService projectFileService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private ProjectStageService projectStageService;

    /**
     * 查询任务负责人下拉列表（status=1，sys_position in (1,2,3,4,5,6,7)，支持模糊搜索）
     */
    @GetMapping("/execUsers")
    public Map<String, Object> execUsers(@RequestParam(required = false) String keyword) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        wrapper.in("sys_position", 1, 2, 3, 4, 5, 6, 7);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like("real_name", keyword);
        }
        wrapper.orderByAsc("real_name");

        List<SysUser> users = sysUserService.list(wrapper);
        // 只返回必要字段
        List<Map<String, Object>> userList = users.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("realName", u.getRealName());
            map.put("phone", u.getPhone());
            return map;
        }).collect(Collectors.toList());

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", userList);
        return result;
    }

    /**
     * 获取项目详情（含附件），用于任务分配页面
     */
    @GetMapping("/projectDetail/{projectId}")
    public Map<String, Object> projectDetail(@PathVariable Long projectId) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        ProjectInfo project = projectInfoService.getById(projectId);
        if (project == null) {
            result.put("code", 404);
            result.put("msg", "项目不存在");
            return result;
        }

        // 查询项目附件（投标阶段 stage_type=1）
        QueryWrapper<ProjectFile> fileWrapper = new QueryWrapper<>();
        fileWrapper.eq("project_id", projectId);
        fileWrapper.eq("stage_type", 1);
        fileWrapper.orderByAsc("create_time");
        List<ProjectFile> files = projectFileService.list(fileWrapper);

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("project", project);
        result.put("files", files);
        return result;
    }

    /**
     * 查询项目下的任务列表
     */
    @GetMapping("/list/{projectId}")
    public Map<String, Object> taskList(@PathVariable Long projectId) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        QueryWrapper<ProjectTask> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId);
        wrapper.orderByAsc("create_time");
        List<ProjectTask> tasks = projectTaskService.list(wrapper);

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", tasks);
        return result;
    }

    /**
     * 新增任务
     */
    @PostMapping("/add")
    public Map<String, Object> addTask(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        ProjectTask task = new ProjectTask();
        task.setProjectId(Long.parseLong(params.get("projectId").toString()));
        task.setStageType(1); // 体系作战任务默认投标阶段
        // 设置当前登录用户为任务分配人
        Long loginUserId = StpUtil.getLoginIdAsLong();
        SysUser loginUser = sysUserService.getById(loginUserId);
        if (loginUser != null) {
            task.setAssignUserId(loginUser.getId());
            task.setAssignUserName(loginUser.getRealName());
            task.setAssignUserPhone(loginUser.getPhone());
        }
        if (params.get("taskType") != null) {
            task.setTaskType(Integer.parseInt(params.get("taskType").toString()));
        }
        task.setTaskContent((String) params.get("taskContent"));
        if (params.get("execUserId") != null) {
            task.setExecUserId(Long.parseLong(params.get("execUserId").toString()));
        }
        task.setExecUserName((String) params.get("execUserName"));
        task.setExecUserPhone((String) params.get("execUserPhone"));
        task.setNeedCar(params.get("needCar") != null ? Integer.parseInt(params.get("needCar").toString()) : 0);
        task.setTollFee(params.get("tollFee") != null ? new java.math.BigDecimal(params.get("tollFee").toString()) : java.math.BigDecimal.ZERO);
        task.setMileage(params.get("mileage") != null ? new java.math.BigDecimal(params.get("mileage").toString()) : java.math.BigDecimal.ZERO);
        task.setTaskEndTime(params.get("taskEndTime") != null && !params.get("taskEndTime").toString().isEmpty()
                ? LocalDateTime.parse(params.get("taskEndTime").toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
        task.setDeliverType(params.get("deliverType") != null ? Integer.parseInt(params.get("deliverType").toString()) : null);
        task.setTaskStatus(1); // 默认执行中
        task.setTaskDesc((String) params.get("taskDesc"));
        task.setRemark((String) params.get("remark"));
        task.setCreateTime(LocalDateTime.now());

        boolean success = projectTaskService.save(task);
        if (success) {
            result.put("code", 200);
            result.put("msg", "任务添加成功");
            result.put("taskId", task.getId());
        } else {
            result.put("code", 500);
            result.put("msg", "任务添加失败");
        }
        return result;
    }

    /**
     * 更新任务
     */
    @PutMapping("/update")
    public Map<String, Object> updateTask(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        Long id = Long.parseLong(params.get("id").toString());
        ProjectTask task = new ProjectTask();
        task.setId(id);
        if (params.get("assignUserId") != null) {
            task.setAssignUserId(Long.parseLong(params.get("assignUserId").toString()));
        }
        task.setAssignUserName((String) params.get("assignUserName"));
        task.setAssignUserPhone((String) params.get("assignUserPhone"));
        if (params.get("taskType") != null) {
            task.setTaskType(Integer.parseInt(params.get("taskType").toString()));
        }
        task.setTaskContent((String) params.get("taskContent"));
        if (params.get("execUserId") != null) {
            task.setExecUserId(Long.parseLong(params.get("execUserId").toString()));
        }
        task.setExecUserName((String) params.get("execUserName"));
        task.setExecUserPhone((String) params.get("execUserPhone"));
        task.setNeedCar(params.get("needCar") != null ? Integer.parseInt(params.get("needCar").toString()) : 0);
        task.setTollFee(params.get("tollFee") != null ? new java.math.BigDecimal(params.get("tollFee").toString()) : java.math.BigDecimal.ZERO);
        task.setMileage(params.get("mileage") != null ? new java.math.BigDecimal(params.get("mileage").toString()) : java.math.BigDecimal.ZERO);
        task.setTaskEndTime(params.get("taskEndTime") != null && !params.get("taskEndTime").toString().isEmpty()
                ? LocalDateTime.parse(params.get("taskEndTime").toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
        task.setDeliverType(params.get("deliverType") != null ? Integer.parseInt(params.get("deliverType").toString()) : null);
        task.setTaskDesc((String) params.get("taskDesc"));
        task.setRemark((String) params.get("remark"));

        boolean success = projectTaskService.updateById(task);
        if (success) {
            result.put("code", 200);
            result.put("msg", "任务更新成功");
        } else {
            result.put("code", 500);
            result.put("msg", "任务更新失败");
        }
        return result;
    }

    /**
     * 删除任务
     */
    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteTask(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        // 删除任务相关附件
        QueryWrapper<ProjectTaskAttachment> attachWrapper = new QueryWrapper<>();
        attachWrapper.eq("task_id", id);
        projectTaskAttachmentService.remove(attachWrapper);

        boolean success = projectTaskService.removeById(id);
        if (success) {
            result.put("code", 200);
            result.put("msg", "任务删除成功");
        } else {
            result.put("code", 500);
            result.put("msg", "任务删除失败");
        }
        return result;
    }

    /**
     * 批量保存任务（含消息通知）
     * 前端点击保存时调用，批量保存所有任务并向每个执行人发送消息通知
     */
    @PostMapping("/batchSave")
    public Map<String, Object> batchSave(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        Long projectId = Long.parseLong(params.get("projectId").toString());
        ProjectInfo project = projectInfoService.getById(projectId);
        if (project == null) {
            result.put("code", 404);
            result.put("msg", "项目不存在");
            return result;
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> taskList = (List<Map<String, Object>>) params.get("taskList");

        if (taskList == null || taskList.isEmpty()) {
            result.put("code", 400);
            result.put("msg", "任务列表不能为空");
            return result;
        }

        // 验证任务内容不为空
        for (Map<String, Object> taskMap : taskList) {
            String taskContent = (String) taskMap.get("taskContent");
            if (taskContent == null || taskContent.trim().isEmpty()) {
                result.put("code", 400);
                result.put("msg", "任务内容不能为空");
                return result;
            }
        }

        // 先删除该项目下已有的投标阶段任务
        QueryWrapper<ProjectTask> delWrapper = new QueryWrapper<>();
        delWrapper.eq("project_id", projectId);
        delWrapper.eq("stage_type", 1);
        // 获取旧任务ID用于清理附件和消息
        List<ProjectTask> oldTasks = projectTaskService.list(delWrapper);
        for (ProjectTask oldTask : oldTasks) {
            // 删除旧任务附件
            QueryWrapper<ProjectTaskAttachment> attachWrapper = new QueryWrapper<>();
            attachWrapper.eq("task_id", oldTask.getId());
            projectTaskAttachmentService.remove(attachWrapper);
        }
        projectTaskService.remove(delWrapper);

        // 获取当前登录用户作为任务分配人
        Long loginUserId = StpUtil.getLoginIdAsLong();
        SysUser loginUser = sysUserService.getById(loginUserId);

        // 逐个保存新任务
        for (Map<String, Object> taskMap : taskList) {
            ProjectTask task = new ProjectTask();
            task.setProjectId(projectId);
            task.setStageType(1);
            // 设置当前登录用户为任务分配人
            if (loginUser != null) {
                task.setAssignUserId(loginUser.getId());
                task.setAssignUserName(loginUser.getRealName());
                task.setAssignUserPhone(loginUser.getPhone());
            }
            if (taskMap.get("taskType") != null) {
                task.setTaskType(Integer.parseInt(taskMap.get("taskType").toString()));
            }
            task.setTaskContent((String) taskMap.get("taskContent"));
            if (taskMap.get("execUserId") != null) {
                task.setExecUserId(Long.parseLong(taskMap.get("execUserId").toString()));
            }
            task.setExecUserName((String) taskMap.get("execUserName"));
            task.setExecUserPhone((String) taskMap.get("execUserPhone"));
            task.setNeedCar(taskMap.get("needCar") != null ? Integer.parseInt(taskMap.get("needCar").toString()) : 0);
            task.setTollFee(taskMap.get("tollFee") != null ? new java.math.BigDecimal(taskMap.get("tollFee").toString()) : java.math.BigDecimal.ZERO);
            task.setMileage(taskMap.get("mileage") != null ? new java.math.BigDecimal(taskMap.get("mileage").toString()) : java.math.BigDecimal.ZERO);
            task.setTaskEndTime(taskMap.get("taskEndTime") != null && !taskMap.get("taskEndTime").toString().isEmpty()
                    ? LocalDateTime.parse(taskMap.get("taskEndTime").toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
            task.setDeliverType(taskMap.get("deliverType") != null ? Integer.parseInt(taskMap.get("deliverType").toString()) : null);
            task.setTaskStatus(0);
            task.setTaskDesc((String) taskMap.get("taskDesc"));
            task.setRemark((String) taskMap.get("remark"));
            task.setCreateTime(LocalDateTime.now());

            projectTaskService.save(task);

            // 向任务执行人发送消息通知
            if (task.getExecUserId() != null) {
                SysMessage message = new SysMessage();
                message.setProjectId(projectId);
                message.setTaskId(task.getId());
                message.setReceiveUserId(task.getExecUserId());
                message.setMsgType(1); // 任务消息
                message.setContent("您有一项新的体系作战任务待处理！项目名称为" + project.getProjectName());
                message.setReadStatus(0);
                message.setCreateTime(LocalDateTime.now());
                sysMessageService.save(message);
            }
        }

        // 更新项目阶段状态为执行中（1）
        QueryWrapper<ProjectStage> stageWrapper = new QueryWrapper<>();
        stageWrapper.eq("project_id", projectId);
        stageWrapper.eq("stage_type", 1);
        stageWrapper.orderByDesc("create_time");
        List<ProjectStage> stages = projectStageService.list(stageWrapper);
        if (!stages.isEmpty()) {
            ProjectStage latestStage = stages.get(0);
            latestStage.setStageStatus(1); // 执行中
            latestStage.setStartTime(LocalDateTime.now());
            projectStageService.updateById(latestStage);
        }

        result.put("code", 200);
        result.put("msg", "任务保存成功");
        return result;
    }

    /**
     * 查询项目的投标阶段状态
     */
    @GetMapping("/stageStatus/{projectId}")
    public Map<String, Object> stageStatus(@PathVariable Long projectId) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        QueryWrapper<ProjectStage> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId);
        wrapper.eq("stage_type", 1);
        wrapper.orderByDesc("create_time");
        List<ProjectStage> stages = projectStageService.list(wrapper);

        Integer stageStatus = null;
        if (!stages.isEmpty()) {
            stageStatus = stages.get(0).getStageStatus();
        }

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("stageStatus", stageStatus);
        return result;
    }
}
