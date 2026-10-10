package com.zjhl.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zjhl.project.entity.WaterStationData;

import java.util.Date;
import java.util.List;

public interface WaterStationDataService extends IService<WaterStationData> {

    /**
     * 按数据日期删除。
     * 历史遗留方法，已不建议使用：DELETE 会持有大范围行锁，与唯一索引插入意向锁
     * 互相等待，是补录"锁表"的根源。补录幂等请改用 upsertBatch。
     */
    int deleteByDataDay(Date dataDay);

    /** 查询日期范围内已有数据的日期 */
    List<String> selectDataDays(String start, String end);

    /** 查询最新一天的全站记录（补录兜底取站点基础信息用） */
    List<WaterStationData> selectLatestStationInfo();

    /**
     * 批量 UPSERT（按 data_day + station_code 唯一键幂等写入）。
     * 单条 SQL 完成"存在则更新、不存在则插入"，返回本次提交的行数。
     */
    int upsertBatch(List<WaterStationData> list);
}
