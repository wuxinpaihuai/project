package com.zjhl.project.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import org.springframework.web.bind.annotation.PostMapping;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zjhl.project.entity.WaterStationInfo;
import com.zjhl.project.service.WaterStationInfoService;

import cn.dev33.satoken.stp.StpUtil;

@RestController
@RequestMapping("/water/station")
public class WaterStationInfoController {

    @Autowired
    private WaterStationInfoService waterStationInfoService;

    /** 批量提交大小 */
    private static final int BATCH_SIZE = 500;

    /** 站点类型映射 */
    private static final Map<String, String> STATION_TYPE_MAP = new HashMap<>();
    static {
        STATION_TYPE_MAP.put("RR", "雨量站");
        STATION_TYPE_MAP.put("ZZ", "水位站");
        STATION_TYPE_MAP.put("ZQ", "流量站");
        STATION_TYPE_MAP.put("TT", "潮位站");
        STATION_TYPE_MAP.put("DD", "堰闸站");
    }

    /**
     * 站点列表（分页 + 条件查询）
     */
    @GetMapping("/list")
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String stationName,
            @RequestParam(required = false) String stationCode,
            @RequestParam(required = false) String stationType,
            @RequestParam(required = false) String city) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        QueryWrapper<WaterStationInfo> wrapper = buildQuery(stationName, stationCode, stationType, city);
        wrapper.orderByDesc("create_time");

        Page<WaterStationInfo> page = new Page<>(pageNum, pageSize);
        Page<WaterStationInfo> resultPage = waterStationInfoService.page(page, wrapper);

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("total", resultPage.getTotal());
        result.put("records", resultPage.getRecords());
        return result;
    }

    /**
     * 构造查询条件（列表与下载共用）
     */
    private QueryWrapper<WaterStationInfo> buildQuery(String stationName, String stationCode,
                                                     String stationType, String city) {
        QueryWrapper<WaterStationInfo> wrapper = new QueryWrapper<>();
        if (stationName != null && !stationName.isEmpty()) {
            wrapper.like("station_name", stationName);
        }
        if (stationCode != null && !stationCode.isEmpty()) {
            wrapper.like("station_code", stationCode);
        }
        if (stationType != null && !stationType.isEmpty()) {
            wrapper.eq("station_type", stationType);
        }
        if (city != null && !city.isEmpty()) {
            wrapper.eq("city", city);
        }
        return wrapper;
    }

    /**
     * 批量同步站点
     * 读取 /file/zhandian.txt 中的 qt 数组，以「站点编号」为唯一标识：
     *   已存在 → 更新（站点名称、站点类型、经度、纬度）
     *   不存在 → 新增
     * 分批提交保证性能，并统计成功/失败数量。
     */
    @PostMapping("/sync")
    public Map<String, Object> sync() {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        long startMs = System.currentTimeMillis();
        int insertOk = 0, insertFail = 0, updateOk = 0, updateFail = 0;
        int skipCount = 0;

        try {
            // ---------- 1. 读取并解析 JSON 文件 ----------
            JsonNode root;
            try (InputStream is = openStationFile()) {
                root = new ObjectMapper().readTree(is);
            }
            JsonNode qt = root.path("qt");
            if (qt.isMissingNode() || !qt.isArray() || qt.size() == 0) {
                result.put("code", 400);
                result.put("msg", "数据文件格式不正确：未找到有效的 qt 数组");
                return result;
            }

            // ---------- 2. 解析出待处理数据 ----------
            List<StationItem> items = new ArrayList<>();
            for (JsonNode node : qt) {
                String code = readText(node, "zh");
                if (code.isEmpty()) {
                    skipCount++;   // 无站点编号，无法定位唯一记录
                    continue;
                }
                StationItem item = new StationItem();
                item.code = code;
                item.name = readText(node, "zm");
                item.type = readText(node, "zl");
                item.longitude = readText(node, "jd");
                item.latitude = readText(node, "wd");
                items.add(item);
            }

            // ---------- 3. 一次性查出已有站点编号，避免逐条查库 ----------
            Map<String, Long> existMap = new HashMap<>();
            List<WaterStationInfo> exists = waterStationInfoService.list(
                    new QueryWrapper<WaterStationInfo>().select("id", "station_code"));
            for (WaterStationInfo w : exists) {
                if (w.getStationCode() != null) {
                    existMap.put(w.getStationCode(), w.getId());
                }
            }

            // ---------- 4. 分离「新增」与「更新」 ----------
            List<WaterStationInfo> toInsert = new ArrayList<>();
            List<WaterStationInfo> toUpdate = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            for (StationItem item : items) {
                WaterStationInfo w = new WaterStationInfo();
                w.setStationCode(item.code);
                w.setStationName(item.name);
                w.setStationType(item.type);
                w.setLongitude(item.longitude);
                w.setLatitude(item.latitude);

                Long existId = existMap.get(item.code);
                if (existId != null) {
                    w.setId(existId);
                    toUpdate.add(w);
                } else {
                    w.setCreateTime(now);
                    toInsert.add(w);
                }
            }

            // ---------- 5. 分批执行（失败自动降级为逐条，便于精确计数） ----------
            int[] insRes = batchSave(toInsert);
            insertOk = insRes[0];
            insertFail = insRes[1];

            int[] updRes = batchUpdate(toUpdate);
            updateOk = updRes[0];
            updateFail = updRes[1];

        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "同步失败：" + e.getMessage());
            return result;
        }

        int successCount = insertOk + updateOk;
        int failCount = insertFail + updateFail + skipCount;

        result.put("code", 200);
        result.put("msg", "同步完成");
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("insertCount", insertOk);
        result.put("updateCount", updateOk);
        result.put("skipCount", skipCount);
        result.put("costMs", System.currentTimeMillis() - startMs);
        return result;
    }

    /**
     * 批量新增：分批提交，整批异常时降级逐条，保证成功/失败计数精确
     */
    private int[] batchSave(List<WaterStationInfo> list) {
        int ok = 0, fail = 0;
        for (int i = 0; i < list.size(); i += BATCH_SIZE) {
            List<WaterStationInfo> sub = list.subList(i, Math.min(i + BATCH_SIZE, list.size()));
            try {
                waterStationInfoService.saveBatch(sub);
                ok += sub.size();
            } catch (Exception e) {
                for (WaterStationInfo w : sub) {
                    try {
                        waterStationInfoService.save(w);
                        ok++;
                    } catch (Exception ex) {
                        fail++;
                    }
                }
            }
        }
        return new int[]{ok, fail};
    }

    /**
     * 批量更新：分批提交，整批异常时降级逐条
     * 只更新非 null 字段（站点名称、站点类型、经度、纬度），update_time 由数据库自动维护
     */
    private int[] batchUpdate(List<WaterStationInfo> list) {
        int ok = 0, fail = 0;
        for (int i = 0; i < list.size(); i += BATCH_SIZE) {
            List<WaterStationInfo> sub = list.subList(i, Math.min(i + BATCH_SIZE, list.size()));
            try {
                waterStationInfoService.updateBatchById(sub);
                ok += sub.size();
            } catch (Exception e) {
                for (WaterStationInfo w : sub) {
                    try {
                        waterStationInfoService.updateById(w);
                        ok++;
                    } catch (Exception ex) {
                        fail++;
                    }
                }
            }
        }
        return new int[]{ok, fail};
    }

    /**
     * 读取站点数据文件，依次尝试 classpath 与磁盘路径
     */
    private InputStream openStationFile() throws IOException {
        ClassLoader cl = getClass().getClassLoader();
        String[] classpathPaths = {"static/file/zhandian.txt", "file/zhandian.txt"};
        for (String p : classpathPaths) {
            InputStream is = cl.getResourceAsStream(p);
            if (is != null) {
                return is;
            }
        }
        String[] filePaths = {
                "/file/zhandian.txt",
                System.getProperty("user.dir") + File.separator + "file" + File.separator + "zhandian.txt"
        };
        for (String p : filePaths) {
            File f = new File(p);
            if (f.exists() && f.isFile()) {
                return new FileInputStream(f);
            }
        }
        throw new IOException("未找到站点数据文件，请确认已放置到 classpath:static/file/zhandian.txt 或磁盘 /file/zhandian.txt");
    }

    /**
     * 安全读取 JSON 节点文本值
     */
    private String readText(JsonNode node, String field) {
        JsonNode v = node.path(field);
        if (v.isMissingNode() || v.isNull()) {
            return "";
        }
        return v.asText("").trim();
    }

    /** 同步用的数据载体 */
    private static class StationItem {
        String code;
        String name;
        String type;
        String longitude;
        String latitude;
    }

    /**
     * 下载站点（按当前查询条件导出 Excel）
     */
    @GetMapping("/download")
    public void download(
            @RequestParam(required = false) String stationName,
            @RequestParam(required = false) String stationCode,
            @RequestParam(required = false) String stationType,
            @RequestParam(required = false) String city,
            HttpServletResponse response) {
        try {
            QueryWrapper<WaterStationInfo> wrapper = buildQuery(stationName, stationCode, stationType, city);
            wrapper.orderByDesc("create_time");
            List<WaterStationInfo> list = waterStationInfoService.list(wrapper);

            // 表头
            String[] heads = {"站点名称", "站点编号", "站点类型", "经度", "纬度", "地市", "区县"};
            List<List<String>> headList = new ArrayList<>();
            for (String h : heads) {
                List<String> col = new ArrayList<>();
                col.add(h);
                headList.add(col);
            }

            // 数据
            List<List<String>> dataList = new ArrayList<>();
            for (WaterStationInfo w : list) {
                List<String> row = new ArrayList<>();
                row.add(nvl(w.getStationName()));
                row.add(nvl(w.getStationCode()));
                row.add(STATION_TYPE_MAP.getOrDefault(w.getStationType(), nvl(w.getStationType())));
                row.add(nvl(w.getLongitude()));
                row.add(nvl(w.getLatitude()));
                row.add(nvl(w.getCity()));
                row.add(nvl(w.getCounty()));
                dataList.add(row);
            }

            String fileName = URLEncoder.encode("水利站点信息", StandardCharsets.UTF_8.toString());
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

            EasyExcel.write(response.getOutputStream())
                    .head(headList)
                    .autoCloseStream(true)
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .registerWriteHandler(createCellStyleStrategy())
                    .sheet("水利站点")
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
