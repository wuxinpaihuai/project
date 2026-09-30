package com.zjhl.project.util;

import com.zjhl.project.ProjectApplication;
import com.zjhl.project.task.WaterDataFetchTask;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 水情数据采集手动执行入口。
 *
 * 使用场景：当天 17:00 的定时任务因故（服务重启、接口异常等）没有成功执行时，
 * 在 IDEA/Eclipse 中直接右键运行本类的 main 方法，即可立即补跑一次采集。
 *
 * 说明：
 * - 以非 Web 模式启动 Spring 容器，不会占用 8080 端口，
 *   即使主工程正在运行也不会冲突；
 * - 采集逻辑与定时任务完全共用同一个 WaterDataFetchTask；
 * - 任务本身是幂等的（先按 data_day 删除当天旧数据再插入），重复执行不会产生重复数据。
 */
public class WaterDataManualRunner {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = null;
        try {
            context = new SpringApplicationBuilder(ProjectApplication.class)
                    .web(WebApplicationType.NONE)   // 不启动 Web 服务，避免端口冲突
                    .run(args);

            WaterDataFetchTask task = context.getBean(WaterDataFetchTask.class);
            task.fetchAndSave();

            System.out.println("手动采集执行完毕，请查看上方日志确认入库条数。");
        } finally {
            if (context != null) {
                context.close();
            }
        }
    }
}
