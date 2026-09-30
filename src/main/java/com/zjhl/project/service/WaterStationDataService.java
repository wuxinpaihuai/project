package com.zjhl.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zjhl.project.entity.WaterStationData;

import java.util.Date;

public interface WaterStationDataService extends IService<WaterStationData> {

    /** 按数据日期删除（同一天重复跑任务时先清后插，保证幂等） */
    int deleteByDataDay(Date dataDay);
}
