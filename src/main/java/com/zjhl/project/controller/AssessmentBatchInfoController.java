package com.zjhl.project.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zjhl.project.entity.AssessmentBatch;
import com.zjhl.project.entity.ProjectExtend;
import com.zjhl.project.entity.ProjectFeeInfo;
import com.zjhl.project.entity.ProjectIncome;
import com.zjhl.project.entity.ProjectInfo;
import com.zjhl.project.entity.ProjectAssessmentUser;
import com.zjhl.project.entity.SysUser;
import com.zjhl.project.service.AssessmentBatchService;
import com.zjhl.project.service.ProjectExtendService;
import com.zjhl.project.service.ProjectFeeInfoService;
import com.zjhl.project.service.ProjectIncomeService;
import com.zjhl.project.service.ProjectInfoService;
import com.zjhl.project.service.ProjectAssessmentUserService;
import com.zjhl.project.service.SysUserService;

import cn.dev33.satoken.stp.StpUtil;

@RestController
@RequestMapping("/assessment/info")
public class AssessmentBatchInfoController {

    @Autowired
    private AssessmentBatchService assessmentBatchService;
    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private ProjectAssessmentUserService projectAssessmentUserService;
    @Autowired
    private ProjectExtendService projectExtendService;
    @Autowired
    private ProjectInfoService projectInfoService;
    @Autowired
    private ProjectIncomeService projectIncomeService;
    @Autowired
    private ProjectFeeInfoService projectFeeInfoService;

    private static final Map<Integer, String> ASSESS_TYPE_MAP = new HashMap<>();
    static {
        ASSESS_TYPE_MAP.put(1, "报告书");
        ASSESS_TYPE_MAP.put(7, "报告表");
        ASSESS_TYPE_MAP.put(8, "登记表");
        ASSESS_TYPE_MAP.put(2, "场调");
        ASSESS_TYPE_MAP.put(3, "应急预案");
        ASSESS_TYPE_MAP.put(4, "验收");
        ASSESS_TYPE_MAP.put(5, "环评入围");
        ASSESS_TYPE_MAP.put(6, "场调入围");
        ASSESS_TYPE_MAP.put(99, "其他");
    }

    private static final Map<Integer, String> MEETING_APPROVE_MAP = new HashMap<>();
    static {
        MEETING_APPROVE_MAP.put(0, "一次性过会");
        MEETING_APPROVE_MAP.put(1, "多次过会");
        MEETING_APPROVE_MAP.put(2, "其他");
    }

    /**
     * 获取考核批次下拉（assess_status=2或3）
     */
    @GetMapping("/batchOptions")
    public Map<String, Object> batchOptions() {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        QueryWrapper<AssessmentBatch> wrapper = new QueryWrapper<>();
        wrapper.in("assess_status", 2, 3);
        wrapper.orderByDesc("create_time");
        List<AssessmentBatch> list = assessmentBatchService.list(wrapper);

        List<Map<String, Object>> options = new ArrayList<>();
        for (AssessmentBatch b : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", b.getId());
            m.put("batchName", b.getBatchName());
            m.put("assessBeginDay", b.getAssessBeginDay() != null ? b.getAssessBeginDay().toString() : "");
            m.put("assessEndDay", b.getAssessEndDay() != null ? b.getAssessEndDay().toString() : "");
            options.add(m);
        }
        result.put("code", 200);
        result.put("data", options);
        return result;
    }

