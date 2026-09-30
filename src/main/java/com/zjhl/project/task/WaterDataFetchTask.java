package com.zjhl.project.task;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zjhl.project.entity.WaterStationData;
import com.zjhl.project.service.WaterStationDataService;
import com.zjhl.project.util.DateLocalUtil;

/**
 * 浙江实时水情数据每日采集任务。
 * 数据来源：浙江省水利厅实时水情页面背后的数据接口（全省约 1.1 万个站点）。
 */
@Component
public class WaterDataFetchTask {

    private static final Logger log = LoggerFactory.getLogger(WaterDataFetchTask.class);

    /** 数据接口（参数与页面一致：全省、全部水库类型、全部报讯等级） */
    private static final String API_URL =
            "https://sqfb.slt.zj.gov.cn:30050/rest/water/getNewDataList";

    /** MyBatis-Plus saveBatch 每批条数 */
    private static final int BATCH_SIZE = 500;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private WaterStationDataService waterStationDataService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 每天 17:00 执行一次。
     * 如需调整时间，改 application.yml 中 water.fetch.cron 即可。
     */
    @Scheduled(cron = "${water.fetch.cron:0 0 17 * * ?}")
    public void fetchAndSave() {
        log.info("===== 水情数据采集任务开始 =====");
        try {
            // 幂等处理：先删除当天旧数据，再抓取同步，避免重复入库
            SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date today = dayFormat.parse(dayFormat.format(new Date()));
            int deleted = waterStationDataService.deleteByDataDay(today);
            if (deleted > 0) {
                log.info("已删除 {} 的旧数据 {} 条", today, deleted);
            }

            String url = API_URL
                    + "?areaFlag=1"
                    + "&sss=" + URLEncoder.encode("全部", "UTF-8")
                    + "&ssx="
                    + "&zl=RR,ZZ,ZQ,TT,DD"
                    + "&sklx=1,9,2,3,4,5"
                    + "&ly="
                    + "&sfcj=0"
                    + "&bxdj=1,2,3,4,5"
                    + "&zm="
                    + "&cjly="
                    + "&bx=0";

            // 注意：url 中的中文参数已手动 URLEncoder 编码，
            // 必须传 URI 对象，否则 RestTemplate 会当作 URI 模板再次编码（% 变成 %25），
            // 导致服务端收到乱码参数而返回空数据
            String json = restTemplate.getForObject(new URI(url), String.class);
            if (json == null || json.isEmpty()) {
                log.warn("接口返回为空，本次任务结束");
                return;
            }

            JsonNode root = objectMapper.readTree(json);
            JsonNode qt = root.get("qt");
            if (qt == null || !qt.isArray() || qt.size() == 0) {
                log.warn("接口返回无站点数据，本次任务结束");
                return;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

            List<WaterStationData> list = new ArrayList<>(qt.size());
            for (JsonNode node : qt) {
                WaterStationData d = new WaterStationData();
                d.setStationCode(text(node, "zh"));       // 站号
                d.setStationName(text(node, "zm"));       // 站名
                d.setProvince(text(node, "pro"));         // 省
                d.setCity(text(node, "sss"));             // 市
                d.setCounty(text(node, "ssx"));           // 区县
                if(text(node, "sw")!=null) {
                	d.setWaterLevel(new BigDecimal(text(node, "sw")));        // 水位
                }
                if (text(node, "xxsw") !=null) {
                	d.setWaterLimitLevel(new BigDecimal(text(node, "xxsw"))); // 汛限水位
				}
                if(text(node, "jd")!=null){
                	
                	d.setLongitude(new BigDecimal(text(node, "jd")));         // 经度
                }
                if(text(node, "wd") !=null){
                	
                	d.setLatitude(new BigDecimal(text(node, "wd")));          // 纬度
                }

                // 上报时间 -> report_time / data_day
                String sbsj = text(node, "sbsj");
                if (sbsj != null) {
                    try {
                        d.setReportTime(sdf.parse(sbsj));
                        d.setDataDay(DateLocalUtil.getCurrentYmdDate());
                    } catch (Exception e) {
                        d.setDataDay(today);
                    }
                } else {
                    d.setDataDay(today);
                }

                // 是否超汛限：水位 > 汛限水位
                d.setIsOut(isOver(node) ? 1 : 0);

                list.add(d);
            }

            // MyBatis-Plus 分批批量插入
            waterStationDataService.saveBatch(list, BATCH_SIZE);

            log.info("===== 水情数据采集完成，共入库 {} 条 =====", list.size());
        } catch (Exception e) {
            log.error("水情数据采集任务执行失败", e);
        }
    }

    
    /** 安全取字符串字段（null/缺字段返回 null） */
    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    /** 水位是否超过汛限水位 */
    private static boolean isOver(JsonNode node) {
        JsonNode sw = node.get("sw");
        JsonNode xxsw = node.get("xxsw");
        if (sw == null || sw.isNull() || xxsw == null || xxsw.isNull()) {
            return false;
        }
        try {
            return new BigDecimal(sw.asText()).compareTo(new BigDecimal(xxsw.asText())) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
