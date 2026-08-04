package com.zjhl.project.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjhl.project.entity.ProjectInfo;
import com.zjhl.project.entity.ProjectTask;
import com.zjhl.project.entity.ProjectTaskAttachment;
import com.zjhl.project.service.ProjectInfoService;
import com.zjhl.project.service.ProjectTaskAttachmentService;
import com.zjhl.project.service.ProjectTaskService;

import cn.dev33.satoken.stp.StpUtil;

@RestController
@RequestMapping("/project/task")
public class ProjectTaskCompleteController {
	@Autowired
    private ProjectTaskService projectTaskService;

    @Autowired
    private ProjectInfoService projectInfoService;

    @Autowired
    private ProjectTaskAttachmentService projectTaskAttachmentService;



    /**
     * 我的任务列表 - 查询分配到当前登录用户的任务（分页）
     */
    @GetMapping("/myTaskList")
    public Map<String, Object> myTaskList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String projectName,
            @RequestParam(required = false) String taskStatus) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        Long loginUserId = StpUtil.getLoginIdAsLong();

        // 解析任务状态（兼容空字符串、null、数字字符串）
        Integer status = null;
        if (taskStatus != null && !taskStatus.isEmpty()) {
            try {
                status = Integer.parseInt(taskStatus);
            } catch (NumberFormatException e) {
                result.put("code", 400);
                result.put("msg", "任务状态参数格式错误");
                return result;
            }
        }

        // 1. 如果有项目名称筛选，先查匹配的项目ID
        List<Long> projectIds = null;
        if (projectName != null && !projectName.isEmpty()) {
            QueryWrapper<ProjectInfo> piWrapper = new QueryWrapper<>();
            piWrapper.like("project_name", projectName);
            List<ProjectInfo> projects = projectInfoService.list(piWrapper);
            projectIds = projects.stream().map(ProjectInfo::getId).collect(Collectors.toList());
            if (projectIds.isEmpty()) {
                result.put("code", 200);
                result.put("msg", "查询成功");
                result.put("total", 0);
                result.put("records", Collections.EMPTY_LIST);
                return result;
            }
        }

        // 2. 查询分配到当前用户的实施任务
        QueryWrapper<ProjectTask> taskWrapper = new QueryWrapper<>();
        taskWrapper.eq("exec_user_id", loginUserId);
        taskWrapper.eq("stage_type", 3); // 实施阶段
        if (projectIds != null) {
            taskWrapper.in("project_id", projectIds);
        }
        if (status != null) {
            taskWrapper.eq("task_status", status);
        }
        taskWrapper.orderByDesc("task_end_time");

        Page<ProjectTask> page = new Page<>(pageNum, pageSize);
        Page<ProjectTask> resultPage = projectTaskService.page(page, taskWrapper);

        // 3. 为每条任务拼接项目名称和附件列表
        List<Map<String, Object>> records = new ArrayList<>();
        // 批量获取项目名称
        List<Long> allProjectIds = resultPage.getRecords().stream()
                .map(ProjectTask::getProjectId).distinct().collect(Collectors.toList());
        Map<Long, String> projectNameMap = new HashMap<>();
        if (!allProjectIds.isEmpty()) {
            List<ProjectInfo> projectInfos = projectInfoService.listByIds(allProjectIds);
            for (ProjectInfo pi : projectInfos) {
                projectNameMap.put(pi.getId(), pi.getProjectName());
            }
        }
        // 批量获取附件
        List<Long> allTaskIds = resultPage.getRecords().stream()
                .map(ProjectTask::getId).collect(Collectors.toList());
        Map<Long, List<ProjectTaskAttachment>> attachMap = new HashMap<>();
        if (!allTaskIds.isEmpty()) {
            QueryWrapper<ProjectTaskAttachment> attWrapper = new QueryWrapper<>();
            attWrapper.in("task_id", allTaskIds).eq("attach_type", 2);
            List<ProjectTaskAttachment> allAttachs = projectTaskAttachmentService.list(attWrapper);
            for (ProjectTaskAttachment att : allAttachs) {
                attachMap.computeIfAbsent(att.getTaskId(), k -> new ArrayList<>()).add(att);
            }
        }

        for (ProjectTask task : resultPage.getRecords()) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", task.getId());
            row.put("projectId", task.getProjectId());
            row.put("projectName", projectNameMap.getOrDefault(task.getProjectId(), "-"));
            row.put("taskContent", task.getTaskContent());
            row.put("workAmount", task.getWorkAmount());
            row.put("taskEndTime", task.getTaskEndTime() != null ? task.getTaskEndTime().toString() : null);
            row.put("taskStatus", task.getTaskStatus());
            row.put("execFinishTime", task.getExecFinishTime() != null ? task.getExecFinishTime().toString() : null);
            row.put("workContent", task.getWorkContent());
            row.put("remark", task.getRemark());
            row.put("assignUserName", task.getAssignUserName());
            row.put("assignUserPhone", task.getAssignUserPhone());
            row.put("needCar", task.getNeedCar());
            row.put("tollFee", task.getTollFee());
            row.put("mileage", task.getMileage());
            // 附件列表
            List<ProjectTaskAttachment> taskAttachs = attachMap.getOrDefault(task.getId(), new ArrayList<>());
            List<Map<String, Object>> attachList = new ArrayList<>();
            for (ProjectTaskAttachment att : taskAttachs) {
                Map<String, Object> attMap = new HashMap<>();
                attMap.put("id", att.getId());
                attMap.put("fileName", att.getFileName());
                attMap.put("filePath", att.getFilePath());
                attachList.add(attMap);
            }
            row.put("attachments", attachList);
            records.add(row);
        }

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("total", resultPage.getTotal());
        result.put("records", records);
        return result;
    }

    /**
     * 任务详情 - 返回任务信息、项目名称、附件列表
     */
    @GetMapping("/taskDetail/{taskId}")
    public Map<String, Object> taskDetail(@PathVariable Long taskId) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        ProjectTask task = projectTaskService.getById(taskId);
        if (task == null) {
            result.put("code", 404);
            result.put("msg", "任务不存在");
            return result;
        }

        // 项目名称
        ProjectInfo project = projectInfoService.getById(task.getProjectId());
        String projectName = project != null ? project.getProjectName() : "-";

        // 附件列表（attach_type=2 执行时附件）
        QueryWrapper<ProjectTaskAttachment> attWrapper = new QueryWrapper<>();
        attWrapper.eq("task_id", taskId).eq("attach_type", 2);
        List<ProjectTaskAttachment> attachments = projectTaskAttachmentService.list(attWrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("id", task.getId());
        data.put("projectId", task.getProjectId());
        data.put("projectName", projectName);
        data.put("assignUserName", task.getAssignUserName());
        data.put("assignUserPhone", task.getAssignUserPhone());
        data.put("taskContent", task.getTaskContent());
        data.put("workAmount", task.getWorkAmount());
        data.put("taskEndTime", task.getTaskEndTime() != null ? task.getTaskEndTime().toString() : null);
        data.put("taskStatus", task.getTaskStatus());
        data.put("execFinishTime", task.getExecFinishTime() != null ? task.getExecFinishTime().toString() : null);
        data.put("workContent", task.getWorkContent());
        data.put("remark", task.getRemark());
        data.put("execUserName", task.getExecUserName());

        List<Map<String, Object>> attachList = new ArrayList<>();
        for (ProjectTaskAttachment att : attachments) {
            Map<String, Object> attMap = new HashMap<>();
            attMap.put("id", att.getId());
            attMap.put("fileName", att.getFileName());
            attMap.put("filePath", att.getFilePath());
            attachList.add(attMap);
        }
        data.put("attachments", attachList);

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", data);
        return result;
    }

    /**
     * 完成任务 - 保存信息并更新任务状态
     * 状态逻辑：0/1→3(按时完成), 2→4(延时完成)
     */
    @PostMapping("/completeTask")
    public Map<String, Object> completeTask(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        Long taskId = Long.parseLong(params.get("taskId").toString());
        ProjectTask task = projectTaskService.getById(taskId);
        if (task == null) {
            result.put("code", 404);
            result.put("msg", "任务不存在");
            return result;
        }

        // 更新可编辑字段
        updateTaskEditableFields(task, params);

        // 更新任务状态
        Integer currentStatus = task.getTaskStatus();
        if (currentStatus == 0 || currentStatus == 1) {
            task.setTaskStatus(3); // 按时完成
        } else if (currentStatus == 2) {
            task.setTaskStatus(4); // 延时完成
        } else if (currentStatus == 6) {
            // 申请延期中的任务完成：根据是否延时来判断
            if (task.getTaskEndTime() != null && task.getExecFinishTime() != null
                    && task.getExecFinishTime().isAfter(task.getTaskEndTime())) {
                task.setTaskStatus(4); // 延时完成
            } else {
                task.setTaskStatus(3); // 按时完成
            }
        }

        projectTaskService.updateById(task);

        // 保存附件记录（如有新的）
        saveAttachmentRecords(params, taskId);

        result.put("code", 200);
        result.put("msg", "完成任务成功");
        return result;
    }

    /**
     * 暂存未完成 - 只保存信息，不更新任务状态
     */
    @PostMapping("/tempSave")
    public Map<String, Object> tempSave(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        Long taskId = Long.parseLong(params.get("taskId").toString());
        ProjectTask task = projectTaskService.getById(taskId);
        if (task == null) {
            result.put("code", 404);
            result.put("msg", "任务不存在");
            return result;
        }

        // 更新可编辑字段，但不更新状态
        updateTaskEditableFields(task, params);
        projectTaskService.updateById(task);

        // 保存附件记录（如有新的）
        saveAttachmentRecords(params, taskId);

        result.put("code", 200);
        result.put("msg", "暂存成功");
        return result;
    }

    /**
     * 申请延期 - 保存信息并更新任务状态为6(申请延期中)
     * 仅允许状态为0(未开始)、1(执行中)的任务申请延期
     */
    @PostMapping("/applyDelay")
    public Map<String, Object> applyDelay(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        Long taskId = Long.parseLong(params.get("taskId").toString());
        ProjectTask task = projectTaskService.getById(taskId);
        if (task == null) {
            result.put("code", 404);
            result.put("msg", "任务不存在");
            return result;
        }

        Integer currentStatus = task.getTaskStatus();
        if (currentStatus != 0 && currentStatus != 1) {
            result.put("code", 400);
            result.put("msg", "只有未开始或执行中的任务可以申请延期");
            return result;
        }

        // 更新可编辑字段
        updateTaskEditableFields(task, params);

        // 更新状态为申请延期中
        task.setTaskStatus(6);
        projectTaskService.updateById(task);

        // 保存附件记录（如有新的）
        saveAttachmentRecords(params, taskId);

        result.put("code", 200);
        result.put("msg", "申请延期成功");
        return result;
    }

    /**
     * 保存附件记录 - 前端上传文件后调用此接口将记录写入数据库
     */
    @PostMapping("/saveAttachment")
    public Map<String, Object> saveAttachment(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        Long taskId = Long.parseLong(params.get("taskId").toString());
        String filePath = (String) params.get("filePath");
        String fileName = (String) params.get("fileName");

        ProjectTaskAttachment attachment = new ProjectTaskAttachment();
        attachment.setTaskId(taskId);
        attachment.setAttachType(2); // 执行时附件
        attachment.setFilePath(filePath);
        attachment.setFileName(fileName);
        attachment.setCreateTime(LocalDateTime.now());
        projectTaskAttachmentService.save(attachment);

        result.put("code", 200);
        result.put("msg", "附件保存成功");
        result.put("attachmentId", attachment.getId());
        return result;
    }

    /**
     * 删除附件记录
     */
    @PostMapping("/deleteAttachment/{attachId}")
    public Map<String, Object> deleteAttachment(@PathVariable Long attachId) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        ProjectTaskAttachment attachment = projectTaskAttachmentService.getById(attachId);
        if (attachment == null) {
            result.put("code", 404);
            result.put("msg", "附件不存在");
            return result;
        }

        projectTaskAttachmentService.removeById(attachId);

        result.put("code", 200);
        result.put("msg", "附件删除成功");
        return result;
    }

    // ========== 私有辅助方法 ==========

    /**
     * 更新任务可编辑字段（任务完成时间、工作成果、备注）
     */
    private void updateTaskEditableFields(ProjectTask task, Map<String, Object> params) {
        // 任务完成时间
        String execFinishTimeStr = (String) params.get("execFinishTime");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (execFinishTimeStr != null && !execFinishTimeStr.isEmpty()) {
            if (execFinishTimeStr.length() == 10) {
                execFinishTimeStr += " 00:00:00";
            }
            task.setExecFinishTime(LocalDateTime.parse(execFinishTimeStr, formatter));
        }

        // 工作成果
        task.setWorkContent((String) params.get("workContent"));

        // 备注
        task.setRemark((String) params.get("remark"));
    }

    /**
     * 保存前端传入的新附件记录列表
     */
    private void saveAttachmentRecords(Map<String, Object> params, Long taskId) {
        Object newAttachmentsObj = params.get("newAttachments");
        if (newAttachmentsObj instanceof List) {
            @SuppressWarnings("unchecked")
			List<Map<String, Object>> newAttachments = (List<Map<String, Object>>) newAttachmentsObj;
            for (Map<String, Object> att : newAttachments) {
                String filePath = (String) att.get("filePath");
                String fileName = (String) att.get("fileName");
                if (filePath != null && fileName != null) {
                    ProjectTaskAttachment attachment = new ProjectTaskAttachment();
                    attachment.setTaskId(taskId);
                    attachment.setAttachType(2);
                    attachment.setFilePath(filePath);
                    attachment.setFileName(fileName);
                    attachment.setCreateTime(LocalDateTime.now());
                    projectTaskAttachmentService.save(attachment);
                }
            }
        }
    }
}
