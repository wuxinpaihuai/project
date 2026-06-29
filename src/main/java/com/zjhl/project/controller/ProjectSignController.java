package com.zjhl.project.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjhl.project.entity.*;
import com.zjhl.project.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/project/sign")
public class ProjectSignController {

    @Autowired
    private ProjectInfoService projectInfoService;

    @Autowired
    private ProjectExtendService projectExtendService;

    @Autowired
    private SignVisitService signVisitService;

    @Autowired
    private SignVisitFileService signVisitFileService;

    @Autowired
    private ProjectFileService projectFileService;

    @Autowired
    private SignMilestoneService signMilestoneService;

    @Autowired
    private SignPaymentService signPaymentService;
    
    
    @Autowired
    private ProjectBidResultService projectBidResultService;

    @Autowired
    private ProjectTaskService projectTaskService;

    @Autowired
    private ProjectTaskAttachmentService projectTaskAttachmentService;
    /**
     * 合同签订列表 - 查询已中标的项目（分页）
     */
    @GetMapping("/signList")
    public Map<String, Object> signList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String projectName,
            @RequestParam(required = false) Integer isSign) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        // 1. 查询已中标的项目ID
        QueryWrapper<ProjectExtend> extendWrapper = new QueryWrapper<>();
        extendWrapper.eq("is_win_bid", 1);
        if (isSign != null) {
            extendWrapper.eq("is_sign", isSign);
        }
        List<ProjectExtend> extend = projectExtendService.list(extendWrapper);
        List<Long> projectIds = extend.stream().map(ProjectExtend::getProjectId).collect(Collectors.toList());

        if (projectIds.isEmpty()) {
            result.put("code", 200);
            result.put("msg", "查询成功");
            result.put("total", 0);
            result.put("records", Collections.EMPTY_LIST);
            return result;
        }

        // 2. 查询项目信息
        QueryWrapper<ProjectInfo> wrapper = new QueryWrapper<>();
        wrapper.in("id", projectIds);
        if (projectName != null && !projectName.isEmpty()) {
            wrapper.like("project_name", projectName);
        }
        wrapper.orderByDesc("create_time");

        Page<ProjectInfo> page = new Page<>(pageNum, pageSize);
        Page<ProjectInfo> resultPage = projectInfoService.page(page, wrapper);

        // 3. 为每条记录拼接extend信息
        List<Map<String, Object>> records = new ArrayList<>();
        for (ProjectInfo info : resultPage.getRecords()) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", info.getId());
            row.put("projectName", info.getProjectName());
            row.put("projectNo", info.getProjectNo());
            row.put("area", info.getArea());
            row.put("projectAmount", info.getProjectAmount());
            row.put("techUserName", info.getTechUserName());
            row.put("techUserPhone", info.getTechUserPhone());

            // 查找对应的extend
            ProjectExtend ext = extend.stream()
                    .filter(e -> e.getProjectId().equals(info.getId()))
                    .findFirst().orElse(null);
            if (ext != null) {
                row.put("winBidAmount", ext.getWinBidAmount());
                row.put("fileName", ext.getFileName());
                row.put("filePath", ext.getFilePath());
                row.put("isSign", ext.getIsSign());
                row.put("extendId", ext.getId());
                row.put("signEndTime", ext.getSignEndTime());
            } else {
                row.put("winBidAmount", null);
                row.put("fileName", null);
                row.put("filePath", null);
                row.put("isSign", 0);
                row.put("extendId", null);
            }
            records.add(row);
        }

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("total", resultPage.getTotal());
        result.put("records", records);
        return result;
    }

    /**
     * 拜访记录列表
     */
    @GetMapping("/visitList/{projectId}")
    public Map<String, Object> visitList(@PathVariable Long projectId) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        // 项目基本信息
        ProjectInfo project = projectInfoService.getById(projectId);
        result.put("project", project);

        // 拜访记录（按开始时间倒序）
        QueryWrapper<SignVisit> visitWrapper = new QueryWrapper<>();
        visitWrapper.eq("project_id", projectId);
        visitWrapper.orderByDesc("visit_start_time");
        List<SignVisit> visits = signVisitService.list(visitWrapper);

        // 为每条拜访记录查询附件
        List<Map<String, Object>> visitList = new ArrayList<>();
        for (SignVisit v : visits) {
            Map<String, Object> visitMap = new HashMap<>();
            visitMap.put("id", v.getId());
            visitMap.put("projectId", v.getProjectId());
            visitMap.put("visitTarget", v.getVisitTarget());
            visitMap.put("communicateContent", v.getCommunicateContent());
            visitMap.put("visitStartTime", v.getVisitStartTime() != null ? v.getVisitStartTime().toString() : null);
            visitMap.put("visitEndTime", v.getVisitEndTime() != null ? v.getVisitEndTime().toString() : null);
            visitMap.put("createTime", v.getCreateTime() != null ? v.getCreateTime().toString() : null);

            // 查询附件
            QueryWrapper<SignVisitFile> fileWrapper = new QueryWrapper<>();
            fileWrapper.eq("visit_id", v.getId());
            List<SignVisitFile> files = signVisitFileService.list(fileWrapper);
            visitMap.put("files", files);

            visitList.add(visitMap);
        }

        result.put("code", 200);
        result.put("visits", visitList);
        return result;
    }

    /**
     * 新增拜访记录
     */
    @PostMapping("/addVisit")
    public Map<String, Object> addVisit(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        Long projectId = Long.parseLong(params.get("projectId").toString());
        SignVisit visit = new SignVisit();
        visit.setProjectId(projectId);
        visit.setVisitTarget((String) params.get("visitTarget"));
        visit.setCommunicateContent((String) params.get("communicateContent"));

        String visitStartTime = (String) params.get("visitStartTime");
        String visitEndTime = (String) params.get("visitEndTime");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (visitStartTime != null && !visitStartTime.isEmpty()) {
            visit.setVisitStartTime(LocalDateTime.parse(visitStartTime, formatter));
        }
        if (visitEndTime != null && !visitEndTime.isEmpty()) {
            visit.setVisitEndTime(LocalDateTime.parse(visitEndTime, formatter));
        }

        visit.setCreateTime(LocalDateTime.now());
        signVisitService.save(visit);

        // 保存附件
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fileList = (List<Map<String, Object>>) params.get("files");
        if (fileList != null && !fileList.isEmpty()) {
            for (Map<String, Object> f : fileList) {
                SignVisitFile vf = new SignVisitFile();
                vf.setVisitId(visit.getId());
                vf.setFilePath((String) f.get("filePath"));
                vf.setFileName((String) f.get("fileName"));
                vf.setCreateTime(LocalDateTime.now());
                signVisitFileService.save(vf);
            }
        }

        result.put("code", 200);
        result.put("msg", "新增成功");
        result.put("visitId", visit.getId());
        return result;
    }

    /**
     * 编辑拜访记录
     */
    @PostMapping("/editVisit")
    public Map<String, Object> editVisit(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        Long visitId = Long.parseLong(params.get("id").toString());
        SignVisit visit = signVisitService.getById(visitId);
        if (visit == null) {
            result.put("code", 404);
            result.put("msg", "拜访记录不存在");
            return result;
        }

        visit.setVisitTarget((String) params.get("visitTarget"));
        visit.setCommunicateContent((String) params.get("communicateContent"));

        String visitStartTime = (String) params.get("visitStartTime");
        String visitEndTime = (String) params.get("visitEndTime");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (visitStartTime != null && !visitStartTime.isEmpty()) {
            visit.setVisitStartTime(LocalDateTime.parse(visitStartTime, formatter));
        } else {
            visit.setVisitStartTime(null);
        }
        if (visitEndTime != null && !visitEndTime.isEmpty()) {
            visit.setVisitEndTime(LocalDateTime.parse(visitEndTime, formatter));
        } else {
            visit.setVisitEndTime(null);
        }

        signVisitService.updateById(visit);

        // 删除旧附件，重新保存
        QueryWrapper<SignVisitFile> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("visit_id", visitId);
        signVisitFileService.remove(deleteWrapper);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fileList = (List<Map<String, Object>>) params.get("files");
        if (fileList != null && !fileList.isEmpty()) {
            for (Map<String, Object> f : fileList) {
                SignVisitFile vf = new SignVisitFile();
                vf.setVisitId(visitId);
                vf.setFilePath((String) f.get("filePath"));
                vf.setFileName((String) f.get("fileName"));
                vf.setCreateTime(LocalDateTime.now());
                signVisitFileService.save(vf);
            }
        }

        result.put("code", 200);
        result.put("msg", "编辑成功");
        return result;
    }

    /**
     * 删除拜访记录
     */
    @PostMapping("/deleteVisit/{visitId}")
    public Map<String, Object> deleteVisit(@PathVariable Long visitId) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        // 删除附件
        QueryWrapper<SignVisitFile> fileWrapper = new QueryWrapper<>();
        fileWrapper.eq("visit_id", visitId);
        signVisitFileService.remove(fileWrapper);

        // 删除拜访记录
        signVisitService.removeById(visitId);

        result.put("code", 200);
        result.put("msg", "删除成功");
        return result;
    }

    /**
     * 上传合同（标记签约）
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

        QueryWrapper<ProjectExtend> extendWrapper = new QueryWrapper<>();
        extendWrapper.eq("project_id", projectId);
        ProjectExtend extend = projectExtendService.getOne(extendWrapper);
        if (extend == null) {
            result.put("code", 404);
            result.put("msg", "项目扩展信息不存在");
            return result;
        }

        extend.setIsSign(1);
        extend.setUpdateTime(LocalDateTime.now());
        projectExtendService.updateById(extend);

        result.put("code", 200);
        result.put("msg", "签约成功");
        return result;
    }

    /**
     * 保存签约信息（签约日期 + 合同附件 + 标记签约）
     */
    @PostMapping("/saveSignInfo")
    public Map<String, Object> saveSignInfo(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        Long projectId = Long.parseLong(params.get("projectId").toString());

        // 1. 更新 project_extend：签约日期 + 标记签约
        QueryWrapper<ProjectExtend> extendWrapper = new QueryWrapper<>();
        extendWrapper.eq("project_id", projectId);
        ProjectExtend extend = projectExtendService.getOne(extendWrapper);
        if (extend == null) {
            result.put("code", 404);
            result.put("msg", "项目扩展信息不存在");
            return result;
        }

        String signTimeStr = (String) params.get("signTime");
        if (signTimeStr != null && !signTimeStr.isEmpty()) {
            extend.setSignTime(LocalDate.parse(signTimeStr));
        }
        extend.setIsSign(1);
        extend.setUpdateTime(LocalDateTime.now());
        projectExtendService.updateById(extend);

        // 2. 保存合同附件到 project_file（stage_type=2，先删旧后存新）
        QueryWrapper<ProjectFile> fileDeleteWrapper = new QueryWrapper<>();
        fileDeleteWrapper.eq("project_id", projectId).eq("stage_type", 2);
        projectFileService.remove(fileDeleteWrapper);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fileList = (List<Map<String, Object>>) params.get("contractFiles");
        if (fileList != null && !fileList.isEmpty()) {
            for (Map<String, Object> f : fileList) {
                ProjectFile pf = new ProjectFile();
                pf.setProjectId(projectId);
                pf.setStageType(2);
                pf.setFilePath((String) f.get("filePath"));
                pf.setOriginalName((String) f.get("fileName"));
                pf.setCreateTime(LocalDateTime.now());
                projectFileService.save(pf);
            }
        }

        result.put("code", 200);
        result.put("msg", "保存成功");
        return result;
    }

    /**
     * 查询合同页面数据（项目信息 + 签约日期 + 合同附件 + 里程碑 + 收款节点）
     */
    @GetMapping("/contractInfo/{projectId}")
    public Map<String, Object> contractInfo(@PathVariable Long projectId) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        // 项目基本信息
        ProjectInfo project = projectInfoService.getById(projectId);
        result.put("project", project);

        // project_extend 信息
        QueryWrapper<ProjectExtend> extendWrapper = new QueryWrapper<>();
        extendWrapper.eq("project_id", projectId);
        ProjectExtend extend = projectExtendService.getOne(extendWrapper);
        result.put("extend", extend);

        // 合同附件（stage_type=2）
        QueryWrapper<ProjectFile> fileWrapper = new QueryWrapper<>();
        fileWrapper.eq("project_id", projectId).eq("stage_type", 2);
        List<ProjectFile> contractFiles = projectFileService.list(fileWrapper);
        result.put("contractFiles", contractFiles);

        // 里程碑节点
        QueryWrapper<SignMilestone> milestoneWrapper = new QueryWrapper<>();
        milestoneWrapper.eq("project_id", projectId).orderByAsc("id");
        List<SignMilestone> milestones = signMilestoneService.list(milestoneWrapper);
        result.put("milestones", milestones);

        // 收款节点
        QueryWrapper<SignPayment> paymentWrapper = new QueryWrapper<>();
        paymentWrapper.eq("project_id", projectId).orderByAsc("id");
        List<SignPayment> payments = signPaymentService.list(paymentWrapper);
        result.put("payments", payments);

        result.put("code", 200);
        result.put("msg", "查询成功");
        return result;
    }

    /**
     * 删除合同附件
     */
    @PostMapping("/deleteContractFile/{fileId}")
    public Map<String, Object> deleteContractFile(@PathVariable Long fileId) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        projectFileService.removeById(fileId);

        result.put("code", 200);
        result.put("msg", "删除成功");
        return result;
    }

    /**
     * 新增里程碑节点
     */
    @PostMapping("/addMilestone")
    public Map<String, Object> addMilestone(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        Long projectId = Long.parseLong(params.get("projectId").toString());
        SignMilestone milestone = new SignMilestone();
        milestone.setProjectId(projectId);
        milestone.setMilestoneName((String) params.get("milestoneName"));

        String expectFinishDateStr = (String) params.get("expectFinishDate");
        if (expectFinishDateStr != null && !expectFinishDateStr.isEmpty()) {
            milestone.setExpectFinishDate(LocalDate.parse(expectFinishDateStr));
        }
        milestone.setMilestoneDesc((String) params.get("milestoneDesc"));
        milestone.setCreateTime(LocalDateTime.now());
        signMilestoneService.save(milestone);

        result.put("code", 200);
        result.put("msg", "新增成功");
        result.put("milestoneId", milestone.getId());
        return result;
    }

    /**
     * 编辑里程碑节点
     */
    @PostMapping("/editMilestone")
    public Map<String, Object> editMilestone(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        Long milestoneId = Long.parseLong(params.get("id").toString());
        SignMilestone milestone = signMilestoneService.getById(milestoneId);
        if (milestone == null) {
            result.put("code", 404);
            result.put("msg", "里程碑节点不存在");
            return result;
        }

        milestone.setMilestoneName((String) params.get("milestoneName"));
        String expectFinishDateStr = (String) params.get("expectFinishDate");
        if (expectFinishDateStr != null && !expectFinishDateStr.isEmpty()) {
            milestone.setExpectFinishDate(LocalDate.parse(expectFinishDateStr));
        } else {
            milestone.setExpectFinishDate(null);
        }
        milestone.setMilestoneDesc((String) params.get("milestoneDesc"));
        signMilestoneService.updateById(milestone);

        result.put("code", 200);
        result.put("msg", "编辑成功");
        return result;
    }

    /**
     * 删除里程碑节点
     */
    @PostMapping("/deleteMilestone/{milestoneId}")
    public Map<String, Object> deleteMilestone(@PathVariable Long milestoneId) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        signMilestoneService.removeById(milestoneId);

        result.put("code", 200);
        result.put("msg", "删除成功");
        return result;
    }

    /**
     * 新增收款节点
     */
    @PostMapping("/addPayment")
    public Map<String, Object> addPayment(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        Long projectId = Long.parseLong(params.get("projectId").toString());
        SignPayment payment = new SignPayment();
        payment.setProjectId(projectId);
        payment.setPaymentNode((String) params.get("paymentNode"));

        String receiveAmountStr = (String) params.get("receiveAmount");
        if (receiveAmountStr != null && !receiveAmountStr.isEmpty()) {
            payment.setReceiveAmount(new BigDecimal(receiveAmountStr));
        }
        String paymentRateStr = (String) params.get("paymentRate");
        if (paymentRateStr != null && !paymentRateStr.isEmpty()) {
            payment.setPaymentRate(new BigDecimal(paymentRateStr));
        }
        String expectPayDateStr = (String) params.get("expectPayDate");
        if (expectPayDateStr != null && !expectPayDateStr.isEmpty()) {
            payment.setExpectPayDate(LocalDate.parse(expectPayDateStr));
        }
        payment.setCreateTime(LocalDateTime.now());
        signPaymentService.save(payment);

        result.put("code", 200);
        result.put("msg", "新增成功");
        result.put("paymentId", payment.getId());
        return result;
    }

    /**
     * 编辑收款节点
     */
    @PostMapping("/editPayment")
    public Map<String, Object> editPayment(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        Long paymentId = Long.parseLong(params.get("id").toString());
        SignPayment payment = signPaymentService.getById(paymentId);
        if (payment == null) {
            result.put("code", 404);
            result.put("msg", "收款节点不存在");
            return result;
        }

        payment.setPaymentNode((String) params.get("paymentNode"));
        String receiveAmountStr = (String) params.get("receiveAmount");
        if (receiveAmountStr != null && !receiveAmountStr.isEmpty()) {
            payment.setReceiveAmount(new BigDecimal(receiveAmountStr));
        } else {
            payment.setReceiveAmount(null);
        }
        String paymentRateStr = (String) params.get("paymentRate");
        if (paymentRateStr != null && !paymentRateStr.isEmpty()) {
            payment.setPaymentRate(new BigDecimal(paymentRateStr));
        } else {
            payment.setPaymentRate(null);
        }
        String expectPayDateStr = (String) params.get("expectPayDate");
        if (expectPayDateStr != null && !expectPayDateStr.isEmpty()) {
            payment.setExpectPayDate(LocalDate.parse(expectPayDateStr));
        } else {
            payment.setExpectPayDate(null);
        }
        signPaymentService.updateById(payment);

        result.put("code", 200);
        result.put("msg", "编辑成功");
        return result;
    }

    /**
     * 删除收款节点
     */
    @PostMapping("/deletePayment/{paymentId}")
    public Map<String, Object> deletePayment(@PathVariable Long paymentId) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        signPaymentService.removeById(paymentId);

        result.put("code", 200);
        result.put("msg", "删除成功");
        return result;
    }
    
    /**
     * 签约详情页面数据（项目基本信息 + 任务执行 + 投标结果 + 签约详情）
     */
    @GetMapping("/signDetail/{projectId}")
    public Map<String, Object> signDetail(@PathVariable Long projectId) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        // 1. 项目基本信息
        ProjectInfo project = projectInfoService.getById(projectId);
        if (project == null) {
            result.put("code", 404);
            result.put("msg", "项目不存在");
            return result;
        }
        result.put("project", project);

        // 2. 项目附件（投标阶段 stage_type=1）
        QueryWrapper<ProjectFile> fileWrapper = new QueryWrapper<>();
        fileWrapper.eq("project_id", projectId);
        fileWrapper.eq("stage_type", 1);
        fileWrapper.orderByAsc("create_time");
        List<ProjectFile> files = projectFileService.list(fileWrapper);
        result.put("files", files);

        // 3. 任务执行情况（stage_type=1）
        QueryWrapper<ProjectTask> taskWrapper = new QueryWrapper<>();
        taskWrapper.eq("project_id", projectId);
        taskWrapper.eq("stage_type", 1);
        taskWrapper.orderByAsc("create_time");
        List<ProjectTask> tasks = projectTaskService.list(taskWrapper);
        result.put("tasks", tasks);

        // 4. 每个任务的交付物附件（attach_type=2）
        List<Map<String, Object>> taskAttachList = new ArrayList<>();
        for (ProjectTask task : tasks) {
            QueryWrapper<ProjectTaskAttachment> attachWrapper = new QueryWrapper<>();
            attachWrapper.eq("task_id", task.getId());
            attachWrapper.eq("attach_type", 2);
            attachWrapper.orderByAsc("create_time");
            List<ProjectTaskAttachment> attachments = projectTaskAttachmentService.list(attachWrapper);
            Map<String, Object> taskMap = new HashMap<>();
            taskMap.put("taskId", task.getId());
            taskMap.put("attachments", attachments);
            taskAttachList.add(taskMap);
        }
        result.put("taskAttachments", taskAttachList);

        // 5. project_extend（是否中标、中标金额、签约截止时间等）
        QueryWrapper<ProjectExtend> extendWrapper = new QueryWrapper<>();
        extendWrapper.eq("project_id", projectId);
        ProjectExtend extend = projectExtendService.getOne(extendWrapper);
        result.put("extend", extend);

        // 6. 投标单位得分
        QueryWrapper<ProjectBidResult> bidWrapper = new QueryWrapper<>();
        bidWrapper.eq("project_id", projectId);
        bidWrapper.orderByAsc("final_rank");
        List<ProjectBidResult> bidResults = projectBidResultService.list(bidWrapper);
        result.put("bidResults", bidResults);

        // 7. 合同附件（stage_type=2）
        QueryWrapper<ProjectFile> contractWrapper = new QueryWrapper<>();
        contractWrapper.eq("project_id", projectId);
        contractWrapper.eq("stage_type", 2);
        contractWrapper.orderByAsc("create_time");
        List<ProjectFile> contractFiles = projectFileService.list(contractWrapper);
        result.put("contractFiles", contractFiles);

        // 8. 里程碑节点
        QueryWrapper<SignMilestone> milestoneWrapper = new QueryWrapper<>();
        milestoneWrapper.eq("project_id", projectId).orderByAsc("id");
        List<SignMilestone> milestones = signMilestoneService.list(milestoneWrapper);
        result.put("milestones", milestones);

        // 9. 收款节点
        QueryWrapper<SignPayment> paymentWrapper = new QueryWrapper<>();
        paymentWrapper.eq("project_id", projectId).orderByAsc("id");
        List<SignPayment> payments = signPaymentService.list(paymentWrapper);
        result.put("payments", payments);

        // 10. 拜访记录（含附件）
        QueryWrapper<SignVisit> visitWrapper = new QueryWrapper<>();
        visitWrapper.eq("project_id", projectId);
        visitWrapper.orderByDesc("visit_start_time");
        List<SignVisit> visits = signVisitService.list(visitWrapper);

        List<Map<String, Object>> visitList = new ArrayList<>();
        for (SignVisit v : visits) {
            Map<String, Object> visitMap = new HashMap<>();
            visitMap.put("id", v.getId());
            visitMap.put("projectId", v.getProjectId());
            visitMap.put("visitTarget", v.getVisitTarget());
            visitMap.put("communicateContent", v.getCommunicateContent());
            visitMap.put("visitStartTime", v.getVisitStartTime() != null ? v.getVisitStartTime().toString() : null);
            visitMap.put("visitEndTime", v.getVisitEndTime() != null ? v.getVisitEndTime().toString() : null);
            visitMap.put("createTime", v.getCreateTime() != null ? v.getCreateTime().toString() : null);

            // 查询附件
            QueryWrapper<SignVisitFile> vFileWrapper = new QueryWrapper<>();
            vFileWrapper.eq("visit_id", v.getId());
            List<SignVisitFile> vFiles = signVisitFileService.list(vFileWrapper);
            visitMap.put("files", vFiles);

            visitList.add(visitMap);
        }
        result.put("visits", visitList);

        result.put("code", 200);
        result.put("msg", "查询成功");
        return result;
    }
     
}
