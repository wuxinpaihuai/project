package com.zjhl.project.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
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
import com.zjhl.project.entity.ProjectBidResult;
import com.zjhl.project.entity.ProjectExtend;
import com.zjhl.project.entity.ProjectFile;
import com.zjhl.project.entity.ProjectInfo;
import com.zjhl.project.entity.ProjectStage;
import com.zjhl.project.entity.ProjectTask;
import com.zjhl.project.entity.ProjectTaskAttachment;
import com.zjhl.project.service.ProjectBidResultService;
import com.zjhl.project.service.ProjectExtendService;
import com.zjhl.project.service.ProjectFileService;
import com.zjhl.project.service.ProjectInfoService;
import com.zjhl.project.service.ProjectStageService;
import com.zjhl.project.service.ProjectTaskAttachmentService;
import com.zjhl.project.service.ProjectTaskService;

import cn.dev33.satoken.stp.StpUtil;

@RestController
@RequestMapping("/project/tb")
public class ProjectTbController {

    @Autowired
    private ProjectInfoService projectInfoService;

    @Autowired
    private ProjectStageService projectStageService;

    @Autowired
    private ProjectTaskService projectTaskService;

    @Autowired
    private ProjectFileService projectFileService;

    @Autowired
    private ProjectTaskAttachmentService projectTaskAttachmentService;

    @Autowired
    private ProjectExtendService projectExtendService;

    @Autowired
    private ProjectBidResultService projectBidResultService;

