package com.zjhl.project.task;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zjhl.project.entity.WaterStationData;
import com.zjhl.project.service.WaterStationDataService;
import com.zjhl.project.util.WaterHistoryParser;

/**
 * 水情历史数据补录任务（手动触发，非定时）。
 *
 * 用于应用停机导致部分日期数据缺失后的补录：
 * 对每个站点请求 MarkInfo 页面（SSR HTML 内嵌逐小时历史数据），
 * 取每个缺失日期"当天最后一条"水位记录入库，口径与每日定时任务一致。
 *
 * 站点基础信息（省/市/区县/经纬度/汛限水位）优先取自实时清单接口；
 * 实时接口中缺失的站点（已下线/改名），从库中最新一天的数据兜底获取；
 * 省份兜底默认"浙江省"。
 *
 * 由 util.WaterDataBackfillRunner 的 main 方法触发。
 *
 * ============================ 性能设计说明 ============================
 * 1) 抓取与入库解耦：抓取线程只做 HTTP + 解析，产出数据后投入结果队列；
 *    入库由独立的单线程负责。这样 DB 写入天然串行，不会出现多个线程同时
 *    写同一张表 + 唯一索引导致的锁竞争/锁等待。
 * 2) 跨站合并批次：原实现是"每站一次 saveBatch"，而每站最多只有几十条
 *    （缺失天数），批次远未填满就提交一次事务 —— 1.1 万站就是 1.1 万次事务
 *    提交（每次都 fsync）。现在改为全局攒批，攒够 DB_BATCH_SIZE 条才写一次。
 * 3) 幂等写入用 UPSERT：依赖唯一键 uk_day_station(data_day, station_code)，
 *    一条多值 INSERT ... ON DUPLICATE KEY UPDATE 完成，不再"先删后插"
 *    （DELETE 持大范围行锁是之前锁表的根源），重复运行也不会报唯一键冲突。
 * 4) 有界队列做背压：任务队列与结果队列都设上限，抓取快于入库时自动阻塞
 *    抓取线程，避免把全部站点数据/结果一次性堆在内存里。
 * 5) 降低对象与日志开销：每个站点先聚合出"每天最后一条"再创建实体
 *    （避免为每条小时记录都 new 一个实体），日志按固定步长打印。
 * ====================================================================
 */
@Component
public class WaterDataBackfillTask {

    private static final Logger log = LoggerFactory.getLogger(WaterDataBackfillTask.class);

    /** 实时全站清单接口 */
    private static final String STATION_LIST_URL =
            "https://sqfb.slt.zj.gov.cn:30050/rest/water/getNewDataList";

    /** 历史数据页面（SSR，HTML 内嵌逐小时序列） */
    private static final String MARK_INFO_URL =
            "https://sqfb.slt.zj.gov.cn:30050/nuxtsyq/new/MarkInfo";

    /** 单次请求最大回溯天数（实测 day=90 会服务端超时，60 可用，留余量取 55） */
    private static final int MAX_DAY = 55;

    /** 并发抓取线程数（政务网站，不宜过高；仅为 HTTP 并发，入库不参与） */
    private static final int THREADS = 5;

    /** 每线程每次请求间隔，避免给对方服务器造成压力 */
    private static final long REQUEST_INTERVAL_MS = 200;

    private static final int MAX_RETRY = 3;

    private static final String DEFAULT_PROVINCE = "浙江省";

    /** 单次入库的批量条数（多值 INSERT 行数，控制在 max_allowed_packet 之内） */
    private static final int DB_BATCH_SIZE = 500;

    /** 抓取结果队列容量：队列满则抓取线程阻塞（背压），避免解析结果堆积占内存 */
    private static final int ROW_QUEUE_CAPACITY = 64;

    /** 抓取任务队列容量：配合 CallerRunsPolicy 形成背压，避免上万任务一次性入队 */
    private static final int TASK_QUEUE_CAPACITY = THREADS * 4;

