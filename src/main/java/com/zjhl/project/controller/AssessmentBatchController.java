package com.zjhl.project.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

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

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zjhl.project.entity.AssessmentBatch;
import com.zjhl.project.entity.SysUser;
import com.zjhl.project.service.AssessmentBatchService;
import com.zjhl.project.service.SysUserService;

import cn.dev33.satoken.stp.StpUtil;

@RestController
@RequestMapping("/assessment/batch")
public class AssessmentBatchController {

    @Autowired
    private AssessmentBatchService assessmentBatchService;

    @Autowired
    private SysUserService sysUserService;

    /** 考核状态映射 */
    private static final Map<Integer, String> ASSESS_STATUS_MAP = new HashMap<>();
    static {
        ASSESS_STATUS_MAP.put(1, "未开始");
        ASSESS_STATUS_MAP.put(2, "统计中");
        ASSESS_STATUS_MAP.put(3, "已完成");
        ASSESS_STATUS_MAP.put(4, "统计失败");
    }

    /** 职位映射 */
    private static final Map<String, String> POSITION_MAP = new HashMap<>();
    static {
        POSITION_MAP.put("0", "董事长");
        POSITION_MAP.put("1", "总裁");
        POSITION_MAP.put("2", "副总裁");
        POSITION_MAP.put("3", "处长");
        POSITION_MAP.put("4", "副处长");
        POSITION_MAP.put("5", "技术学术委员会主任");
        POSITION_MAP.put("6", "副主任");
        POSITION_MAP.put("7", "处长助理");
        POSITION_MAP.put("8", "普通员工");
    }

