package com.zjhl.project.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjhl.project.entity.ProjectExtend;
import com.zjhl.project.entity.ProjectFile;
import com.zjhl.project.entity.ProjectInfo;
import com.zjhl.project.entity.ProjectStage;
import com.zjhl.project.service.ProjectExtendService;
import com.zjhl.project.service.ProjectFileService;
import com.zjhl.project.service.ProjectInfoService;
import com.zjhl.project.service.ProjectStageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
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

    @Autowired
    private ProjectExtendService projectExtendService;

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

        // 为每条项目记录推导「项目阶段」与「项目状态」
        List<ProjectInfo> records = resultPage.getRecords();
        for (ProjectInfo info : records) {
            ProjectStage currentStage = resolveCurrentStage(info.getId());
            if (currentStage != null) {
                info.setStageType(currentStage.getStageType());
                info.setStageStatus(currentStage.getStageStatus());
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
        project.setBisnessUserId(params.get("bisnessUserId") != null ? Long.parseLong(params.get("bisnessUserId").toString()) : null);
        project.setBisnessUserName((String) params.get("bisnessUserName"));
        project.setBisnessUserPhone((String) params.get("bisnessUserPhone"));
        project.setTechUserId(params.get("techUserId") != null ? Long.parseLong(params.get("techUserId").toString()) : null);
        project.setTechUserName((String) params.get("techUserName"));
        project.setTechUserPhone((String) params.get("techUserPhone"));
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
            stage.setStageStatus(1); // 0=未开始 1投标中 默认
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
     * 新增现有项目（已有项目直接入库，无需招标流程）
     * 保存成功后自动写入：
     *   1) project_stage 三条记录：
     *      - 投标阶段(stage_type=1, stage_status=2, remark=现有项目无需投标)
     *      - 签约阶段(stage_type=2, stage_status=1, remark=现有项目自动进入签约阶段)
     *      - 实施阶段(stage_type=3, stage_status=1, remark=现有项目自动进入签约阶段)
     *   2) project_extend 一条记录：is_win_bid=1、is_sign=0
     */
    @PostMapping("/addOld")
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> addOld(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        // ========== 1. 保存项目基本信息 ==========
        ProjectInfo project = new ProjectInfo();
        project.setProjectName((String) params.get("projectName"));
        project.setProjectNo((String) params.get("projectNo"));
        project.setProjectType(params.get("projectType") != null ? Integer.parseInt(params.get("projectType").toString()) : null);
        project.setArea((String) params.get("area"));
        project.setProjectSource((String) params.get("projectSource"));
        project.setProjectCycle(params.get("projectCycle") != null ? Integer.parseInt(params.get("projectCycle").toString()) : null);
        project.setOwnerCompany((String) params.get("ownerCompany"));
        project.setOwnerContact((String) params.get("ownerContact"));
        project.setOwnerPhone((String) params.get("ownerPhone"));
        project.setOwnerPost((String) params.get("ownerPost"));
        project.setBisnessUserId(params.get("bisnessUserId") != null ? Long.parseLong(params.get("bisnessUserId").toString()) : null);
        project.setBisnessUserName((String) params.get("bisnessUserName"));
        project.setBisnessUserPhone((String) params.get("bisnessUserPhone"));
        project.setTechUserId(params.get("techUserId") != null ? Long.parseLong(params.get("techUserId").toString()) : null);
        project.setTechUserName((String) params.get("techUserName"));
        project.setTechUserPhone((String) params.get("techUserPhone"));
        project.setProjectAmount(params.get("projectAmount") != null ? new java.math.BigDecimal(params.get("projectAmount").toString()) : null);
        project.setPriceUnit((String) params.get("priceUnit"));
        project.setContentDetail((String) params.get("contentDetail"));
        project.setRemark((String) params.get("remark"));
        project.setCreateTime(LocalDateTime.now());
        project.setUpdateTime(LocalDateTime.now());

        boolean success = projectInfoService.save(project);
        if (!success) {
            result.put("code", 500);
            result.put("msg", "新增失败");
            return result;
        }

        Long newProjectId = project.getId();

        // ========== 2. 写入三条项目阶段记录 ==========
        // 2.1 投标阶段：直接标记为已完成（现有项目无需投标）
        ProjectStage bidStage = new ProjectStage();
        bidStage.setProjectId(newProjectId);
        bidStage.setStageType(1);
        bidStage.setStageStatus(2);
        bidStage.setStageRemark("现有项目无需投标");
        bidStage.setCreateTime(LocalDateTime.now());
        projectStageService.save(bidStage);

        // 2.2 签约阶段：进行中
        ProjectStage signStage = new ProjectStage();
        signStage.setProjectId(newProjectId);
        signStage.setStageType(2);
        signStage.setStageStatus(1);
        signStage.setStageRemark("现有项目自动进入签约阶段");
        signStage.setCreateTime(LocalDateTime.now());
        projectStageService.save(signStage);

        // 2.3 实施阶段：进行中
        ProjectStage execStage = new ProjectStage();
        execStage.setProjectId(newProjectId);
        execStage.setStageType(3);
        execStage.setStageStatus(1);
        execStage.setStageRemark("现有项目自动进入实施阶段");
        execStage.setCreateTime(LocalDateTime.now());
        projectStageService.save(execStage);

        // ========== 3. 写入项目拓展信息（已中标、未签约） ==========
        ProjectExtend extend = new ProjectExtend();
        extend.setProjectId(newProjectId);
        extend.setIsWinBid(1);
        extend.setIsSign(0);
        extend.setCreateTime(LocalDateTime.now());
        projectExtendService.save(extend);

        // ========== 4. 保存项目附件 ==========
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fileList = (List<Map<String, Object>>) params.get("fileList");
        if (fileList != null && !fileList.isEmpty()) {
            for (Map<String, Object> fileMap : fileList) {
                ProjectFile pf = new ProjectFile();
                pf.setProjectId(newProjectId);
                // 现有项目自动进入签约阶段，附件默认归属签约阶段
                pf.setStageType(2);
                pf.setFilePath((String) fileMap.get("filePath"));
                pf.setOriginalName((String) fileMap.get("originalName"));
                pf.setCreateTime(LocalDateTime.now());
                projectFileService.save(pf);
            }
        }

        result.put("code", 200);
        result.put("msg", "新增成功");
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
        project.setBisnessUserId(params.get("bisnessUserId") != null ? Long.parseLong(params.get("bisnessUserId").toString()) : null);
        project.setBisnessUserName((String) params.get("bisnessUserName"));
        project.setBisnessUserPhone((String) params.get("bisnessUserPhone"));
        project.setTechUserId(params.get("techUserId") != null ? Long.parseLong(params.get("techUserId").toString()) : null);
        project.setTechUserName((String) params.get("techUserName"));
        project.setTechUserPhone((String) params.get("techUserPhone"));
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

    /**
     * 推导项目当前所处阶段（用于列表页「项目阶段」「项目状态」两列）
     *
     * 规则（依据 project_stage 记录数 + project_extend.is_sign）：
     *   1) 只有招标阶段(type=1)                     → 项目阶段=招标阶段(1)，状态取 type=1 记录
     *   2) 招标(1) + 签约(2)                        → 项目阶段=签约阶段(2)，状态取 type=2 记录
     *   3) 存在实施阶段(3)记录                      → 查 project_extend.is_sign：
     *        is_sign=1 已签约 → 项目阶段=实施阶段(3)，状态取 type=3 记录
     *        is_sign=0 未签约 → 项目阶段=签约阶段(2)，状态取 type=2 记录
     *
     * @return 当前阶段对应的 project_stage 记录（其 stageStatus 即为项目状态）；无记录返回 null
     */
    private ProjectStage resolveCurrentStage(Long projectId) {
        QueryWrapper<ProjectStage> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId);
        List<ProjectStage> stages = projectStageService.list(wrapper);
        if (stages == null || stages.isEmpty()) {
            return null;
        }

        // 按阶段类型分组，同类型保留 create_time 最新的一条
        ProjectStage bidStage = null;   // type=1 招标/投标阶段
        ProjectStage signStage = null;  // type=2 签约阶段
        ProjectStage execStage = null;  // type=3 实施阶段
        for (ProjectStage s : stages) {
            Integer type = s.getStageType();
            if (type == null) {
                continue;
            }
            if (type == 1) {
                if (bidStage == null || isNewer(s, bidStage)) bidStage = s;
            } else if (type == 2) {
                if (signStage == null || isNewer(s, signStage)) signStage = s;
            } else if (type == 3) {
                if (execStage == null || isNewer(s, execStage)) execStage = s;
            }
        }

        // 存在实施阶段记录 → 由「是否已签约」决定当前阶段
        if (execStage != null) {
            if (isProjectSigned(projectId)) {
                return execStage;                       // 已签约 → 实施阶段
            }
            // 未签约 → 签约阶段（若没有签约阶段记录，则退回实施阶段记录）
            return signStage != null ? signStage : execStage;
        }

        // 尚在签约阶段
        if (signStage != null) {
            return signStage;
        }

        // 仅招标阶段
        if (bidStage != null) {
            return bidStage;
        }

        // 兜底
        return stages.get(0);
    }

    /** 判断 s1 是否比 s2 更新（create_time 更晚） */
    private boolean isNewer(ProjectStage s1, ProjectStage s2) {
        return s1.getCreateTime() != null && s2.getCreateTime() != null
                && s1.getCreateTime().isAfter(s2.getCreateTime());
    }

    /**
     * 项目是否已签约（project_extend.is_sign = 1）
     */
    private boolean isProjectSigned(Long projectId) {
        QueryWrapper<ProjectExtend> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId);
        wrapper.last("limit 1");
        ProjectExtend extend = projectExtendService.getOne(wrapper, false);
        return extend != null && extend.getIsSign() != null && extend.getIsSign() == 1;
    }
}
