package com.zjhl.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjhl.project.entity.WaterStationData;
import com.zjhl.project.mapper.WaterStationDataMapper;
import com.zjhl.project.service.WaterStationDataService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class WaterStationDataServiceImpl
        extends ServiceImpl<WaterStationDataMapper, WaterStationData>
        implements WaterStationDataService {

    @Override
    public int deleteByDataDay(Date dataDay) {
        QueryWrapper<WaterStationData> wrapper = new QueryWrapper<>();
        wrapper.eq("data_day", dataDay);
        return baseMapper.delete(wrapper);
    }

    @Override
    public List<String> selectDataDays(String start, String end) {
        return baseMapper.selectDataDays(start, end);
    }

    @Override
    public List<WaterStationData> selectLatestStationInfo() {
        return baseMapper.selectLatestStationInfo();
    }

    @Override
    public int upsertBatch(List<WaterStationData> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        return baseMapper.upsertBatch(list);
    }
}
