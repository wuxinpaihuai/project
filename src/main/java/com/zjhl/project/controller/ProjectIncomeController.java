package com.zjhl.project.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.zjhl.project.entity.ProjectExtend;
import com.zjhl.project.entity.ProjectIncome;
import com.zjhl.project.entity.ProjectIncomeFile;
import com.zjhl.project.entity.ProjectInfo;
import com.zjhl.project.entity.SignPayment;
import com.zjhl.project.service.ProjectExtendService;
import com.zjhl.project.service.ProjectIncomeFileService;
import com.zjhl.project.service.ProjectIncomeService;
import com.zjhl.project.service.ProjectInfoService;
import com.zjhl.project.service.SignPaymentService;

import cn.dev33.satoken.stp.StpUtil;

@RestController
@RequestMapping("/project/income")
public class ProjectIncomeController {

    private static final Logger log = LoggerFactory.getLogger(ProjectIncomeController.class);

    @Autowired
    private ProjectInfoService projectInfoService;

    @Autowired
    private ProjectExtendService projectExtendService;

    @Autowired
    private ProjectIncomeService projectIncomeService;

    @Autowired
    private ProjectIncomeFileService projectIncomeFileService;

    @Autowired
    private SignPaymentService signPaymentService;

    /**
     * 收款管理列表 - 查询已签约的项目
     */
    @GetMapping("/list")
    public Map<String, Object> incomeList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String projectName) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        // 1. 查询已签约的项目ID
        QueryWrapper<ProjectExtend> extendWrapper = new QueryWrapper<>();
        extendWrapper.eq("is_sign", 1);
        List<ProjectExtend> extendsList = projectExtendService.list(extendWrapper);
        List<Long> projectIds = extendsList.stream().map(ProjectExtend::getProjectId).collect(Collectors.toList());

        if (projectIds.isEmpty()) {
            result.put("code", 200);
            result.put("msg", "查询成功");
            result.put("total", 0);
            result.put("records", Collections.EMPTY_LIST);
            return result;
        }

        // 2. 分页查询项目信息
        QueryWrapper<ProjectInfo> wrapper = new QueryWrapper<>();
        wrapper.in("id", projectIds);
        if (projectName != null && !projectName.isEmpty()) {
            wrapper.like("project_name", projectName);
        }
        wrapper.orderByDesc("create_time");

        Page<ProjectInfo> page = new Page<>(pageNum, pageSize);
        Page<ProjectInfo> resultPage = projectInfoService.page(page, wrapper);

        // 3. 组装结果
        List<Map<String, Object>> records = new ArrayList<>();
        for (ProjectInfo info : resultPage.getRecords()) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", info.getId());
            row.put("projectName", info.getProjectName());
            row.put("ownerCompany", info.getOwnerCompany());
            row.put("projectNo", info.getProjectNo());
            row.put("bisnessUserName", info.getBisnessUserName());
            row.put("techUserName", info.getTechUserName());

            // 合同金额
            ProjectExtend ext = extendsList.stream()
                    .filter(e -> e.getProjectId().equals(info.getId()))
                    .findFirst().orElse(null);
            BigDecimal contractAmount = BigDecimal.ZERO;
            if (ext != null && ext.getContractAmount() != null) {
                contractAmount = ext.getContractAmount();
            }
            row.put("contractAmount", contractAmount);
            row.put("extendId", ext != null ? ext.getId() : null);

            // 已收款 = SUM(project_income.receive_amount where project_id=info.id)
            QueryWrapper<ProjectIncome> incomeWrapper = new QueryWrapper<>();
            incomeWrapper.eq("project_id", info.getId());
            List<ProjectIncome> incomes = projectIncomeService.list(incomeWrapper);
            BigDecimal receivedAmount = incomes.stream()
                    .map(ProjectIncome::getReceiveAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal unReceivedAmount = contractAmount.subtract(receivedAmount);
            if (unReceivedAmount.compareTo(BigDecimal.ZERO) < 0) {
                unReceivedAmount = BigDecimal.ZERO;
            }
            row.put("receivedAmount", receivedAmount);
            row.put("unReceivedAmount", unReceivedAmount);
            row.put("incomeComplete", contractAmount.compareTo(BigDecimal.ZERO) > 0
                    && receivedAmount.compareTo(contractAmount) >= 0);

            records.add(row);
        }

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("total", resultPage.getTotal());
        result.put("records", records);
        return result;
    }

    /**
     * 收款详情 - 按项目ID查看收款明细
     */
    @GetMapping("/detail")
    public Map<String, Object> incomeDetail(@RequestParam Long projectId) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        ProjectInfo info = projectInfoService.getById(projectId);
        if (info == null) {
            result.put("code", 404);
            result.put("msg", "项目不存在");
            return result;
        }

        Map<String, Object> projectInfo = new HashMap<>();
        projectInfo.put("id", info.getId());
        projectInfo.put("projectName", info.getProjectName());
        projectInfo.put("projectNo", info.getProjectNo());
        projectInfo.put("ownerCompany", info.getOwnerCompany());
        projectInfo.put("bisnessUserName", info.getBisnessUserName());
        projectInfo.put("techUserName", info.getTechUserName());

        // 合同金额
        QueryWrapper<ProjectExtend> extWrapper = new QueryWrapper<>();
        extWrapper.eq("project_id", projectId);
        ProjectExtend ext = projectExtendService.getOne(extWrapper);
        BigDecimal contractAmount = BigDecimal.ZERO;
        if (ext != null && ext.getContractAmount() != null) {
            contractAmount = ext.getContractAmount();
        }
        projectInfo.put("contractAmount", contractAmount);

        // 已收款
        QueryWrapper<ProjectIncome> incomeWrapper = new QueryWrapper<>();
        incomeWrapper.eq("project_id", projectId);
        incomeWrapper.orderByDesc("receive_time");
        List<ProjectIncome> incomes = projectIncomeService.list(incomeWrapper);

        BigDecimal receivedAmount = incomes.stream()
                .map(ProjectIncome::getReceiveAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal unReceivedAmount = contractAmount.subtract(receivedAmount);
        if (unReceivedAmount.compareTo(BigDecimal.ZERO) < 0) {
            unReceivedAmount = BigDecimal.ZERO;
        }

        // 收款比例
        BigDecimal receiveRate = BigDecimal.ZERO;
        if (contractAmount.compareTo(BigDecimal.ZERO) > 0) {
            receiveRate = receivedAmount.multiply(new BigDecimal("100"))
                    .divide(contractAmount, 2, BigDecimal.ROUND_HALF_UP);
        }

        projectInfo.put("receivedAmount", receivedAmount);
        projectInfo.put("unReceivedAmount", unReceivedAmount);
        projectInfo.put("receiveRate", receiveRate);

        // 收款明细列表（含附件和节点名称）
        List<Map<String, Object>> incomeList = new ArrayList<>();
        for (ProjectIncome income : incomes) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", income.getId());
            item.put("projectId", income.getProjectId());
            item.put("payerCompanyName", income.getPayerCompanyName());
            item.put("payType", income.getPayType());
            item.put("receiveAmount", income.getReceiveAmount());
            item.put("receiveTime", income.getReceiveTime() != null ? income.getReceiveTime().toString() : "");
            item.put("isMatchnode", income.getIsMatchnode());
            item.put("paymentNodeId", income.getPaymentNodeId());

            // 收款节点名称
            if (income.getPaymentNodeId() != null) {
                SignPayment sp = signPaymentService.getById(income.getPaymentNodeId());
                item.put("paymentNodeName", sp != null ? sp.getPaymentNode() : "");
            } else {
                item.put("paymentNodeName", "");
            }

            // 附件列表
            try {
                QueryWrapper<ProjectIncomeFile> fileWrapper = new QueryWrapper<>();
                fileWrapper.eq("income_id", income.getId());
                List<ProjectIncomeFile> files = projectIncomeFileService.list(fileWrapper);
                item.put("files", files);
            } catch (Exception e) {
                log.warn("查询收款附件失败, incomeId={}", income.getId(), e);
                item.put("files", Collections.EMPTY_LIST);
            }

            incomeList.add(item);
        }

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("projectInfo", projectInfo);
        result.put("incomeList", incomeList);
        return result;
    }

    /**
     * 新增收款记录
     */
    @PostMapping("/add")
    public Map<String, Object> addIncome(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        ProjectIncome income = new ProjectIncome();
        income.setProjectId(Long.parseLong(params.get("projectId").toString()));
        income.setReceiveAmount(new BigDecimal(params.get("receiveAmount").toString()));
        income.setReceiveTime(params.get("receiveTime") != null && !params.get("receiveTime").toString().isEmpty()
                ? LocalDate.parse(params.get("receiveTime").toString()) : null);
        income.setPayerCompanyName(params.get("payerCompanyName") != null ? (String) params.get("payerCompanyName") : null);
        income.setPayType(params.get("payType") != null ? Integer.parseInt(params.get("payType").toString()) : null);
        income.setIsMatchnode(params.get("isMatchnode") != null ? Integer.parseInt(params.get("isMatchnode").toString()) : 0);
        income.setPaymentNodeId(params.get("paymentNodeId") != null && !params.get("paymentNodeId").toString().isEmpty()
                ? Long.parseLong(params.get("paymentNodeId").toString()) : null);
        income.setCreateTime(LocalDateTime.now());

        boolean success = projectIncomeService.save(income);
        if (success) {
            // 保存附件
            saveIncomeFiles(income.getId(), params);

            // 更新project_extend的is_receive_money状态
            updateReceiveMoneyStatus(income.getProjectId());

            result.put("code", 200);
            result.put("msg", "新增成功");
            result.put("id", income.getId());
        } else {
            result.put("code", 500);
            result.put("msg", "新增失败");
        }
        return result;
    }

    /**
     * 编辑收款记录
     */
    @PutMapping("/update")
    public Map<String, Object> updateIncome(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        Long id = Long.parseLong(params.get("id").toString());
        ProjectIncome income = new ProjectIncome();
        income.setId(id);
        income.setReceiveAmount(new BigDecimal(params.get("receiveAmount").toString()));
        income.setReceiveTime(params.get("receiveTime") != null && !params.get("receiveTime").toString().isEmpty()
                ? LocalDate.parse(params.get("receiveTime").toString()) : null);
        income.setPayerCompanyName(params.get("payerCompanyName") != null ? (String) params.get("payerCompanyName") : null);
        income.setPayType(params.get("payType") != null ? Integer.parseInt(params.get("payType").toString()) : null);
        income.setIsMatchnode(params.get("isMatchnode") != null ? Integer.parseInt(params.get("isMatchnode").toString()) : 0);
        income.setPaymentNodeId(params.get("paymentNodeId") != null && !params.get("paymentNodeId").toString().isEmpty()
                ? Long.parseLong(params.get("paymentNodeId").toString()) : null);

        boolean success = projectIncomeService.updateById(income);
        if (success) {
            // 更新附件：先删后增
            try {
                QueryWrapper<ProjectIncomeFile> delWrapper = new QueryWrapper<>();
                delWrapper.eq("income_id", id);
                projectIncomeFileService.remove(delWrapper);
            } catch (Exception e) {
                log.warn("删除旧附件失败, incomeId={}", id, e);
            }

            saveIncomeFiles(id, params);

            // 获取projectId更新收款状态
            ProjectIncome updated = projectIncomeService.getById(id);
            if (updated != null) {
                updateReceiveMoneyStatus(updated.getProjectId());
            }

            result.put("code", 200);
            result.put("msg", "编辑成功");
        } else {
            result.put("code", 500);
            result.put("msg", "编辑失败");
        }
        return result;
    }

    /**
     * 删除收款记录
     */
    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteIncome(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        // 先获取projectId
        ProjectIncome income = projectIncomeService.getById(id);
        Long projectId = income != null ? income.getProjectId() : null;

        // 删除附件
        try {
            QueryWrapper<ProjectIncomeFile> fileWrapper = new QueryWrapper<>();
            fileWrapper.eq("income_id", id);
            projectIncomeFileService.remove(fileWrapper);
        } catch (Exception e) {
            log.warn("删除收款附件失败, incomeId={}", id, e);
        }

        boolean success = projectIncomeService.removeById(id);
        if (success && projectId != null) {
            updateReceiveMoneyStatus(projectId);
            result.put("code", 200);
            result.put("msg", "删除成功");
        } else {
            result.put("code", 500);
            result.put("msg", "删除失败");
        }
        return result;
    }

    /**
     * 获取项目的收款节点下拉列表
     */
    @GetMapping("/paymentNodes/{projectId}")
    public Map<String, Object> paymentNodes(@PathVariable Long projectId) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        QueryWrapper<SignPayment> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId);
        wrapper.orderByAsc("create_time");
        List<SignPayment> nodes = signPaymentService.list(wrapper);

        List<Map<String, Object>> nodeList = new ArrayList<>();
        for (SignPayment sp : nodes) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", sp.getId());
            map.put("paymentNode", sp.getPaymentNode());
            map.put("receiveAmount", sp.getReceiveAmount());
            map.put("expectPayDate", sp.getExpectPayDate() != null ? sp.getExpectPayDate().toString() : "");
            // 下拉显示：收款节点名称_收款金额(元)_预计收款日期
            String label = sp.getPaymentNode();
            if (sp.getReceiveAmount() != null) {
                label += "_" + sp.getReceiveAmount().toPlainString() + "(元)";
            }
            if (sp.getExpectPayDate() != null) {
                label += "_" + sp.getExpectPayDate().toString();
            }
            map.put("label", label);
            nodeList.add(map);
        }

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", nodeList);
        return result;
    }

    /**
     * 获取单条收款记录详情（用于编辑回显）
     */
    @GetMapping("/get/{id}")
    public Map<String, Object> getIncome(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        ProjectIncome income = projectIncomeService.getById(id);
        if (income == null) {
            result.put("code", 404);
            result.put("msg", "记录不存在");
            return result;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", income.getId());
        data.put("projectId", income.getProjectId());
        data.put("payerCompanyName", income.getPayerCompanyName());
        data.put("payType", income.getPayType());
        data.put("receiveAmount", income.getReceiveAmount());
        data.put("receiveTime", income.getReceiveTime() != null ? income.getReceiveTime().toString() : "");
        data.put("isMatchnode", income.getIsMatchnode());
        data.put("paymentNodeId", income.getPaymentNodeId());

        // 附件
        try {
            QueryWrapper<ProjectIncomeFile> fileWrapper = new QueryWrapper<>();
            fileWrapper.eq("income_id", id);
            List<ProjectIncomeFile> files = projectIncomeFileService.list(fileWrapper);
            data.put("files", files);
        } catch (Exception e) {
            log.warn("查询收款附件失败, incomeId={}", id, e);
            data.put("files", Collections.EMPTY_LIST);
        }

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", data);
        return result;
    }

    /**
     * 保存收款附件（抽取公共方法）
     */
    private void saveIncomeFiles(Long incomeId, Map<String, Object> params) {
        if (params.get("files") == null) return;
        try {
            List<Map<String, Object>> files = (List<Map<String, Object>>) params.get("files");
            for (Map<String, Object> f : files) {
                ProjectIncomeFile pif = new ProjectIncomeFile();
                pif.setIncomeId(incomeId);
                pif.setFileName(f.get("fileName") != null ? f.get("fileName").toString() : null);
                pif.setFilePath(f.get("filePath") != null ? f.get("filePath").toString() : null);
                pif.setCreateTime(LocalDateTime.now());
                projectIncomeFileService.save(pif);
            }
        } catch (Exception e) {
            log.error("保存收款附件失败, incomeId={}", incomeId, e);
        }
    }

    /**
     * 更新project_extend的is_receive_money状态
     */
    private void updateReceiveMoneyStatus(Long projectId) {
        QueryWrapper<ProjectExtend> extWrapper = new QueryWrapper<>();
        extWrapper.eq("project_id", projectId);
        ProjectExtend ext = projectExtendService.getOne(extWrapper);
        if (ext != null && ext.getContractAmount() != null && ext.getContractAmount().compareTo(BigDecimal.ZERO) > 0) {
            QueryWrapper<ProjectIncome> incWrapper = new QueryWrapper<>();
            incWrapper.eq("project_id", projectId);
            List<ProjectIncome> incomes = projectIncomeService.list(incWrapper);
            BigDecimal total = incomes.stream()
                    .map(ProjectIncome::getReceiveAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            ProjectExtend update = new ProjectExtend();
            update.setId(ext.getId());
            update.setIsReceiveMoney(total.compareTo(ext.getContractAmount()) >= 0 ? 1 : 0);
            projectExtendService.updateById(update);
        }
    }
}
