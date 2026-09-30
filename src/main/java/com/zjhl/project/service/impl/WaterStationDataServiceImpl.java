package com.zjhl.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjhl.project.entity.WaterStationData;
import com.zjhl.project.mapper.WaterStationDataMapper;
import com.zjhl.project.service.WaterStationDataService;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class WaterStationDataServiceImpl
        extends ServiceImpl<WaterStationDataMapper, WaterStationData>
        implements WaterStationDataService {

    @Override
    public int deleteByDataDay(Date dataDay) {
        return baseMapper.delete(
                new LambdaQueryWrapper<WaterStationData>()
                        .eq(WaterStationData::getDataDay, dataDay));
    }
}
