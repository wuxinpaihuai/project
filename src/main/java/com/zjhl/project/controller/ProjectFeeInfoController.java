package com.zjhl.project.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjhl.project.entity.ProjectFeeInfo;
import com.zjhl.project.entity.ProjectInfo;
import com.zjhl.project.service.ProjectFeeInfoService;
import com.zjhl.project.service.ProjectInfoService;

import cn.dev33.satoken.stp.StpUtil;

@RestController
@RequestMapping("/project/fee")
public class ProjectFeeInfoController {

    @Autowired
    private ProjectFeeInfoService projectFeeInfoService;

    @Autowired
    private ProjectInfoService projectInfoService;

    /** 费用类型映射 */
    private static final Map<Integer, String> FEE_TYPE_MAP = new HashMap<>();
    static {
        FEE_TYPE_MAP.put(1, "监测费/工程直接成本");
        FEE_TYPE_MAP.put(2, "评审费");
        FEE_TYPE_MAP.put(3, "协作费/业务费");
        FEE_TYPE_MAP.put(4, "公示公告费");
        FEE_TYPE_MAP.put(5, "材料费");
        FEE_TYPE_MAP.put(6, "差旅费");
        FEE_TYPE_MAP.put(7, "装订费/快递费");
        FEE_TYPE_MAP.put(8, "其他直接费用");
    }