    /**
     * 考核批次列表
     */
    @GetMapping("/list")
    public Map<String, Object> batchList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String batchName,
            @RequestParam(required = false) Integer assessStatus) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        QueryWrapper<AssessmentBatch> wrapper = new QueryWrapper<>();
        if (batchName != null && !batchName.isEmpty()) {
            wrapper.like("batch_name", batchName);
        }
        if (assessStatus != null) {
            wrapper.eq("assess_status", assessStatus);
        }
        wrapper.orderByDesc("create_time");

        Page<AssessmentBatch> page = new Page<>(pageNum, pageSize);
        Page<AssessmentBatch> resultPage = assessmentBatchService.page(page, wrapper);

        List<Map<String, Object>> records = new ArrayList<>();
        for (AssessmentBatch batch : resultPage.getRecords()) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", batch.getId());
            row.put("batchName", batch.getBatchName());
            row.put("assessBeginDay", batch.getAssessBeginDay() != null ? batch.getAssessBeginDay().toString() : "");
            row.put("assessEndDay", batch.getAssessEndDay() != null ? batch.getAssessEndDay().toString() : "");
            row.put("assessStatus", batch.getAssessStatus());
            row.put("assessStatusName", ASSESS_STATUS_MAP.getOrDefault(batch.getAssessStatus(), "未知"));
            row.put("remark", batch.getRemark() != null ? batch.getRemark() : "");
            row.put("createTime", batch.getCreateTime() != null ? batch.getCreateTime().toString() : "");
            records.add(row);
        }

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("total", resultPage.getTotal());
        result.put("records", records);
        return result;
    }

    /**
     * 新增考核批次
     */
    @PostMapping("/add")
    public Map<String, Object> addBatch(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        String batchName = params.get("batchName") != null ? params.get("batchName").toString() : null;
        if (batchName == null || batchName.isEmpty()) {
            result.put("code", 400);
            result.put("msg", "批次名称不能为空");
            return result;
        }

        // 唯一性校验
        QueryWrapper<AssessmentBatch> existWrapper = new QueryWrapper<>();
        existWrapper.eq("batch_name", batchName);
        long count = assessmentBatchService.count(existWrapper);
        if (count > 0) {
            result.put("code", 400);
            result.put("msg", "批次名称已存在，请更换");
            return result;
        }

        AssessmentBatch batch = new AssessmentBatch();
        batch.setBatchName(batchName);
        batch.setAssessBeginDay(params.get("assessBeginDay") != null ? java.time.LocalDate.parse(params.get("assessBeginDay").toString()) : null);
        batch.setAssessEndDay(params.get("assessEndDay") != null ? java.time.LocalDate.parse(params.get("assessEndDay").toString()) : null);
        batch.setRemark(params.get("remark") != null ? params.get("remark").toString() : null);
        batch.setAssessStatus(params.get("assessStatus") != null ? Integer.parseInt(params.get("assessStatus").toString()) : 1);
        batch.setCreateTime(LocalDateTime.now());

        boolean success = assessmentBatchService.save(batch);
        if (success) {
            result.put("code", 200);
            result.put("msg", "新增成功");
            result.put("id", batch.getId());
        } else {
            result.put("code", 500);
            result.put("msg", "新增失败");
        }
        return result;
    }

    /**
     * 编辑考核批次
     */
    @PutMapping("/update")
    public Map<String, Object> updateBatch(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        Long id = Long.parseLong(params.get("id").toString());

        AssessmentBatch existing = assessmentBatchService.getById(id);
        if (existing == null) {
            result.put("code", 404);
            result.put("msg", "记录不存在");
            return result;
        }

        // 只有未开始(1)和统计失败(4)可以编辑
        if (existing.getAssessStatus() != null && existing.getAssessStatus() != 1 && existing.getAssessStatus() != 4) {
            result.put("code", 400);
            result.put("msg", "当前状态不允许编辑");
            return result;
        }

        String batchName = params.get("batchName") != null ? params.get("batchName").toString() : null;
        if (batchName != null && !batchName.isEmpty()) {
            // 唯一性校验（排除自身）
            QueryWrapper<AssessmentBatch> existWrapper = new QueryWrapper<>();
            existWrapper.eq("batch_name", batchName);
            existWrapper.ne("id", id);
            long count = assessmentBatchService.count(existWrapper);
            if (count > 0) {
                result.put("code", 400);
                result.put("msg", "批次名称已存在，请更换");
                return result;
            }
        }

        AssessmentBatch batch = new AssessmentBatch();
        batch.setId(id);
        if (batchName != null) batch.setBatchName(batchName);
        if (params.get("assessBeginDay") != null) batch.setAssessBeginDay(java.time.LocalDate.parse(params.get("assessBeginDay").toString()));
        if (params.get("assessEndDay") != null) batch.setAssessEndDay(java.time.LocalDate.parse(params.get("assessEndDay").toString()));
        if (params.get("remark") != null) batch.setRemark(params.get("remark").toString());
        if (params.get("assessStatus") != null) batch.setAssessStatus(Integer.parseInt(params.get("assessStatus").toString()));

        boolean success = assessmentBatchService.updateById(batch);
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
     * 删除考核批次
     */
    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteBatch(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        boolean success = assessmentBatchService.removeById(id);
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
     * 获取单条批次详情（用于编辑回显）
     */
    @GetMapping("/get/{id}")
    public Map<String, Object> getBatch(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        AssessmentBatch batch = assessmentBatchService.getById(id);
        if (batch == null) {
            result.put("code", 404);
            result.put("msg", "记录不存在");
            return result;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", batch.getId());
        data.put("batchName", batch.getBatchName());
        data.put("assessBeginDay", batch.getAssessBeginDay() != null ? batch.getAssessBeginDay().toString() : "");
        data.put("assessEndDay", batch.getAssessEndDay() != null ? batch.getAssessEndDay().toString() : "");
        data.put("remark", batch.getRemark() != null ? batch.getRemark() : "");
        data.put("assessStatus", batch.getAssessStatus());

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", data);
        return result;
    }

    /**
     * 下载考核人员表格（导出is_assess=1的用户）
     */
    @GetMapping("/downloadAssessUsers")
    public void downloadAssessUsers(HttpServletResponse response) {
        try {
            // 查询is_assess=1的用户
            QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
            wrapper.eq("is_assess", 1);
            wrapper.eq("status", 1);
            List<SysUser> users = sysUserService.list(wrapper);

            // 构造导出数据
            List<List<String>> dataList = new ArrayList<>();
            for (SysUser user : users) {
                List<String> row = new ArrayList<>();
                row.add(user.getUsername() != null ? user.getUsername() : "");
                row.add(user.getRealName() != null ? user.getRealName() : "");
                row.add(POSITION_MAP.getOrDefault(user.getSysPosition(), user.getSysPosition() != null ? user.getSysPosition() : ""));
                row.add(user.getPhone() != null ? user.getPhone() : "");
                dataList.add(row);
            }

            // 表头
            List<List<String>> headList = new ArrayList<>();
            List<String> head1 = new ArrayList<>(); head1.add("账号"); headList.add(head1);
            List<String> head2 = new ArrayList<>(); head2.add("姓名"); headList.add(head2);
            List<String> head3 = new ArrayList<>(); head3.add("职位"); headList.add(head3);
            List<String> head4 = new ArrayList<>(); head4.add("电话"); headList.add(head4);

            // 设置响应头
            String fileName = URLEncoder.encode("考核人员表", StandardCharsets.UTF_8.toString());
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

            // 写出Excel
            EasyExcel.write(response.getOutputStream())
                    .head(headList)
                    .autoCloseStream(true)
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .sheet("考核人员")
                    .doWrite(dataList);

        } catch (IOException e) {
            try {
                response.reset();
                response.setContentType("application/json");
                response.setCharacterEncoding("utf-8");
                Map<String, Object> err = new HashMap<>();
                err.put("code", 500);
                err.put("msg", "导出失败：" + e.getMessage());
                response.getWriter().write(new ObjectMapper().writeValueAsString(err));
            } catch (IOException ex) {
                // ignore
            }
        }
    }
}