    /**
     * 获取考核人员下拉（is_assess=1）
     */
    @GetMapping("/userOptions")
    public Map<String, Object> userOptions(@RequestParam(required = false) String keyword) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("is_assess", 1);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like("real_name", keyword);
        }
        wrapper.orderByDesc("create_time");
        List<SysUser> list = sysUserService.list(wrapper);

        List<Map<String, Object>> options = new ArrayList<>();
        for (SysUser u : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("realName", u.getRealName());
            m.put("phone", u.getPhone());
            String label = (u.getRealName() != null ? u.getRealName() : "") + "_" + (u.getPhone() != null ? u.getPhone() : "");
            m.put("label", label);
            options.add(m);
        }
        result.put("code", 200);
        result.put("data", options);
        return result;
    }

    /**
     * 统计考核明细 - 查询某用户在某个批次下参与的项目统计
     */
    @GetMapping("/stat")
    public Map<String, Object> stat(
            @RequestParam Long batchId,
            @RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        // 1. 获取批次信息
        AssessmentBatch batch = assessmentBatchService.getById(batchId);
        if (batch == null) {
            result.put("code", 400);
            result.put("msg", "批次不存在");
            return result;
        }
        LocalDate beginDay = batch.getAssessBeginDay();
        LocalDate endDay = batch.getAssessEndDay();

        // 2. 获取用户信息
        SysUser user = sysUserService.getById(userId);
        String userName = user != null ? user.getRealName() : "";

        // 3. 查询该用户作为技术人员参与的项目（project_assessment_user表）
        QueryWrapper<ProjectAssessmentUser> ratioWrapper = new QueryWrapper<>();
        ratioWrapper.eq("user_id", userId);
        List<ProjectAssessmentUser> ratioList = projectAssessmentUserService.list(ratioWrapper);
        List<Long> techProjectIds = ratioList.stream().map(ProjectAssessmentUser::getProjectId).distinct().collect(Collectors.toList());
        // ratioMap: projectId -> ratio
        Map<Long, BigDecimal> ratioMap = new HashMap<>();
        for (ProjectAssessmentUser r : ratioList) {
            ratioMap.put(r.getProjectId(), r.getRatio());
        }

        // 4. 查询该用户作为评审人员参与的项目（project_extend表）
        QueryWrapper<ProjectExtend> extWrapper = new QueryWrapper<>();
        extWrapper.eq("ys_user_id", userId).or().eq("es_user_id", userId).or().eq("ss_user_id", userId);
        List<ProjectExtend> extList = projectExtendService.list(extWrapper);
        List<Long> reviewProjectIds = extList.stream().map(ProjectExtend::getProjectId).distinct().collect(Collectors.toList());

        // 去掉同时作为技术人员的项目（避免重复）
        reviewProjectIds.removeAll(techProjectIds);

        // 合并所有项目ID
        List<Long> allProjectIds = new ArrayList<>(techProjectIds);
        allProjectIds.addAll(reviewProjectIds);

        if (allProjectIds.isEmpty()) {
            result.put("code", 200);
            result.put("data", new ArrayList<>());
            return result;
        }

        // 5. 批量查询项目信息
        QueryWrapper<ProjectInfo> pInfoWrapper = new QueryWrapper<>();
        pInfoWrapper.in("id", allProjectIds);
        List<ProjectInfo> projectInfoList = projectInfoService.list(pInfoWrapper);
        Map<Long, ProjectInfo> pInfoMap = projectInfoList.stream().collect(Collectors.toMap(ProjectInfo::getId, p -> p));

        // 6. 批量查询项目拓展信息
        QueryWrapper<ProjectExtend> pExtWrapper = new QueryWrapper<>();
        pExtWrapper.in("project_id", allProjectIds);
        List<ProjectExtend> pExtList = projectExtendService.list(pExtWrapper);
        Map<Long, ProjectExtend> pExtMap = new HashMap<>();
        for (ProjectExtend pe : pExtList) {
            pExtMap.put(pe.getProjectId(), pe);
        }

        // 7. 批量查询收款信息（按项目分组统计，按日期范围）
        QueryWrapper<ProjectIncome> incomeWrapper = new QueryWrapper<>();
        incomeWrapper.in("project_id", allProjectIds);
        if (beginDay != null) {
            incomeWrapper.ge("receive_time", beginDay);
        }
        if (endDay != null) {
            incomeWrapper.le("receive_time", endDay);
        }
        List<ProjectIncome> incomeList = projectIncomeService.list(incomeWrapper);
        Map<Long, BigDecimal> incomeMap = new HashMap<>();
        for (ProjectIncome inc : incomeList) {
            Long pid = inc.getProjectId();
            BigDecimal existing = incomeMap.getOrDefault(pid, BigDecimal.ZERO);
            incomeMap.put(pid, existing.add(inc.getReceiveAmount() != null ? inc.getReceiveAmount() : BigDecimal.ZERO));
        }

        // 8. 批量查询费用信息（按项目和类型分组）
        QueryWrapper<ProjectFeeInfo> feeWrapper = new QueryWrapper<>();
        feeWrapper.in("project_id", allProjectIds);
        feeWrapper.eq("fee_status", 1);
        List<ProjectFeeInfo> feeList = projectFeeInfoService.list(feeWrapper);
        // key: projectId_feeType -> sum
        Map<String, BigDecimal> feeMap = new HashMap<>();
        for (ProjectFeeInfo fee : feeList) {
            String key = fee.getProjectId() + "_" + fee.getFeeType();
            BigDecimal existing = feeMap.getOrDefault(key, BigDecimal.ZERO);
            feeMap.put(key, existing.add(fee.getFeeAmount() != null ? fee.getFeeAmount() : BigDecimal.ZERO));
        }

        // 9. 组装统计结果
        List<Map<String, Object>> records = new ArrayList<>();
        int seq = 1;
        for (Long pid : allProjectIds) {
            Map<String, Object> row = buildStatRow(seq++, pid, userId, userName,
                    techProjectIds.contains(pid), ratioMap, pInfoMap, pExtMap, incomeMap, feeMap);
            records.add(row);
        }

        result.put("code", 200);
        result.put("data", records);
        return result;
    }

    /**
     * 构建单条统计行
     */
    private Map<String, Object> buildStatRow(int seq, Long projectId, Long userId, String userName,
            boolean isTech, Map<Long, BigDecimal> ratioMap,
            Map<Long, ProjectInfo> pInfoMap, Map<Long, ProjectExtend> pExtMap,
            Map<Long, BigDecimal> incomeMap, Map<String, BigDecimal> feeMap) {

        Map<String, Object> row = new HashMap<>();
        row.put("seq", seq);
        row.put("userName", userName);

        ProjectInfo pInfo = pInfoMap.get(projectId);
        row.put("projectName", pInfo != null ? pInfo.getProjectName() : "-");
        row.put("ownerCompany", pInfo != null ? pInfo.getOwnerCompany() : "-");

        // 合同额
        ProjectExtend pExt = pExtMap.get(projectId);
        BigDecimal contractAmount = (pExt != null && pExt.getContractAmount() != null) ? pExt.getContractAmount() : BigDecimal.ZERO;
        row.put("contractAmount", contractAmount);

        // 收款额
        BigDecimal receiveAmount = incomeMap.getOrDefault(projectId, BigDecimal.ZERO);
        row.put("receiveAmount", receiveAmount);

        // 各类费用
        BigDecimal fee1 = feeMap.getOrDefault(projectId + "_1", BigDecimal.ZERO); // 监测费
        BigDecimal fee2 = feeMap.getOrDefault(projectId + "_2", BigDecimal.ZERO); // 评审费
        BigDecimal fee3 = feeMap.getOrDefault(projectId + "_3", BigDecimal.ZERO); // 协作费
        BigDecimal fee4 = feeMap.getOrDefault(projectId + "_4", BigDecimal.ZERO); // 公示公告费
        BigDecimal fee5 = feeMap.getOrDefault(projectId + "_5", BigDecimal.ZERO); // 材料费
        BigDecimal fee6 = feeMap.getOrDefault(projectId + "_6", BigDecimal.ZERO); // 差旅费
        BigDecimal fee7 = feeMap.getOrDefault(projectId + "_7", BigDecimal.ZERO); // 装订快递费
        BigDecimal fee8 = feeMap.getOrDefault(projectId + "_8", BigDecimal.ZERO); // 其他直接费用

        row.put("fee1", fee1);
        row.put("fee2", fee2);
        row.put("fee3", fee3);
        row.put("fee4", fee4);
        row.put("fee5", fee5);
        row.put("fee6", fee6);
        row.put("fee7", fee7);
        row.put("fee8", fee8);

        // 税费 = (收款额 - 监测费 - 协作费) * 7%
        BigDecimal taxFee = receiveAmount.subtract(fee1).subtract(fee3).multiply(new BigDecimal("0.07")).setScale(2, RoundingMode.HALF_UP);
        if (taxFee.compareTo(BigDecimal.ZERO) < 0) taxFee = BigDecimal.ZERO;
        row.put("taxFee", taxFee);

        // 净收款额 = 收款额 - 监测费 - 评审费 - 协作费 - 公示公告费 - 材料费 - 差旅费 - 装订快递费 - 税费 - 其他直接费用
        BigDecimal netReceive = receiveAmount.subtract(fee1).subtract(fee2).subtract(fee3)
                .subtract(fee4).subtract(fee5).subtract(fee6).subtract(fee7).subtract(taxFee).subtract(fee8)
                .setScale(2, RoundingMode.HALF_UP);
        row.put("netReceive", netReceive);

        // 净考核额 = 净收款额 * 考核分配比率(ratio/100)
        String remarkInfo = "";
        if (isTech) {
            BigDecimal ratio = ratioMap.get(projectId);
            if (ratio != null) {
                BigDecimal netAssess = netReceive.multiply(ratio.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP))
                        .setScale(2, RoundingMode.HALF_UP);
                row.put("netAssess", netAssess);
                row.put("ratioDisplay", ratio + "%");
            } else {
                row.put("netAssess", "-");
                row.put("ratioDisplay", "-");
                remarkInfo = "未配置项目分配比率";
            }
        } else {
            // 评审人员
            row.put("netAssess", "-");
            row.put("ratioDisplay", "-");
        }

        // 审批或备案
        row.put("isGovernmentApprove", pExt != null && pExt.getIsGovernmentApprove() != null ? (pExt.getIsGovernmentApprove() == 1 ? "是" : "否") : "-");

        // 过会审批
        row.put("meetingApprove", pExt != null && pExt.getMeetingApprove() != null ? MEETING_APPROVE_MAP.getOrDefault(pExt.getMeetingApprove(), "-") : "-");

        // 项目负责人
        row.put("techUserName", pInfo != null ? pInfo.getTechUserName() : "-");

        // 业务员
        row.put("bisnessUserName", pInfo != null ? pInfo.getBisnessUserName() : "-");

        // 技术员 & 占比
        if (isTech) {
            row.put("techPersonName", userName);
            BigDecimal ratio = ratioMap.get(projectId);
            row.put("techRatio", ratio != null ? ratio + "%" : "-");
        } else {
            row.put("techPersonName", "-");
            row.put("techRatio", "-");
        }

        // 一级审核、二级审核、三级审核
        row.put("ysUserName", pExt != null && pExt.getYsUserName() != null ? pExt.getYsUserName() : "-");
        row.put("esUserName", pExt != null && pExt.getEsUserName() != null ? pExt.getEsUserName() : "-");
        row.put("ssUserName", pExt != null && pExt.getSsUserName() != null ? pExt.getSsUserName() : "-");

        // 报告类型
        row.put("assessType", pExt != null && pExt.getAssessType() != null ? ASSESS_TYPE_MAP.getOrDefault(pExt.getAssessType(), "-") : "-");

        // 是否存档
        row.put("isArchiveFinish", pExt != null && pExt.getIsArchiveFinish() != null ? (pExt.getIsArchiveFinish() == 1 ? "是" : "否") : "-");

        // 进展情况
        row.put("progressInfo", pExt != null && pExt.getProgressInfo() != null ? pExt.getProgressInfo() : "-");

        // 备注
        row.put("remark", remarkInfo);

        return row;
    }

    /**
     * 下载考核明细列表 - 当前批次所有考核人员，每人一个sheet
     */
    @GetMapping("/download")
    public void download(@RequestParam Long batchId, HttpServletResponse response) {
        try {
            AssessmentBatch batch = assessmentBatchService.getById(batchId);
            if (batch == null) return;
            LocalDate beginDay = batch.getAssessBeginDay();
            LocalDate endDay = batch.getAssessEndDay();

            // 查询所有考核人员
            QueryWrapper<SysUser> userWrapper = new QueryWrapper<>();
            userWrapper.eq("is_assess", 1);
            userWrapper.orderByDesc("create_time");
            List<SysUser> assessUsers = sysUserService.list(userWrapper);

            // 表头
            List<List<String>> headList = buildExcelHead();

            String fileName = URLEncoder.encode(batch.getBatchName() + "_考核明细", StandardCharsets.UTF_8.toString());
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

            ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream())
                    .autoCloseStream(true)
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .registerWriteHandler(createCellStyleStrategy())
                    .build();

            for (SysUser assessUser : assessUsers) {
                Long userId = assessUser.getId();
                String userName = assessUser.getRealName() != null ? assessUser.getRealName() : "未知";

                // 查询该用户的统计数据（复用逻辑）
                QueryWrapper<ProjectAssessmentUser> ratioWrapper = new QueryWrapper<>();
                ratioWrapper.eq("user_id", userId);
                List<ProjectAssessmentUser> ratioList = projectAssessmentUserService.list(ratioWrapper);
                List<Long> techProjectIds = ratioList.stream().map(ProjectAssessmentUser::getProjectId).distinct().collect(Collectors.toList());
                Map<Long, BigDecimal> ratioMap = new HashMap<>();
                for (ProjectAssessmentUser r : ratioList) { ratioMap.put(r.getProjectId(), r.getRatio()); }

                QueryWrapper<ProjectExtend> extWrapper = new QueryWrapper<>();
                extWrapper.eq("ys_user_id", userId).or().eq("es_user_id", userId).or().eq("ss_user_id", userId);
                List<ProjectExtend> extList = projectExtendService.list(extWrapper);
                List<Long> reviewProjectIds = extList.stream().map(ProjectExtend::getProjectId).distinct().collect(Collectors.toList());
                reviewProjectIds.removeAll(techProjectIds);

                List<Long> allProjectIds = new ArrayList<>(techProjectIds);
                allProjectIds.addAll(reviewProjectIds);

                if (allProjectIds.isEmpty()) {
                    // 空sheet
                    WriteSheet sheetWriter = EasyExcel.writerSheet().sheetName(userName.length() > 31 ? userName.substring(0, 31) : userName).head(headList).build();
                    excelWriter.write(new ArrayList<>(), sheetWriter);
                    continue;
                }

                // 批量查询
                Map<Long, ProjectInfo> pInfoMap = projectInfoService.list(new QueryWrapper<ProjectInfo>().in("id", allProjectIds))
                        .stream().collect(Collectors.toMap(ProjectInfo::getId, p -> p));
                Map<Long, ProjectExtend> pExtMap = new HashMap<>();
                for (ProjectExtend pe : projectExtendService.list(new QueryWrapper<ProjectExtend>().in("project_id", allProjectIds))) {
                    pExtMap.put(pe.getProjectId(), pe);
                }
                Map<Long, BigDecimal> incomeMap = new HashMap<>();
                QueryWrapper<ProjectIncome> incW = new QueryWrapper<>();
                incW.in("project_id", allProjectIds);
                if (beginDay != null) incW.ge("receive_time", beginDay);
                if (endDay != null) incW.le("receive_time", endDay);
                for (ProjectIncome inc : projectIncomeService.list(incW)) {
                    incomeMap.merge(inc.getProjectId(), inc.getReceiveAmount() != null ? inc.getReceiveAmount() : BigDecimal.ZERO, BigDecimal::add);
                }
                Map<String, BigDecimal> feeMap = new HashMap<>();
                for (ProjectFeeInfo fee : projectFeeInfoService.list(new QueryWrapper<ProjectFeeInfo>().in("project_id", allProjectIds).eq("fee_status", 1))) {
                    feeMap.merge(fee.getProjectId() + "_" + fee.getFeeType(), fee.getFeeAmount() != null ? fee.getFeeAmount() : BigDecimal.ZERO, BigDecimal::add);
                }

                List<List<Object>> dataList = new ArrayList<>();
                int seq = 1;
                for (Long pid : allProjectIds) {
                    Map<String, Object> rowMap = buildStatRow(seq++, pid, userId, userName,
                            techProjectIds.contains(pid), ratioMap, pInfoMap, pExtMap, incomeMap, feeMap);
                    dataList.add(buildExcelRow(rowMap));
                }

                // Excel sheet名最长31字符
                String sheetName = userName.length() > 31 ? userName.substring(0, 31) : userName;
                WriteSheet sheetWriter = EasyExcel.writerSheet().sheetName(sheetName).head(headList).build();
                excelWriter.write(dataList, sheetWriter);
            }

            excelWriter.finish();

        } catch (IOException e) {
            try {
                response.reset();
                response.setContentType("application/json");
                response.setCharacterEncoding("utf-8");
                Map<String, Object> err = new HashMap<>();
                err.put("code", 500);
                err.put("msg", "导出失败：" + e.getMessage());
                response.getWriter().write(new ObjectMapper().writeValueAsString(err));
            } catch (IOException ex) { /* ignore */ }
        }
    }

    /**
     * 创建带边框的单元格样式策略
     */
    private HorizontalCellStyleStrategy createCellStyleStrategy() {
        // 表头样式
        WriteCellStyle headStyle = new WriteCellStyle();
        headStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        WriteFont headFont = new WriteFont();
        headFont.setFontHeightInPoints((short) 11);
        headFont.setBold(true);
        headStyle.setWriteFont(headFont);
        headStyle.setBorderLeft(BorderStyle.THIN);
        headStyle.setBorderRight(BorderStyle.THIN);
        headStyle.setBorderTop(BorderStyle.THIN);
        headStyle.setBorderBottom(BorderStyle.THIN);
        headStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        headStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headStyle.setWrapped(true);

        // 内容样式
        WriteCellStyle contentStyle = new WriteCellStyle();
        contentStyle.setBorderLeft(BorderStyle.THIN);
        contentStyle.setBorderRight(BorderStyle.THIN);
        contentStyle.setBorderTop(BorderStyle.THIN);
        contentStyle.setBorderBottom(BorderStyle.THIN);
        contentStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        contentStyle.setWrapped(true);

        return new HorizontalCellStyleStrategy(headStyle, contentStyle);
    }

    private List<List<String>> buildExcelHead() {
        String[] heads = {"序号","姓名","项目名称","委托单位","合同额","收款额","监测费","评审费","协作费",
                "公示公告费","材料费","差旅费","装订快递费","其他直接费用","税费","净收款额","净考核额",
                "审批或备案","过会审批","项目负责人","业务员","技术员","占比","一级审核","二级审核",
                "三级审核","报告类型","是否存档","进展情况","备注"};
        List<List<String>> headList = new ArrayList<>();
        for (String h : heads) {
            List<String> col = new ArrayList<>();
            col.add(h);
            headList.add(col);
        }
        return headList;
    }

    private List<Object> buildExcelRow(Map<String, Object> row) {
        List<Object> list = new ArrayList<>();
        list.add(row.get("seq"));
        list.add(row.get("userName"));
        list.add(row.get("projectName"));
        list.add(row.get("ownerCompany"));
        list.add(row.get("contractAmount"));
        list.add(row.get("receiveAmount"));
        list.add(row.get("fee1"));
        list.add(row.get("fee2"));
        list.add(row.get("fee3"));
        list.add(row.get("fee4"));
        list.add(row.get("fee5"));
        list.add(row.get("fee6"));
        list.add(row.get("fee7"));
        list.add(row.get("fee8"));
        list.add(row.get("taxFee"));
        list.add(row.get("netReceive"));
        list.add(row.get("netAssess"));
        list.add(row.get("isGovernmentApprove"));
        list.add(row.get("meetingApprove"));
        list.add(row.get("techUserName"));
        list.add(row.get("bisnessUserName"));
        list.add(row.get("techPersonName"));
        list.add(row.get("techRatio"));
        list.add(row.get("ysUserName"));
        list.add(row.get("esUserName"));
        list.add(row.get("ssUserName"));
        list.add(row.get("assessType"));
        list.add(row.get("isArchiveFinish"));
        list.add(row.get("progressInfo"));
        list.add(row.get("remark"));
        return list;
    }
}