    /**
     * 费用报销列表 - 查询当前登录用户的费用记录
     */
    @GetMapping("/list")
    public Map<String, Object> feeList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String projectName,
            @RequestParam(required = false) Integer feeType) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        Long userId = StpUtil.getLoginIdAsLong();

        // 1. 查询当前用户的费用记录
        QueryWrapper<ProjectFeeInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("submit_user_id", userId);
        if (feeType != null) {
            wrapper.eq("fee_type", feeType);
        }
        wrapper.orderByDesc("create_time");

        Page<ProjectFeeInfo> page = new Page<>(pageNum, pageSize);
        Page<ProjectFeeInfo> resultPage = projectFeeInfoService.page(page, wrapper);

        // 2. 收集所有projectId，批量查项目信息
        List<Long> projectIds = resultPage.getRecords().stream()
                .map(ProjectFeeInfo::getProjectId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, ProjectInfo> projectMap = new HashMap<>();
        if (!projectIds.isEmpty()) {
            // 如果有项目名称模糊搜索条件，先按名称过滤项目ID
            List<Long> matchProjectIds = null;
            if (projectName != null && !projectName.isEmpty()) {
                QueryWrapper<ProjectInfo> pWrapper = new QueryWrapper<>();
                pWrapper.in("id", projectIds);
                pWrapper.like("project_name", projectName);
                List<ProjectInfo> matchProjects = projectInfoService.list(pWrapper);
                matchProjectIds = matchProjects.stream().map(ProjectInfo::getId).collect(Collectors.toList());
                if (matchProjectIds.isEmpty()) {
                    result.put("code", 200);
                    result.put("msg", "查询成功");
                    result.put("total", 0);
                    result.put("records", Collections.EMPTY_LIST);
                    return result;
                }
            }

            // 查询项目信息
            QueryWrapper<ProjectInfo> pInfoWrapper = new QueryWrapper<>();
            if (matchProjectIds != null) {
                pInfoWrapper.in("id", matchProjectIds);
            } else {
                pInfoWrapper.in("id", projectIds);
            }
            List<ProjectInfo> projectList = projectInfoService.list(pInfoWrapper);
            for (ProjectInfo p : projectList) {
                projectMap.put(p.getId(), p);
            }
        }

        // 3. 组装结果
        List<Map<String, Object>> records = new ArrayList<>();
        for (ProjectFeeInfo fee : resultPage.getRecords()) {
            ProjectInfo pInfo = projectMap.get(fee.getProjectId());

            // 如果有项目名称筛选且当前记录的项目不匹配，跳过
            if (projectName != null && !projectName.isEmpty()) {
                if (pInfo == null || !projectMap.containsKey(fee.getProjectId())) {
                    continue;
                }
            }

            Map<String, Object> row = new HashMap<>();
            row.put("id", fee.getId());
            row.put("projectId", fee.getProjectId());
            row.put("projectName", pInfo != null ? pInfo.getProjectName() : "-");
            row.put("projectNo", pInfo != null ? pInfo.getProjectNo() : "-");
            row.put("ownerCompany", pInfo != null ? pInfo.getOwnerCompany() : "-");
            row.put("feeType", fee.getFeeType());
            row.put("feeTypeName", FEE_TYPE_MAP.getOrDefault(fee.getFeeType(), "未知"));
            row.put("feeAmount", fee.getFeeAmount());
            row.put("feeStatus", fee.getFeeStatus());
            row.put("feeStatusName", fee.getFeeStatus() != null && fee.getFeeStatus() == 1 ? "已提交" : "未提交");
            row.put("remark", fee.getRemark() != null ? fee.getRemark() : "");
            row.put("createTime", fee.getCreateTime() != null ? fee.getCreateTime().toString() : "");
            records.add(row);
        }

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("total", resultPage.getTotal());
        result.put("records", records);
        return result;
    }

    /**
     * 新增费用报销
     */
    @PostMapping("/add")
    public Map<String, Object> addFee(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        ProjectFeeInfo fee = new ProjectFeeInfo();
        fee.setProjectId(Long.parseLong(params.get("projectId").toString()));
        fee.setFeeType(params.get("feeType") != null ? Integer.parseInt(params.get("feeType").toString()) : null);
        fee.setFeeAmount(params.get("feeAmount") != null ? new BigDecimal(params.get("feeAmount").toString()) : null);
        fee.setFeeStatus(params.get("feeStatus") != null ? Integer.parseInt(params.get("feeStatus").toString()) : 0);
        fee.setRemark(params.get("remark") != null ? params.get("remark").toString() : null);
        fee.setSubmitUserId(StpUtil.getLoginIdAsLong());
        fee.setSubmitUserName(params.get("submitUserName") != null ? params.get("submitUserName").toString() : null);
        fee.setSubmitUserPhone(params.get("submitUserPhone") != null ? params.get("submitUserPhone").toString() : null);
        fee.setCreateTime(LocalDateTime.now());
        fee.setUpdateTime(LocalDateTime.now());

        boolean success = projectFeeInfoService.save(fee);
        if (success) {
            result.put("code", 200);
            result.put("msg", "新增成功");
            result.put("id", fee.getId());
        } else {
            result.put("code", 500);
            result.put("msg", "新增失败");
        }
        return result;
    }

    /**
     * 编辑费用报销
     */
    @PutMapping("/update")
    public Map<String, Object> updateFee(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        Long id = Long.parseLong(params.get("id").toString());

        // 校验：只有未提交状态才能编辑
        ProjectFeeInfo existing = projectFeeInfoService.getById(id);
        if (existing == null) {
            result.put("code", 404);
            result.put("msg", "记录不存在");
            return result;
        }
        if (existing.getFeeStatus() != null && existing.getFeeStatus() == 1) {
            result.put("code", 400);
            result.put("msg", "已提交的记录不能编辑");
            return result;
        }

        // 校验：只能编辑自己的记录
        if (!existing.getSubmitUserId().equals(StpUtil.getLoginIdAsLong())) {
            result.put("code", 403);
            result.put("msg", "无权操作");
            return result;
        }

        ProjectFeeInfo fee = new ProjectFeeInfo();
        fee.setId(id);
        fee.setProjectId(Long.parseLong(params.get("projectId").toString()));
        fee.setFeeType(params.get("feeType") != null ? Integer.parseInt(params.get("feeType").toString()) : null);
        fee.setFeeAmount(params.get("feeAmount") != null ? new BigDecimal(params.get("feeAmount").toString()) : null);
        fee.setFeeStatus(params.get("feeStatus") != null ? Integer.parseInt(params.get("feeStatus").toString()) : existing.getFeeStatus());
        fee.setRemark(params.get("remark") != null ? params.get("remark").toString() : null);
        fee.setUpdateTime(LocalDateTime.now());

        boolean success = projectFeeInfoService.updateById(fee);
        if (success) {
            result.put("code", 200);
            result.put("msg", "编辑成功");
        } else {
            result.put("code", 500);
            result.put("msg", "编辑失败");
        }
        return result;
    }

    /**
     * 删除费用报销
     */
    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteFee(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        ProjectFeeInfo existing = projectFeeInfoService.getById(id);
        if (existing == null) {
            result.put("code", 404);
            result.put("msg", "记录不存在");
            return result;
        }
        if (existing.getFeeStatus() != null && existing.getFeeStatus() == 1) {
            result.put("code", 400);
            result.put("msg", "已提交的记录不能删除");
            return result;
        }
        if (!existing.getSubmitUserId().equals(StpUtil.getLoginIdAsLong())) {
            result.put("code", 403);
            result.put("msg", "无权操作");
            return result;
        }

        boolean success = projectFeeInfoService.removeById(id);
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
     * 获取单条费用记录详情（用于编辑回显）
     */
    @GetMapping("/get/{id}")
    public Map<String, Object> getFee(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        ProjectFeeInfo fee = projectFeeInfoService.getById(id);
        if (fee == null) {
            result.put("code", 404);
            result.put("msg", "记录不存在");
            return result;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", fee.getId());
        data.put("projectId", fee.getProjectId());
        data.put("feeType", fee.getFeeType());
        data.put("feeAmount", fee.getFeeAmount());
        data.put("feeStatus", fee.getFeeStatus());
        data.put("remark", fee.getRemark());
        data.put("submitUserName", fee.getSubmitUserName());
        data.put("submitUserPhone", fee.getSubmitUserPhone());

        // 项目名称
        ProjectInfo pInfo = projectInfoService.getById(fee.getProjectId());
        data.put("projectName", pInfo != null ? pInfo.getProjectName() : "");
        data.put("projectNo", pInfo != null ? pInfo.getProjectNo() : "");

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", data);
        return result;
    }

    /**
     * 获取项目列表（用于新增页面的项目下拉框，支持模糊搜索）
     */
    @GetMapping("/projects")
    public Map<String, Object> projectList(@RequestParam(required = false) String keyword) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        QueryWrapper<ProjectInfo> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like("project_name", keyword);
        }
        wrapper.orderByDesc("create_time");
        List<ProjectInfo> projects = projectInfoService.list(wrapper);

        List<Map<String, Object>> list = new ArrayList<>();
        for (ProjectInfo p : projects) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("projectName", p.getProjectName());
            map.put("projectNo", p.getProjectNo());
            // 下拉显示：项目名_项目编号
            String label = p.getProjectName();
            if (p.getProjectNo() != null && !p.getProjectNo().isEmpty()) {
                label += "_" + p.getProjectNo();
            }
            map.put("label", label);
            list.add(map);
        }

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", list);
        return result;
    }
}