    /** 入库线程收到该哨兵对象即退出（正常空列表也会在队列里流转，故用引用比较） */
    private static final List<WaterStationData> POISON_PILL = new ArrayList<>(0);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private WaterStationDataService waterStationDataService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 补录指定日期范围内缺失的数据。
     *
     * @param startDate 开始日期 yyyy-MM-dd（含）
     * @param endDate   结束日期 yyyy-MM-dd（含），不能超过昨天
     */
    public void backfill(String startDate, String endDate) {
        log.info("===== 水情历史数据补录开始：{} ~ {} =====", startDate, endDate);
        long startedAt = System.currentTimeMillis();
        try {
            // 1. 计算缺失日期（范围内已有数据的日期自动跳过）
            List<String> existing = waterStationDataService.selectDataDays(startDate, endDate);
            Set<String> existingSet = new HashSet<>(existing);
            List<String> missingDays = new ArrayList<>();
            LocalDate d = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            while (!d.isAfter(end)) {
                String day = d.format(DateTimeFormatter.ISO_LOCAL_DATE);
                if (!existingSet.contains(day)) {
                    missingDays.add(day);
                }
                d = d.plusDays(1);
            }
            if (missingDays.isEmpty()) {
                log.info("该日期范围内数据完整，无需补录");
                return;
            }
            log.info("缺失日期共 {} 天：{}", missingDays.size(), missingDays);

            // 2. day 参数：从今天往回推，一次请求覆盖整个缺失区间
            long dayRange = ChronoUnit.DAYS.between(LocalDate.parse(startDate), LocalDate.now()) + 1;
            if (dayRange > MAX_DAY) {
                log.warn("开始日期距今 {} 天，超过单次请求上限 {} 天，最早的部分日期可能补不到",
                        dayRange, MAX_DAY);
                dayRange = MAX_DAY;
            }
            // lambda 中引用，必须为 effectively final
            final long dayParam = dayRange;

            // 3. 汇总站点清单：实时接口优先，库中已有站点兜底
            List<StationInfo> stations = collectStations();
            final int total = stations.size();
            log.info("站点总数：{}", total);

            // 4. 结果队列 + 入库线程（DB 写入串行化）
            BlockingQueue<List<WaterStationData>> rowQueue =
                    new ArrayBlockingQueue<>(ROW_QUEUE_CAPACITY);
            AtomicInteger okCount = new AtomicInteger(0);
            AtomicInteger rowCount = new AtomicInteger(0);
            AtomicInteger batchCount = new AtomicInteger(0);
            ConcurrentLinkedQueue<String> failedStations = new ConcurrentLinkedQueue<>();

            Thread dbThread = new Thread(
                    () -> consumeAndUpsert(rowQueue, rowCount, batchCount), "water-backfill-db");
            dbThread.setDaemon(false);
            dbThread.start();

            // 5. 抓取线程池：有界队列 + CallerRunsPolicy（主线程也参与抓取，形成背压）
            ThreadPoolExecutor fetchPool = new ThreadPoolExecutor(
                    THREADS, THREADS, 0L, TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(TASK_QUEUE_CAPACITY),
                    r -> {
                        Thread t = new Thread(r, "water-backfill-fetch");
                        t.setDaemon(false);
                        return t;
                    },
                    new ThreadPoolExecutor.CallerRunsPolicy());

            Set<String> missingSet = new HashSet<>(missingDays);
            CountDownLatch latch = new CountDownLatch(total);

            for (StationInfo st : stations) {
                fetchPool.execute(() -> {
                    try {
                        // 解析结果在方法内即时产出，html 随之失效可被 GC 回收
                        List<WaterStationData> rows = fetchAndParse(st, dayParam, missingSet);
                        if (!rows.isEmpty()) {
                            // 队列满则阻塞 -> 抓取自动降速，内存不会无限增长
                            rowQueue.put(rows);
                        }
                        int done = okCount.incrementAndGet();
                        if (done % 1000 == 0) {
                            log.info("抓取进度：{}/{}，已入库 {} 条", done, total, rowCount.get());
                        }
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        failedStations.add(st.zh + "/" + st.zm);
                        log.warn("站点 {}({}) 补录失败：{}", st.zm, st.zh, e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // 6. 等待抓取完成 -> 投毒丸 -> 等待入库收尾
            boolean finished = latch.await(6, TimeUnit.HOURS);
            if (finished) {
                fetchPool.shutdown();
            } else {
                log.warn("等待抓取线程超时（6 小时），强制中断剩余任务");
                fetchPool.shutdownNow();
            }
            rowQueue.put(POISON_PILL);
            dbThread.join(TimeUnit.MINUTES.toMillis(30));
            if (dbThread.isAlive()) {
                log.error("入库线程未在 30 分钟内结束，可能仍有数据未落库");
            }

            long costSec = (System.currentTimeMillis() - startedAt) / 1000;
            log.info("===== 补录完成：成功 {} 站，失败 {} 站，入库 {} 条，提交 {} 批，耗时 {} 秒 =====",
                    okCount.get(), failedStations.size(), rowCount.get(), batchCount.get(), costSec);
            if (!failedStations.isEmpty()) {
                List<String> fs = new ArrayList<>(failedStations);
                log.warn("失败站点共 {} 个（再次运行本任务即可重试，UPSERT 保证不会产生重复数据）：{}",
                        fs.size(), fs.size() > 50 ? new ArrayList<>(fs.subList(0, 50)) + " ..." : fs);
            }
        } catch (Exception e) {
            log.error("补录任务执行失败", e);
        }
    }

    /**
     * 入库线程主体：从结果队列取数据，攒够 DB_BATCH_SIZE 条批量 UPSERT 一次。
     * 收到毒丸后把剩余不足一批的数据写完再退出。
     */
    private void consumeAndUpsert(BlockingQueue<List<WaterStationData>> queue,
                                  AtomicInteger rowCount, AtomicInteger batchCount) {
        List<WaterStationData> buffer = new ArrayList<>(DB_BATCH_SIZE);
        try {
            while (true) {
                List<WaterStationData> part = queue.take();
                if (part == POISON_PILL) {
                    break;
                }
                buffer.addAll(part);
                if (buffer.size() >= DB_BATCH_SIZE) {
                    flush(buffer, rowCount, batchCount);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("入库线程被中断，缓冲区剩余 {} 条未写入", buffer.size());
        } catch (Exception e) {
            log.error("入库线程异常退出，缓冲区剩余 {} 条未写入", buffer.size(), e);
        } finally {
            // 正常收尾：写完不足一批的剩余数据
            if (!buffer.isEmpty()) {
                flush(buffer, rowCount, batchCount);
            }
        }
    }

    /**
     * 批量 UPSERT 并清空缓冲。整批失败时降级为逐条写入，
     * 避免一条脏数据（如字段超长）导致整批丢失。
     */
    private void flush(List<WaterStationData> buffer, AtomicInteger rowCount, AtomicInteger batchCount) {
        if (buffer.isEmpty()) {
            return;
        }
        try {
            waterStationDataService.upsertBatch(buffer);
            rowCount.addAndGet(buffer.size());
            int n = batchCount.incrementAndGet();
            if (n % 20 == 0) {
                log.info("已入库 {} 条（{} 批）", rowCount.get(), n);
            }
        } catch (Exception e) {
            log.warn("批量入库失败（{} 条），降级为逐条写入：{}", buffer.size(), e.getMessage());
            int ok = 0;
            int loggedFails = 0;
            for (WaterStationData row : buffer) {
                try {
                    waterStationDataService.upsertBatch(Collections.singletonList(row));
                    ok++;
                } catch (Exception ex) {
                    if (loggedFails++ < 5) {
                        log.warn("单条写入失败 station={} day={}：{}",
                                row.getStationCode(), row.getDataDay(), ex.getMessage());
                    }
                }
            }
            rowCount.addAndGet(ok);
        } finally {
            buffer.clear();
        }
    }

    /**
     * 汇总站点清单：实时接口全量站点 + 库中最新一天有记录但实时接口已缺失的站点。
     */
    private List<StationInfo> collectStations() throws Exception {
        Map<String, StationInfo> map = new LinkedHashMap<>();
        for (StationInfo s : fetchStationList()) {
            map.put(s.zh, s);
        }
        // 兜底：库中最新一天的站点（已下线/改名的站点实时接口查不到）
        List<WaterStationData> dbStations = waterStationDataService.selectLatestStationInfo();
        int fromDb = 0;
        for (WaterStationData row : dbStations) {
            if (row.getStationCode() == null || map.containsKey(row.getStationCode())) {
                continue;
            }
            StationInfo s = new StationInfo();
            s.zh = row.getStationCode();
            s.zm = row.getStationName();
            s.pro = row.getProvince() != null ? row.getProvince() : DEFAULT_PROVINCE;
            s.sss = row.getCity();
            s.ssx = row.getCounty();
            s.jd = row.getLongitude() != null ? row.getLongitude().toPlainString() : null;
            s.wd = row.getLatitude() != null ? row.getLatitude().toPlainString() : null;
            s.xxsw = row.getWaterLimitLevel() != null ? row.getWaterLimitLevel().toPlainString() : null;
            map.put(s.zh, s);
            fromDb++;
        }
        if (fromDb > 0) {
            log.info("其中 {} 个站点来自库中已有数据（实时接口已缺失）", fromDb);
        }
        return new ArrayList<>(map.values());
    }

    /**
     * 抓取单个站点的历史页并解析：返回该站点在缺失日期内"每天最后一条"的记录。
     * 只做 HTTP + 解析，不做入库（入库统一由入库线程批量执行）。
     */
    private List<WaterStationData> fetchAndParse(StationInfo st, long dayParam,
                                                 Set<String> missingDays) throws Exception {
        String html = fetchMarkInfoHtml(st, dayParam);
        if (html == null) {
            return Collections.emptyList();
        }
        try {
            List<WaterHistoryParser.PzRecord> pz = WaterHistoryParser.extractPz(html);
            if (pz.isEmpty()) {
                return Collections.emptyList();
            }
            // 汛限水位：历史页面 detail 优先，取不到用清单里的（实时接口或库中已有数据）
            String xxsw = WaterHistoryParser.extractXxsw(html);
            if (xxsw == null) {
                xxsw = st.xxsw;
            }
            return buildDayRecords(st, pz, xxsw, missingDays);
        } finally {
            // html 可能数 MB，显式断开引用，让大字符串尽早可回收
            html = null;
        }
    }

    /**
     * 从逐小时序列中聚合出"每个缺失日期当天最后一条"，再构建实体。
     * 先聚合字符串、最后才 new 实体，避免为每条小时记录都创建对象
     * （55 天 x 24 小时 = 1320 条，原实现会产生 1300+ 个废弃对象）。
     */
    private List<WaterStationData> buildDayRecords(StationInfo st,
                                                   List<WaterHistoryParser.PzRecord> pz,
                                                   String xxsw, Set<String> missingDays) {
        // pz 按时间升序，同一日期反复 put，最后留下的即"当天最后一条"
        Map<String, String[]> lastOfDay = new LinkedHashMap<>();
        for (WaterHistoryParser.PzRecord r : pz) {
            if (r.sw == null || r.tm == null || r.tm.length() < 10) {
                continue;
            }
            String day = r.tm.substring(0, 10);
            if (!missingDays.contains(day)) {
                continue;
            }
            lastOfDay.put(day, new String[]{r.tm, r.sw});
        }
        if (lastOfDay.isEmpty()) {
            return Collections.emptyList();
        }

        List<WaterStationData> list = new ArrayList<>(lastOfDay.size());
        for (Map.Entry<String, String[]> e : lastOfDay.entrySet()) {
            String day = e.getKey();
            String tm = e.getValue()[0];
            String sw = e.getValue()[1];

            WaterStationData data = new WaterStationData();
            try {
                data.setDataDay(toDate(day, "yyyy-MM-dd"));
                data.setReportTime(toDate(tm, "yyyy-MM-dd'T'HH:mm:ss"));
            } catch (Exception ignore) {
                continue;
            }
            data.setStationCode(st.zh);
            data.setStationName(st.zm);
            data.setProvince(st.pro != null ? st.pro : DEFAULT_PROVINCE);
            data.setCity(st.sss);
            data.setCounty(st.ssx);
            data.setLongitude(toBigDecimal(st.jd));
            data.setLatitude(toBigDecimal(st.wd));
            data.setWaterLevel(toBigDecimal(sw));
            data.setWaterLimitLevel(toBigDecimal(xxsw));
            data.setIsOut(isOver(sw, xxsw) ? 1 : 0);
            list.add(data);
        }
        return list;
    }

    /** 带重试地拉取 MarkInfo 页面 HTML */
    private String fetchMarkInfoHtml(StationInfo st, long dayParam) throws Exception {
        Exception lastError = null;
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                Thread.sleep(REQUEST_INTERVAL_MS);
                // 用 URI 对象避免 RestTemplate 对已编码中文二次编码
                URI uri = new URI(MARK_INFO_URL + "?zh=" + st.zh
                        + "&zm=" + URLEncoder.encode(st.zm == null ? "" : st.zm, "UTF-8")
                        + "&day=" + dayParam);
                return restTemplate.getForObject(uri, String.class);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw ie;
            } catch (Exception e) {
                lastError = e;
                if (attempt < MAX_RETRY) {
                    try {
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("补录线程被中断", ie);
                    }
                }
            }
        }
        throw lastError != null ? lastError : new RuntimeException("请求历史数据失败");
    }

    /** 拉取实时全站清单 */
    private List<StationInfo> fetchStationList() throws Exception {
        URI uri = new URI(STATION_LIST_URL
                + "?areaFlag=1"
                + "&sss=" + URLEncoder.encode("全部", "UTF-8")
                + "&ssx=&zl=RR,ZZ,ZQ,TT,DD&sklx=1,9,2,3,4,5&ly=&sfcj=0"
                + "&bxdj=1,2,3,4,5&zm=&cjly=&bx=0");
        String json = restTemplate.getForObject(uri, String.class);
        JsonNode qt = objectMapper.readTree(json).get("qt");
        List<StationInfo> list = new ArrayList<>(qt.size());
        for (JsonNode n : qt) {
            StationInfo s = new StationInfo();
            s.zh = text(n, "zh");
            s.zm = text(n, "zm");
            s.pro = text(n, "pro");
            s.sss = text(n, "sss");
            s.ssx = text(n, "ssx");
            s.jd = text(n, "jd");
            s.wd = text(n, "wd");
            s.xxsw = text(n, "xxsw");
            list.add(s);
        }
        return list;
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    /** 字符串转 BigDecimal，空值/非法数值返回 null */
    private static BigDecimal toBigDecimal(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isOver(String sw, String xxsw) {
        BigDecimal a = toBigDecimal(sw);
        BigDecimal b = toBigDecimal(xxsw);
        return a != null && b != null && a.compareTo(b) > 0;
    }

    /** 按固定格式把字符串解析为 Date */
    private static Date toDate(String s, String pattern) throws Exception {
        return new SimpleDateFormat(pattern).parse(s);
    }

    /** 站点基础信息（来自实时清单接口，或库中已有数据兜底） */
    private static class StationInfo {
        String zh, zm, pro, sss, ssx, jd, wd, xxsw;
    }
}
