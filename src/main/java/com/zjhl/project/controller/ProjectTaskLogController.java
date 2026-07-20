package com.zjhl.project.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zjhl.project.entity.ProjectInfo;
import com.zjhl.project.entity.ProjectTask;
import com.zjhl.project.entity.ProjectTaskLog;
import com.zjhl.project.entity.ProjectTaskLogAttachment;
import com.zjhl.project.service.ProjectInfoService;
import com.zjhl.project.service.ProjectTaskLogAttachmentService;
import com.zjhl.project.service.ProjectTaskLogService;
import com.zjhl.project.service.ProjectTaskService;

import cn.dev33.satoken.stp.StpUtil;

/**
 * 工作日志 Controller
 */
@RestController
@RequestMapping("/project/log")
public class ProjectTaskLogController {

    @Autowired
    private ProjectTaskLogService projectTaskLogService;

    @Autowired
    private ProjectTaskLogAttachmentService projectTaskLogAttachmentService;

    @Autowired
    private ProjectTaskService projectTaskService;

    @Autowired
    private ProjectInfoService projectInfoService;

    /**
     * 获取项目+任务基本信息（工作日志页面头部展示）
     * GET /project/log/taskInfo?taskId=xxx
     */
    @GetMapping("/taskInfo")
    public Map<String, Object> taskInfo(@RequestParam("taskId") Long taskId) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "");

        Map<String, Object> data = new HashMap<>();

        // 获取任务信息
        ProjectTask task = projectTaskService.getById(taskId);
        data.put("task", task != null ? task : new HashMap<>());

        // 获取项目信息
        if (task != null && task.getProjectId() != null) {
            ProjectInfo project = projectInfoService.getById(task.getProjectId());
            data.put("project", project != null ? project : new HashMap<>());
        } else {
            data.put("project", new HashMap<>());
        }

        result.put("data", data);
        return result;
    }

    /**
     * 获取工作日志列表（含附件）
     * GET /project/log/list?taskId=xxx
     */
    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam("taskId") Long taskId) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "");

        // 查询日志列表，按创建时间倒序
        LambdaQueryWrapper<ProjectTaskLog> logWrapper = new LambdaQueryWrapper<>();
        logWrapper.eq(ProjectTaskLog::getTaskId, taskId)
                .orderByDesc(ProjectTaskLog::getLogDate)
                .orderByDesc(ProjectTaskLog::getCreateTime);
        List<ProjectTaskLog> logs = projectTaskLogService.list(logWrapper);

        // 查询所有相关附件
        if (logs != null && !logs.isEmpty()) {
            List<Long> logIds = logs.stream().map(ProjectTaskLog::getId).collect(Collectors.toList());
            LambdaQueryWrapper<ProjectTaskLogAttachment> attWrapper = new LambdaQueryWrapper<>();
            attWrapper.in(ProjectTaskLogAttachment::getLogId, logIds)
                    .orderByAsc(ProjectTaskLogAttachment::getCreateTime);
            List<ProjectTaskLogAttachment> allAttachments = projectTaskLogAttachmentService.list(attWrapper);

            // 按logId分组附件
            Map<Long, List<ProjectTaskLogAttachment>> attMap = new HashMap<>();
            if (allAttachments != null) {
                for (ProjectTaskLogAttachment att : allAttachments) {
                    attMap.computeIfAbsent(att.getLogId(), k -> new ArrayList<>()).add(att);
                }
            }

            // 组装结果
            List<Map<String, Object>> logList = new ArrayList<>();
            for (ProjectTaskLog log : logs) {
                Map<String, Object> logMap = new HashMap<>();
                logMap.put("id", log.getId());
                logMap.put("taskId", log.getTaskId());
                logMap.put("projectId", log.getProjectId());
                logMap.put("logDate", log.getLogDate());
                logMap.put("workContent", log.getWorkContent());
                logMap.put("recorderId", log.getRecorderId());
                logMap.put("recorderName", log.getRecorderName());
                logMap.put("remark", log.getRemark());
                logMap.put("createTime", log.getCreateTime());
                logMap.put("updateTime", log.getUpdateTime());

                List<ProjectTaskLogAttachment> atts = attMap.getOrDefault(log.getId(), new ArrayList<>());
                List<Map<String, Object>> attList = new ArrayList<>();
                for (ProjectTaskLogAttachment att : atts) {
                    Map<String, Object> attMap2 = new HashMap<>();
                    attMap2.put("id", att.getId());
                    attMap2.put("logId", att.getLogId());
                    attMap2.put("filePath", att.getFilePath());
                    attMap2.put("fileName", att.getFileName());
                    attMap2.put("createTime", att.getCreateTime());
                    attList.add(attMap2);
                }
                logMap.put("attachments", attList);

                logList.add(logMap);
            }
            result.put("data", logList);
        } else {
            result.put("data", new ArrayList<>());
        }

        return result;
    }

    /**
     * 获取单条日志详情（含附件）
     * GET /project/log/detail?logId=xxx
     */
    @GetMapping("/detail")
    public Map<String, Object> detail(@RequestParam("logId") Long logId) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "");

        ProjectTaskLog log = projectTaskLogService.getById(logId);
        if (log == null) {
            result.put("code", 500);
            result.put("msg", "日志不存在");
            result.put("data", new HashMap<>());
            return result;
        }

        Map<String, Object> logMap = new HashMap<>();
        logMap.put("id", log.getId());
        logMap.put("taskId", log.getTaskId());
        logMap.put("projectId", log.getProjectId());
        logMap.put("logDate", log.getLogDate());
        logMap.put("workContent", log.getWorkContent());
        logMap.put("recorderId", log.getRecorderId());
        logMap.put("recorderName", log.getRecorderName());
        logMap.put("remark", log.getRemark());
        logMap.put("createTime", log.getCreateTime());
        logMap.put("updateTime", log.getUpdateTime());

        // 查询附件
        LambdaQueryWrapper<ProjectTaskLogAttachment> attWrapper = new LambdaQueryWrapper<>();
        attWrapper.eq(ProjectTaskLogAttachment::getLogId, logId)
                .orderByAsc(ProjectTaskLogAttachment::getCreateTime);
        List<ProjectTaskLogAttachment> attachments = projectTaskLogAttachmentService.list(attWrapper);

        List<Map<String, Object>> attList = new ArrayList<>();
        if (attachments != null) {
            for (ProjectTaskLogAttachment att : attachments) {
                Map<String, Object> attMap = new HashMap<>();
                attMap.put("id", att.getId());
                attMap.put("logId", att.getLogId());
                attMap.put("filePath", att.getFilePath());
                attMap.put("fileName", att.getFileName());
                attMap.put("createTime", att.getCreateTime());
                attList.add(attMap);
            }
        }
        logMap.put("attachments", attList);

        result.put("data", logMap);
        return result;
    }

    /**
     * 新增工作日志
     * POST /project/log/add
     * 参数: taskId, projectId, logDate, workContent, remark
     */
    @PostMapping("/add")
    public Map<String, Object> add(@RequestParam("taskId") Long taskId,
                                    @RequestParam(value = "projectId", required = false) Long projectId,
                                    @RequestParam("logDate") String logDate,
                                    @RequestParam("workContent") String workContent,
                                    @RequestParam(value = "remark", required = false) String remark) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "");

        ProjectTaskLog log = new ProjectTaskLog();
        log.setTaskId(taskId);
        log.setProjectId(projectId);
        log.setLogDate(LocalDate.parse(logDate, DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        log.setWorkContent(workContent);
        log.setRemark(remark);

        // 记录人信息（当前登录用户）
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            String userName = (String) StpUtil.getSession().get("userName");
            log.setRecorderId(userId);
            log.setRecorderName(userName != null ? userName : "");
        } catch (Exception e) {
            // 未登录或获取失败，忽略
            log.setRecorderId(null);
            log.setRecorderName("");
        }

        projectTaskLogService.save(log);

        Map<String, Object> data = new HashMap<>();
        data.put("id", log.getId());
        result.put("data", data);

        return result;
    }

    /**
     * 编辑工作日志
     * POST /project/log/edit
     * 参数: id, logDate, workContent, remark
     */
    @PostMapping("/edit")
    public Map<String, Object> edit(@RequestParam("id") Long id,
                                     @RequestParam("logDate") String logDate,
                                     @RequestParam("workContent") String workContent,
                                     @RequestParam(value = "remark", required = false) String remark) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "");

        ProjectTaskLog log = projectTaskLogService.getById(id);
        if (log == null) {
            result.put("code", 500);
            result.put("msg", "日志不存在");
            return result;
        }

        log.setLogDate(LocalDate.parse(logDate, DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        log.setWorkContent(workContent);
        log.setRemark(remark);
        projectTaskLogService.updateById(log);

        Map<String, Object> data = new HashMap<>();
        data.put("id", log.getId());
        result.put("data", data);

        return result;
    }

    /**
     * 删除工作日志（同时删除关联附件）
     * POST /project/log/delete
     * 参数: id
     */
    @PostMapping("/delete")
    public Map<String, Object> delete(@RequestParam("id") Long id) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "");

        // 删除附件
        LambdaQueryWrapper<ProjectTaskLogAttachment> attWrapper = new LambdaQueryWrapper<>();
        attWrapper.eq(ProjectTaskLogAttachment::getLogId, id);
        projectTaskLogAttachmentService.remove(attWrapper);

        // 删除日志
        projectTaskLogService.removeById(id);

        return result;
    }

    /**
     * 保存日志附件
     * POST /project/log/saveAttachment
     * 参数: logId, filePath, fileName
     */
    @PostMapping("/saveAttachment")
    public Map<String, Object> saveAttachment(@RequestParam("logId") Long logId,
                                               @RequestParam("filePath") String filePath,
                                               @RequestParam("fileName") String fileName) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "");

        ProjectTaskLogAttachment att = new ProjectTaskLogAttachment();
        att.setLogId(logId);
        att.setFilePath(filePath);
        att.setFileName(fileName);
        projectTaskLogAttachmentService.save(att);

        Map<String, Object> data = new HashMap<>();
        data.put("id", att.getId());
        result.put("data", data);

        return result;
    }

    /**
     * 删除日志附件
     * POST /project/log/deleteAttachment
     * 参数: id
     */
    @PostMapping("/deleteAttachment")
    public Map<String, Object> deleteAttachment(@RequestParam("id") Long id) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "");

        projectTaskLogAttachmentService.removeById(id);

        return result;
    }
}
