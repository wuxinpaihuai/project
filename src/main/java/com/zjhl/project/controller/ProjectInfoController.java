package com.zjhl.project.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjhl.project.entity.ProjectFile;
import com.zjhl.project.entity.ProjectInfo;
import com.zjhl.project.entity.ProjectStage;
import com.zjhl.project.service.ProjectFileService;
import com.zjhl.project.service.ProjectInfoService;
import com.zjhl.project.service.ProjectStageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/project/info")
public class ProjectInfoController {

    @Autowired
    private ProjectInfoService projectInfoService;

    @Autowired
    private ProjectStageService projectStageService;

    @Autowired
    private ProjectFileService projectFileService;

    /**
     * 项目列表（分页+条件查询）
     */
    @GetMapping("/list")
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String projectNo,
            @RequestParam(required = false) String projectName) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        QueryWrapper<ProjectInfo> wrapper = new QueryWrapper<>();
        if (projectNo != null && !projectNo.isEmpty()) {
            wrapper.like("project_no", projectNo);
        }
        if (projectName != null && !projectName.isEmpty()) {
            wrapper.like("project_name", projectName);
        }
        wrapper.orderByDesc("create_time");

        Page<ProjectInfo> page = new Page<>(pageNum, pageSize);
        Page<ProjectInfo> resultPage = projectInfoService.page(page, wrapper);

        // 为每条项目记录填充项目状态（stage_status）
        List<ProjectInfo> records = resultPage.getRecords();
        for (ProjectInfo info : records) {
            QueryWrapper<ProjectStage> stageWrapper = new QueryWrapper<>();
            stageWrapper.eq("project_id", info.getId());
            //stageWrapper.eq("stage_type", 1);
            stageWrapper.orderByDesc("create_time");
            List<ProjectStage> stages = projectStageService.list(stageWrapper);
            if (!stages.isEmpty()) {
                info.setStageStatus(stages.get(0).getStageStatus());
                info.setStageType(stages.get(0).getStageType());
            }
        }

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("total", resultPage.getTotal());
        result.put("records", records);
        return result;
    }

    /**
     * 根据ID获取项目详情
     */
    @GetMapping("/get/{id}")
    public Map<String, Object> get(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        ProjectInfo project = projectInfoService.getById(id);
        if (project != null) {
            result.put("code", 200);
            result.put("msg", "查询成功");
            result.put("data", project);
        } else {
            result.put("code", 404);
            result.put("msg", "项目不存在");
        }
        return result;
    }

    /**
     * 新增项目（同时创建投标阶段记录、保存附件）
     */
    @PostMapping("/add")
    public Map<String, Object> add(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        ProjectInfo project = new ProjectInfo();
        project.setProjectName((String) params.get("projectName"));
        project.setProjectNo((String) params.get("projectNo"));
        project.setProjectType(params.get("projectType") != null ? Integer.parseInt(params.get("projectType").toString()) : null);
        project.setBidType(params.get("bidType") != null ? Integer.parseInt(params.get("bidType").toString()) : null);
        project.setBidWay(params.get("bidWay") != null ? Integer.parseInt(params.get("bidWay").toString()) : null);
        project.setArea((String) params.get("area"));
        project.setProjectSource((String) params.get("projectSource"));
        project.setOwnerCompany((String) params.get("ownerCompany"));
        project.setOwnerContact((String) params.get("ownerContact"));
        project.setOwnerPhone((String) params.get("ownerPhone"));
        project.setOwnerPost((String) params.get("ownerPost"));
        project.setBidWebsite((String) params.get("bidWebsite"));
        project.setBidUrl((String) params.get("bidUrl"));
        project.setPriceType(params.get("priceType") != null ? Integer.parseInt(params.get("priceType").toString()) : null);
        project.setProjectCycle(params.get("projectCycle") != null ? Integer.parseInt(params.get("projectCycle").toString()) : null);
        project.setProjectAmount(params.get("projectAmount") != null ? new java.math.BigDecimal(params.get("projectAmount").toString()) : null);
        project.setMaxPrice(params.get("maxPrice") != null ? new java.math.BigDecimal(params.get("maxPrice").toString()) : null);
        project.setMinPrice(params.get("minPrice") != null ? new java.math.BigDecimal(params.get("minPrice").toString()) : null);
        project.setPriceUnit((String) params.get("priceUnit"));
        project.setScoreMethod((String) params.get("scoreMethod"));
        project.setBidDocumentFee(params.get("bidDocumentFee") != null ? new java.math.BigDecimal(params.get("bidDocumentFee").toString()) : null);
        project.setPlatformFee(params.get("platformFee") != null ? new java.math.BigDecimal(params.get("platformFee").toString()) : null);
        project.setAgencyFee(params.get("agencyFee") != null ? new java.math.BigDecimal(params.get("agencyFee").toString()) : null);
        project.setBidDeposit(params.get("bidDeposit") != null ? new java.math.BigDecimal(params.get("bidDeposit").toString()) : null);
        project.setContractDeposit(params.get("contractDeposit") != null ? new java.math.BigDecimal(params.get("contractDeposit").toString()) : null);
        project.setQuestionEndTime(params.get("questionEndTime") != null && !params.get("questionEndTime").toString().isEmpty() ? LocalDateTime.parse(params.get("questionEndTime").toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
        project.setBidTime(params.get("bidTime") != null && !params.get("bidTime").toString().isEmpty() ? LocalDateTime.parse(params.get("bidTime").toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
        project.setQualificationRequire((String) params.get("qualificationRequire"));
        project.setDocumentBindingFee(params.get("documentBindingFee") != null ? new java.math.BigDecimal(params.get("documentBindingFee").toString()) : null);
        project.setRemark((String) params.get("remark"));
        project.setContentDetail((String) params.get("contentDetail"));
        project.setCreateTime(LocalDateTime.now());
        project.setUpdateTime(LocalDateTime.now());

        boolean success = projectInfoService.save(project);
        if (success) {
            // 向项目阶段表插入一条投标阶段记录
            ProjectStage stage = new ProjectStage();
            stage.setProjectId(project.getId());
            stage.setStageType(1); // 1=投标阶段
            stage.setStageStatus(0); // 0=未开始
            stage.setCreateTime(LocalDateTime.now());
            projectStageService.save(stage);

            // 保存附件
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> fileList = (List<Map<String, Object>>) params.get("fileList");
            if (fileList != null && !fileList.isEmpty()) {
                for (Map<String, Object> fileMap : fileList) {
                    ProjectFile pf = new ProjectFile();
                    pf.setProjectId(project.getId());
                    pf.setStageType(1); // 新增时默认投标阶段
                    pf.setFilePath((String) fileMap.get("filePath"));
                    pf.setOriginalName((String) fileMap.get("originalName"));
                    pf.setCreateTime(LocalDateTime.now());
                    projectFileService.save(pf);
                }
            }

            result.put("code", 200);
            result.put("msg", "新增成功");
        } else {
            result.put("code", 500);
            result.put("msg", "新增失败");
        }
        return result;
    }

    /**
     * 更新项目
     */
    @PutMapping("/update")
    public Map<String, Object> update(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        Long id = Long.parseLong(params.get("id").toString());

        ProjectInfo project = new ProjectInfo();
        project.setId(id);
        project.setProjectName((String) params.get("projectName"));
        project.setProjectNo((String) params.get("projectNo"));
        project.setProjectType(params.get("projectType") != null ? Integer.parseInt(params.get("projectType").toString()) : null);
        project.setBidType(params.get("bidType") != null ? Integer.parseInt(params.get("bidType").toString()) : null);
        project.setBidWay(params.get("bidWay") != null ? Integer.parseInt(params.get("bidWay").toString()) : null);
        project.setArea((String) params.get("area"));
        project.setProjectSource((String) params.get("projectSource"));
        project.setOwnerCompany((String) params.get("ownerCompany"));
        project.setOwnerContact((String) params.get("ownerContact"));
        project.setOwnerPhone((String) params.get("ownerPhone"));
        project.setOwnerPost((String) params.get("ownerPost"));
        project.setBidWebsite((String) params.get("bidWebsite"));
        project.setBidUrl((String) params.get("bidUrl"));
        project.setPriceType(params.get("priceType") != null ? Integer.parseInt(params.get("priceType").toString()) : null);
        project.setProjectCycle(params.get("projectCycle") != null ? Integer.parseInt(params.get("projectCycle").toString()) : null);
        project.setProjectAmount(params.get("projectAmount") != null ? new java.math.BigDecimal(params.get("projectAmount").toString()) : null);
        project.setMaxPrice(params.get("maxPrice") != null ? new java.math.BigDecimal(params.get("maxPrice").toString()) : null);
        project.setMinPrice(params.get("minPrice") != null ? new java.math.BigDecimal(params.get("minPrice").toString()) : null);
        project.setPriceUnit((String) params.get("priceUnit"));
        project.setScoreMethod((String) params.get("scoreMethod"));
        project.setBidDocumentFee(params.get("bidDocumentFee") != null ? new java.math.BigDecimal(params.get("bidDocumentFee").toString()) : null);
        project.setPlatformFee(params.get("platformFee") != null ? new java.math.BigDecimal(params.get("platformFee").toString()) : null);
        project.setAgencyFee(params.get("agencyFee") != null ? new java.math.BigDecimal(params.get("agencyFee").toString()) : null);
        project.setBidDeposit(params.get("bidDeposit") != null ? new java.math.BigDecimal(params.get("bidDeposit").toString()) : null);
        project.setContractDeposit(params.get("contractDeposit") != null ? new java.math.BigDecimal(params.get("contractDeposit").toString()) : null);
        project.setQuestionEndTime(params.get("questionEndTime") != null && !params.get("questionEndTime").toString().isEmpty() ? LocalDateTime.parse(params.get("questionEndTime").toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
        project.setBidTime(params.get("bidTime") != null && !params.get("bidTime").toString().isEmpty() ? LocalDateTime.parse(params.get("bidTime").toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
        project.setQualificationRequire((String) params.get("qualificationRequire"));
        project.setDocumentBindingFee(params.get("documentBindingFee") != null ? new java.math.BigDecimal(params.get("documentBindingFee").toString()) : null);
        project.setRemark((String) params.get("remark"));
        project.setContentDetail((String) params.get("contentDetail"));
        project.setUpdateTime(LocalDateTime.now());

        boolean success = projectInfoService.updateById(project);
        if (success) {
            // 更新附件：先删除旧的，再保存新的
            QueryWrapper<ProjectFile> delWrapper = new QueryWrapper<>();
            delWrapper.eq("project_id", id);
            delWrapper.eq("stage_type", 1); // 投标阶段的附件
            projectFileService.remove(delWrapper);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> fileList = (List<Map<String, Object>>) params.get("fileList");
            if (fileList != null && !fileList.isEmpty()) {
                for (Map<String, Object> fileMap : fileList) {
                    ProjectFile pf = new ProjectFile();
                    pf.setProjectId(id);
                    pf.setStageType(1);
                    pf.setFilePath((String) fileMap.get("filePath"));
                    pf.setOriginalName((String) fileMap.get("originalName"));
                    pf.setCreateTime(LocalDateTime.now());
                    projectFileService.save(pf);
                }
            }

            result.put("code", 200);
            result.put("msg", "更新成功");
        } else {
            result.put("code", 500);
            result.put("msg", "更新失败");
        }
        return result;
    }

    /**
     * 删除项目
     */
    @DeleteMapping("/delete/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        // 删除项目附件
        QueryWrapper<ProjectFile> fileWrapper = new QueryWrapper<>();
        fileWrapper.eq("project_id", id);
        projectFileService.remove(fileWrapper);

        // 删除项目阶段
        QueryWrapper<ProjectStage> stageWrapper = new QueryWrapper<>();
        stageWrapper.eq("project_id", id);
        projectStageService.remove(stageWrapper);

        boolean success = projectInfoService.removeById(id);
        if (success) {
            result.put("code", 200);
            result.put("msg", "删除成功");
        } else {
            result.put("code", 500);
            result.put("msg", "删除失败");
        }
        return result;
    }

    /**
     * 获取项目附件列表
     */
    @GetMapping("/files/{projectId}")
    public Map<String, Object> getProjectFiles(@PathVariable Long projectId) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        QueryWrapper<ProjectFile> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId);
        wrapper.orderByAsc("create_time");
        List<ProjectFile> files = projectFileService.list(wrapper);

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", files);
        return result;
    }
}
