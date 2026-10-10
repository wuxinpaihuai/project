package com.zjhl.project.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import com.zjhl.project.ProjectApplication;
import com.zjhl.project.task.WaterDataBackfillTask;

/**
 * 水情历史数据补录手动入口。
 *
 * 使用场景：应用因停机等原因导致部分日期数据缺失时，右键运行本类 main 方法补录。
 *
 * 两种用法：
 * 1. 不带参数：补录过去 30 天（昨天往前 29 天 ~ 昨天）内缺失的日期，
 *    已有数据的日期自动跳过（按库中 data_day 判断，哪天缺失补哪天）；
 * 2. 带参数（IDEA 运行配置里填 Program arguments）：开始日期 结束日期
 *    例如：2026-09-20 2026-09-25（同样只补范围内缺失的日期）
 *
 * 说明：
 * - 数据来源为 MarkInfo 页面内嵌的逐小时历史序列，每个缺失日期取"当天最后一条"；
 * - 范围内已有数据的日期会自动跳过，重复运行不会产生重复数据；
 * - 单次最多回溯约 55 天（对方服务端限制），更早的日期补不到；
 * - 1.1 万个站点 5 线程跑，预计 40~90 分钟，期间日志会持续打印进度。
 */
public class WaterDataBackfillRunner {

    /** 水情数据按北京时间出数，"昨天"等日期判断固定用东八区，避免服务器时区不同算错日期 */
    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");

    /** 自动模式回溯天数：过去 30 天（含昨天） */
    private static final int AUTO_BACKFILL_DAYS = 30;

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    public static void main(String[] args) {
        // 先校验参数再启动 Spring，参数非法直接退出，不白连一次数据库
        LocalDate start = null;
        LocalDate end = null;
        if (args.length == 2) {
            start = parseDay(args[0]);
            end = parseDay(args[1]);
            if (start == null || end == null) {
                exit("日期格式错误，应为 yyyy-MM-dd，例如：2026-09-20 2026-09-25");
            }
            if (start.isAfter(end)) {
                exit("开始日期不能晚于结束日期：" + args[0] + " ~ " + args[1]);
            }
        } else if (args.length != 0) {
            exit("参数个数不对：不带参数=自动补录过去 30 天缺失数据，"
                    + "或传入两个参数：开始日期 结束日期，例如 2026-09-20 2026-09-25");
        }

        if (start == null) {
            // 自动模式：过去 30 天（昨天往前 29 天 ~ 昨天），
            // 范围内已有数据的日期由任务按库中 data_day 自动跳过，只补缺失的日期
            end = LocalDate.now(CHINA_ZONE).minusDays(1);
            start = end.minusDays(AUTO_BACKFILL_DAYS - 1);
        }

        ConfigurableApplicationContext context = null;
        try {
            context = new SpringApplicationBuilder(ProjectApplication.class)
                    .web(WebApplicationType.NONE)
                    .run(args);

            System.out.println("补录范围：" + fmt(start) + " ~ " + fmt(end)
                    + "（已有数据的日期将自动跳过）");
            WaterDataBackfillTask task = context.getBean(WaterDataBackfillTask.class);
            task.backfill(fmt(start), fmt(end));

            System.out.println("补录执行完毕，请查看日志确认成功/失败站点数。");
        } catch (Exception e) {
            System.err.println("补录执行失败：" + e.getMessage());
            e.printStackTrace();
            // System.exit 会等待当前线程的 finally 先执行完，context 仍会正常 close
            System.exit(1);
        } finally {
            if (context != null) {
                context.close();
            }
        }
    }

    /** 打印错误信息并以非 0 退出码终止（调用后不会返回） */
    private static void exit(String message) {
        System.err.println(message);
        System.exit(1);
    }

    /** 解析 yyyy-MM-dd 日期，格式非法返回 null */
    private static LocalDate parseDay(String s) {
        try {
            return LocalDate.parse(s.trim(), DAY_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String fmt(LocalDate day) {
        return day.format(DAY_FORMAT);
    }
}
