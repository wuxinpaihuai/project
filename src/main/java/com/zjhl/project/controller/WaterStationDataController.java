package com.zjhl.project.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zjhl.project.entity.WaterStationData;
import com.zjhl.project.service.WaterStationDataService;

import cn.dev33.satoken.stp.StpUtil;

@RestController
@RequestMapping("/water/data")
public class WaterStationDataController {

    @Autowired
    private WaterStationDataService waterStationDataService;

    /**
     * 站点水情数据列表（分页 + 条件查询）
     * 默认查询当天数据（由前端传入 dataDay 默认当天）
     */
    @GetMapping("/list")
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String stationName,
            @RequestParam(required = false) String stationCode,
            @RequestParam(required = false) String dataDay,
            @RequestParam(required = false) String city) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        QueryWrapper<WaterStationData> wrapper = buildQuery(stationName, stationCode, dataDay, city);
        wrapper.orderByDesc("report_time").orderByDesc("id");

        Page<WaterStationData> page = new Page<>(pageNum, pageSize);
        Page<WaterStationData> resultPage = waterStationDataService.page(page, wrapper);

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("total", resultPage.getTotal());
        result.put("records", resultPage.getRecords());
        return result;
    }

    /**
     * 构造查询条件（列表与下载共用）
     */
    private QueryWrapper<WaterStationData> buildQuery(String stationName, String stationCode,
                                                      String dataDay, String city) {
        QueryWrapper<WaterStationData> wrapper = new QueryWrapper<>();
        if (stationName != null && !stationName.isEmpty()) {
            wrapper.like("station_name", stationName);
        }
        if (stationCode != null && !stationCode.isEmpty()) {
            wrapper.like("station_code", stationCode);
        }
        if (dataDay != null && !dataDay.isEmpty()) {
            wrapper.eq("data_day", dataDay);
        }
        if (city != null && !city.isEmpty()) {
            wrapper.eq("city", city);
        }
        return wrapper;
    }

    /**
     * 下载站点水情数据（按当前查询条件导出）
     * 表头：序号、市、区县、站名、站号、水位（m）、汛限水位（m）、是否超汛限、经度、纬度、上报时间
     */
    @GetMapping("/download")
    public void download(
            @RequestParam(required = false) String stationName,
            @RequestParam(required = false) String stationCode,
            @RequestParam(required = false) String dataDay,
            @RequestParam(required = false) String city,
            HttpServletResponse response) {
        try {
            QueryWrapper<WaterStationData> wrapper = buildQuery(stationName, stationCode, dataDay, city);
            wrapper.orderByDesc("report_time").orderByDesc("id");
            List<WaterStationData> list = waterStationDataService.list(wrapper);

            // 表头
            String[] heads = {"序号", "市", "区县", "站名", "站号", "水位（m）", "汛限水位（m）",
                    "是否超汛限", "经度", "纬度", "上报时间"};
            List<List<String>> headList = new ArrayList<>();
            for (String h : heads) {
                List<String> col = new ArrayList<>();
                col.add(h);
                headList.add(col);
            }

            // 数据
            List<List<Object>> dataList = new ArrayList<>();
            int seq = 1;
            for (WaterStationData d : list) {
                List<Object> row = new ArrayList<>();
                row.add(seq++);
                row.add(nvl(d.getCity()));
                row.add(nvl(d.getCounty()));
                row.add(nvl(d.getStationName()));
                row.add(nvl(d.getStationCode()));
                row.add(decimalToStr(d.getWaterLevel(), 3));
                row.add(decimalToStr(d.getWaterLimitLevel(), 3));
                row.add(d.getIsOut() != null && d.getIsOut() == 1 ? "是" : "否");
                row.add(decimalToStr(d.getLongitude(), 6));
                row.add(decimalToStr(d.getLatitude(), 6));
                row.add(formatDateTime(d.getReportTime()));
                dataList.add(row);
            }

            String fileName = URLEncoder.encode("站点水情数据", StandardCharsets.UTF_8.toString());
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

            EasyExcel.write(response.getOutputStream())
                    .head(headList)
                    .autoCloseStream(true)
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .registerWriteHandler(createCellStyleStrategy())
                    .sheet("站点水情数据")
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

    private String nvl(String v) {
        return v != null ? v : "";
    }

    /**
     * Date → 字符串（yyyy-MM-dd HH:mm:ss，null 返回空串）
     */
    private String formatDateTime(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    /**
     * BigDecimal → 字符串（保留指定小数位，null 返回空串，避免导出成科学计数法）
     */
    private String decimalToStr(BigDecimal v, int scale) {
        if (v == null) {
            return "";
        }
        return v.setScale(scale, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * 导出样式：表头灰底加粗居中 + 全表细边框
     */
    private HorizontalCellStyleStrategy createCellStyleStrategy() {
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

        WriteCellStyle contentStyle = new WriteCellStyle();
        contentStyle.setBorderLeft(BorderStyle.THIN);
        contentStyle.setBorderRight(BorderStyle.THIN);
        contentStyle.setBorderTop(BorderStyle.THIN);
        contentStyle.setBorderBottom(BorderStyle.THIN);
        contentStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        return new HorizontalCellStyleStrategy(headStyle, contentStyle);
    }
}