    /**
     * 项目信息tab页 - 查询投标阶段进行中的项目列表（分页）
     */
    @GetMapping("/projectList")
    public Map<String, Object> projectList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String projectName,
            @RequestParam(required = false) String bidTimeStart,
            @RequestParam(required = false) String bidTimeEnd) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        // 1. 查询投标阶段(stage_type=1)且进行中(stage_status=1)的项目ID
        QueryWrapper<ProjectStage> stageWrapper = new QueryWrapper<>();
        stageWrapper.eq("stage_type", 1);
        stageWrapper.eq("stage_status", 1);
        List<ProjectStage> stages = projectStageService.list(stageWrapper);
        List<Long> projectIds = stages.stream().map(ProjectStage::getProjectId).collect(Collectors.toList());

        if (projectIds.isEmpty()) {
            result.put("code", 200);
            result.put("msg", "查询成功");
            result.put("total", 0);
            result.put("records", Arrays.asList());
            return result;
        }

        // 2. 根据项目ID和条件查询项目信息
        QueryWrapper<ProjectInfo> wrapper = new QueryWrapper<>();
        wrapper.in("id", projectIds);
        if (projectName != null && !projectName.isEmpty()) {
            wrapper.like("project_name", projectName);
        }
        if (bidTimeStart != null && !bidTimeStart.isEmpty()) {
            wrapper.ge("bid_time", LocalDateTime.parse(bidTimeStart, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (bidTimeEnd != null && !bidTimeEnd.isEmpty()) {
            wrapper.le("bid_time", LocalDateTime.parse(bidTimeEnd, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        wrapper.orderByDesc("create_time");

        Page<ProjectInfo> page = new Page<>(pageNum, pageSize);
        Page<ProjectInfo> resultPage = projectInfoService.page(page, wrapper);

        // 3. 为每个项目填充任务完成情况
        List<ProjectInfo> records = resultPage.getRecords();
        for (ProjectInfo info : records) {
            info.setStageStatus(1); // 都是进行中
            // 查询任务完成情况
            QueryWrapper<ProjectTask> taskWrapper = new QueryWrapper<>();
            taskWrapper.eq("project_id", info.getId());
            taskWrapper.eq("stage_type", 1);
            List<ProjectTask> tasks = projectTaskService.list(taskWrapper);
            int totalCount = tasks.size();
            long finishedCount = tasks.stream().filter(t -> t.getTaskStatus() != null && (t.getTaskStatus() == 3 || t.getTaskStatus() == 4)).count();
            info.setRemark(totalCount > 0 ? finishedCount + "/" + totalCount : "0/0");
        }

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("total", resultPage.getTotal());
        result.put("records", records);
        return result;
    }

    /**
     * 查看项目详情（含基本信息、附件、任务执行情况、投标结果）
     */
    @GetMapping("/projectDetail/{projectId}")
    public Map<String, Object> projectDetail(@PathVariable Long projectId) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        // 项目基本信息
        ProjectInfo project = projectInfoService.getById(projectId);
        if (project == null) {
            result.put("code", 404);
            result.put("msg", "项目不存在");
            return result;
        }

        // 项目附件（投标阶段）
        QueryWrapper<ProjectFile> fileWrapper = new QueryWrapper<>();
        fileWrapper.eq("project_id", projectId);
        fileWrapper.eq("stage_type", 1);
        fileWrapper.orderByAsc("create_time");
        List<ProjectFile> files = projectFileService.list(fileWrapper);

        // 任务执行情况
        QueryWrapper<ProjectTask> taskWrapper = new QueryWrapper<>();
        taskWrapper.eq("project_id", projectId);
        taskWrapper.eq("stage_type", 1);
        taskWrapper.orderByAsc("create_time");
        List<ProjectTask> tasks = projectTaskService.list(taskWrapper);

        // 为每个任务查询附件
        for (ProjectTask task : tasks) {
            QueryWrapper<ProjectTaskAttachment> attachWrapper = new QueryWrapper<>();
            attachWrapper.eq("task_id", task.getId());
            attachWrapper.eq("attach_type", 2);
            attachWrapper.orderByAsc("create_time");
           // List<ProjectTaskAttachment> attachments = projectTaskAttachmentService.list(attachWrapper);
            task.setTaskDesc(task.getTaskDesc()); // 借用taskDesc暂存附件JSON
        }

        // 投标结果 - 是否中标
        QueryWrapper<ProjectExtend> extendWrapper = new QueryWrapper<>();
        extendWrapper.eq("project_id", projectId);
        ProjectExtend extend = projectExtendService.getOne(extendWrapper);

        // 投标单位得分
        QueryWrapper<ProjectBidResult> bidWrapper = new QueryWrapper<>();
        bidWrapper.eq("project_id", projectId);
        bidWrapper.orderByAsc("final_rank");
        List<ProjectBidResult> bidResults = projectBidResultService.list(bidWrapper);

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("project", project);
        result.put("files", files);
        result.put("tasks", tasks);
        result.put("extend", extend);
        result.put("bidResults", bidResults);
        return result;
    }

    /**
     * 获取项目投标结果信息（用于录入/编辑页面）
     */
    @GetMapping("/bidResult/{projectId}")
    public Map<String, Object> getBidResult(@PathVariable Long projectId) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        // 项目信息
        ProjectInfo project = projectInfoService.getById(projectId);
        if (project == null) {
            result.put("code", 404);
            result.put("msg", "项目不存在");
            return result;
        }

        // 是否中标
        QueryWrapper<ProjectExtend> extendWrapper = new QueryWrapper<>();
        extendWrapper.eq("project_id", projectId);
        ProjectExtend extend = projectExtendService.getOne(extendWrapper);

        // 投标单位得分
        QueryWrapper<ProjectBidResult> bidWrapper = new QueryWrapper<>();
        bidWrapper.eq("project_id", projectId);
        bidWrapper.orderByAsc("final_rank");
        List<ProjectBidResult> bidResults = projectBidResultService.list(bidWrapper);

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("project", project);
        result.put("extend", extend);
        result.put("bidResults", bidResults);
        return result;
    }

    /**
     * 保存投标结果（含是否中标、中标金额、投标单位得分）
     */
    @PostMapping("/saveBidResult")
    public Map<String, Object> saveBidResult(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        Long projectId = Long.parseLong(params.get("projectId").toString());
        Integer isWinBid = params.get("isWinBid") != null ? Integer.parseInt(params.get("isWinBid").toString()) : 0;
        String winBidAmount = (String) params.get("winBidAmount");
        String filePath = (String) params.get("filePath");
        String fileName = (String) params.get("fileName");

        // 保存/更新 project_extend
        QueryWrapper<ProjectExtend> extendWrapper = new QueryWrapper<>();
        extendWrapper.eq("project_id", projectId);
        ProjectExtend extend = projectExtendService.getOne(extendWrapper);
        if (extend == null) {
            extend = new ProjectExtend();
            extend.setProjectId(projectId);
            extend.setIsWinBid(isWinBid);
            extend.setWinBidAmount(winBidAmount);
            if (isWinBid == 1) {
                extend.setFilePath(filePath);
                extend.setFileName(fileName);
            } else {
                extend.setFilePath(null);
                extend.setFileName(null);
            }
            extend.setCreateTime(LocalDateTime.now());
            extend.setUpdateTime(LocalDateTime.now());
            projectExtendService.save(extend);
        } else {
            extend.setIsWinBid(isWinBid);
            extend.setWinBidAmount(winBidAmount);
            if (isWinBid == 1) {
                extend.setFilePath(filePath);
                extend.setFileName(fileName);
            } else {
                extend.setFilePath(null);
                extend.setFileName(null);
            }
            extend.setUpdateTime(LocalDateTime.now());
            projectExtendService.updateById(extend);
        }

        // 删除旧的投标单位得分记录，重新保存
        QueryWrapper<ProjectBidResult> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("project_id", projectId);
        projectBidResultService.remove(deleteWrapper);

        // 保存新的投标单位得分
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bidList = (List<Map<String, Object>>) params.get("bidList");
        if (bidList != null && !bidList.isEmpty()) {
            for (Map<String, Object> item : bidList) {
                ProjectBidResult bidResult = new ProjectBidResult();
                bidResult.setProjectId(projectId);
                bidResult.setBidCompanyName((String) item.get("bidCompanyName"));
                bidResult.setBidPrice((String) item.get("bidPrice"));
                bidResult.setPriceScore(item.get("priceScore") != null && !item.get("priceScore").toString().isEmpty() ?
                        new java.math.BigDecimal(item.get("priceScore").toString()) : null);
                bidResult.setBusinessScore(item.get("businessScore") != null && !item.get("businessScore").toString().isEmpty() ?
                        new java.math.BigDecimal(item.get("businessScore").toString()) : null);
                bidResult.setTechScore(item.get("techScore") != null && !item.get("techScore").toString().isEmpty() ?
                        new java.math.BigDecimal(item.get("techScore").toString()) : null);
                bidResult.setFinalScore(item.get("finalScore") != null && !item.get("finalScore").toString().isEmpty() ?
                        new java.math.BigDecimal(item.get("finalScore").toString()) : null);
                bidResult.setFinalRank(item.get("finalRank") != null && !item.get("finalRank").toString().isEmpty() ?
                        Integer.parseInt(item.get("finalRank").toString()) : null);
                bidResult.setCreateTime(LocalDateTime.now());
                projectBidResultService.save(bidResult);
            }
        }

        result.put("code", 200);
        result.put("msg", "保存成功");
        return result;
    }

    /**
     * 提交签约
     */
    @PostMapping("/signContract")
    public Map<String, Object> signContract(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        Long projectId = Long.parseLong(params.get("projectId").toString());

        // 检查是否中标
        QueryWrapper<ProjectExtend> extendWrapper = new QueryWrapper<>();
        extendWrapper.eq("project_id", projectId);
        ProjectExtend extend = projectExtendService.getOne(extendWrapper);

        if (extend == null || extend.getIsWinBid() == null || extend.getIsWinBid() != 1) {
            result.put("code", 500);
            result.put("msg", "项目未中标，不能提交签约");
            return result;
        }

        // 校验技术负责人
        Long techUserId = params.get("techUserId") != null ? Long.parseLong(params.get("techUserId").toString()) : null;
        String techUserName = (String) params.get("techUserName");
        String techUserPhone = (String) params.get("techUserPhone");
        if (techUserId == null) {
            result.put("code", 500);
            result.put("msg", "请选择技术负责人");
            return result;
        }

        // 更新签约状态
       // extend.setIsSign(1);
        extend.setUpdateTime(LocalDateTime.now());
        projectExtendService.updateById(extend);

        // 更新项目信息表的技术负责人
        ProjectInfo project = projectInfoService.getById(projectId);
        if (project != null) {
            project.setTechUserId(techUserId);
            project.setTechUserName(techUserName);
            project.setTechUserPhone(techUserPhone);
            project.setUpdateTime(LocalDateTime.now());
            projectInfoService.updateById(project);
        }

        //更新投标阶段结束时间，插入签约阶段和实施阶段记录。（目前机制是默认签约阶段和实施阶段同时展开）
        QueryWrapper<ProjectStage> stageWrapper =  new QueryWrapper<>();
        stageWrapper.eq("project_id", projectId);
        stageWrapper.eq("stage_type", 1);//投标状态
        ProjectStage updateStage = projectStageService.getOne(stageWrapper);
        
        if (updateStage == null ) {
            result.put("code", 500);
            result.put("msg", "项目进入签约阶段出错，请联系管理员！");
            return result;
        }
        
        updateStage.setEndTime(LocalDateTime.now());//更新投标阶段结束时间
        updateStage.setStageStatus(2);//已完成
        updateStage.setStageRemark("投标已完成，进入下一阶段");
        projectStageService.updateById(updateStage);
        
        //签约阶段
        ProjectStage qyStage = new ProjectStage();
        qyStage.setCreateTime(LocalDateTime.now());
        qyStage.setProjectId(projectId);
        qyStage.setStageType(2);//签约
        qyStage.setStageStatus(1);//进行中
        qyStage.setStartTime(LocalDateTime.now());//开始时间
        projectStageService.save(qyStage);
        
        
        //实施阶段
        ProjectStage ssStage = new ProjectStage();
        ssStage.setCreateTime(LocalDateTime.now());
        ssStage.setProjectId(projectId);
        ssStage.setStageType(3);//实施
        ssStage.setStageStatus(1);//进行中
        ssStage.setStartTime(LocalDateTime.now());//开始时间
        projectStageService.save(ssStage);
        
        result.put("code", 200);
        result.put("msg", "提交签约成功");
        return result;
    }

    /**
     * 我的任务tab页 - 查询当前用户的任务列表
     */
    @GetMapping("/myTaskList")
    public Map<String, Object> myTaskList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        Long userId = StpUtil.getLoginIdAsLong();

        QueryWrapper<ProjectTask> wrapper = new QueryWrapper<>();
        wrapper.eq("exec_user_id", userId);
        wrapper.eq("stage_type", 1);
        wrapper.orderByAsc("task_status");
        wrapper.orderByAsc("task_end_time");

        Page<ProjectTask> page = new Page<>(pageNum, pageSize);
        Page<ProjectTask> resultPage = projectTaskService.page(page, wrapper);

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("total", resultPage.getTotal());
        result.put("records", resultPage.getRecords());
        return result;
    }

    /**
     * 获取任务详情（含附件，用于完成任务页面）
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

        // 查询任务附件（attach_type=2 任务完成附件）
        QueryWrapper<ProjectTaskAttachment> attachWrapper = new QueryWrapper<>();
        attachWrapper.eq("task_id", taskId);
        attachWrapper.orderByAsc("create_time");
        List<ProjectTaskAttachment> attachments = projectTaskAttachmentService.list(attachWrapper);

        // 查询项目信息
        ProjectInfo project = projectInfoService.getById(task.getProjectId());

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("task", task);
        result.put("attachments", attachments);
        result.put("project", project);
        return result;
    }

    /**
     * 完成任务（提交任务）
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
        String taskDesc = (String) params.get("taskDesc");
        Integer taskStatus = params.get("taskStatus") != null ? Integer.parseInt(params.get("taskStatus").toString()) : 3;

        ProjectTask task = projectTaskService.getById(taskId);
        if (task == null) {
            result.put("code", 404);
            result.put("msg", "任务不存在");
            return result;
        }

        // 更新任务状态
        task.setTaskDesc(taskDesc);
        task.setTaskStatus(taskStatus);
        task.setExecFinishTime(LocalDateTime.now());
        projectTaskService.updateById(task);

        // 保存附件
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fileList = (List<Map<String, Object>>) params.get("fileList");
        if (fileList != null && !fileList.isEmpty()) {
            for (Map<String, Object> fileMap : fileList) {
                ProjectTaskAttachment attachment = new ProjectTaskAttachment();
                attachment.setTaskId(taskId);
                attachment.setAttachType(2); // 任务完成附件
                attachment.setFilePath((String) fileMap.get("filePath"));
                attachment.setFileName((String) fileMap.get("fileName"));
                attachment.setCreateTime(LocalDateTime.now());
                projectTaskAttachmentService.save(attachment);
            }
        }

        result.put("code", 200);
        result.put("msg", "提交成功");
        return result;
    }

    /**
     * 获取任务附件列表
     */
    @GetMapping("/taskAttachments/{taskId}")
    public Map<String, Object> taskAttachments(@PathVariable Long taskId) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        QueryWrapper<ProjectTaskAttachment> attachWrapper = new QueryWrapper<>();
        attachWrapper.eq("task_id", taskId);
        attachWrapper.orderByAsc("create_time");
        List<ProjectTaskAttachment> attachments = projectTaskAttachmentService.list(attachWrapper);

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", attachments);
        return result;
    }
}
