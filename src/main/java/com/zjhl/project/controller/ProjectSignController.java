package com.zjhl.project.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjhl.project.entity.*;
import com.zjhl.project.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
}
