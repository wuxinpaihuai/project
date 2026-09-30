# 浙江实时水情每日采集任务（适配 com.zjhl.project 工程）

基于你现有工程的技术栈：JDK 8 + Spring Boot 2.7.18 + MyBatis-Plus 3.5.3.1 + MySQL + Lombok。
每天 17:00 定时调用浙江省水利厅实时水情接口，将约 1.1 万条站点水位数据写入
`water_station_data` 表；另提供 main 方法手动补跑入口。

## 文件清单（按你工程的包结构放置）

| 文件 | 放入位置 |
|---|---|
| `com/zjhl/project/entity/WaterStationData.java` | `com.zjhl.project.entity` |
| `com/zjhl/project/mapper/WaterStationDataMapper.java` | `com.zjhl.project.mapper` |
| `com/zjhl/project/service/WaterStationDataService.java` | `com.zjhl.project.service` |
| `com/zjhl/project/service/impl/WaterStationDataServiceImpl.java` | `com.zjhl.project.service.impl` |
| `com/zjhl/project/task/WaterDataFetchTask.java` | 新建 `com.zjhl.project.task` 包 |
| `com/zjhl/project/config/RestTemplateConfig.java` | `com.zjhl.project.config` |
| `com/zjhl/project/util/WaterDataManualRunner.java` | `com.zjhl.project.util`（手动执行入口） |

不需要改 pom.xml（web/Jackson、MyBatis-Plus、Lombok、MySQL 驱动你都有了），
也不需要 mapper XML（用的是 MyBatis-Plus 的 BaseMapper + saveBatch）。

## 接入步骤（共 3 步）

### 1. 复制上面 7 个文件到对应包

### 2. application.yml 追加一段配置

```yaml
water:
  fetch:
    # 每天 17:00 抓取；想改时间直接改这个 cron
    cron: "0 0 17 * * ?"
```

### 3. 确认启动类上有 `@EnableScheduling`

```java
@EnableScheduling
@SpringBootApplication
public class ProjectApplication { ... }
```

另外注意：`WaterDataManualRunner` 中 import 的启动类写的是 `ProjectApplication`，
如果你工程的启动类名不同（如 `Application`、`ProjectMainApplication`），改一下这个引用即可。

## 手动补跑（任务当天没执行时）

在 IDEA/Eclipse 中打开 `com.zjhl.project.util.WaterDataManualRunner`，
右键运行 main 方法即可立即采集一次。特点：

- 以**非 Web 模式**启动，不占 8080 端口，主工程正在运行也不冲突；
- 与定时任务共用同一套采集逻辑；
- **幂等**：每次入库前按 `data_day` 先删后插，重复跑不会产生重复数据。

## 关键设计说明

- **执行时间**：每天 17:00（`water.fetch.cron` 可配）。
- **数据来源**：`GET https://sqfb.slt.zj.gov.cn:30050/rest/water/getNewDataList`，
  参数与页面一致，站点数组在返回 JSON 的 `qt` 字段。
- **SSL**：目标站点证书不在 JDK 默认信任库中，`RestTemplateConfig` 放宽了校验，
  仅用于采集该公开接口。
- **超时**：接口返回约 4.5MB，读取超时设 180 秒。
- **is_out**：按 `水位 > 汛限水位` 计算，汛限水位为空的站点记 0。
- **data_day**：取上报时间 `sbsj` 的日期部分，而非任务执行日期。
- **容错**：单条数据时间/数字格式异常不中断整批；接口异常仅记日志，不影响主工程。
- **逻辑删除**：实体没有加 `@TableLogic` 字段，`deleteByDataDay` 是物理删除，
  不受 yml 中 `logic-delete-field: deleted` 全局配置影响。

## 字段对应关系

| 接口字段 | 表字段 | 说明 |
|---|---|---|
| sbsj 的日期部分 | data_day | yyyy-MM-dd |
| zm | station_name | 站名 |
| zh | station_code | 站号 |
| sw | water_level | 水位(m) |
| xxsw | water_limit_level | 汛限水位(m) |
| sw > xxsw | is_out | 1=超汛限，0=否 |
| jd / wd | longitude / latitude | 经度 / 纬度 |
| pro / sss / ssx | province / city / county | 省 / 市 / 区县 |
| sbsj | report_time | 上报时间 |
