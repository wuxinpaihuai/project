package com.zjhl.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjhl.project.entity.WaterStationInfo;
import com.zjhl.project.mapper.WaterStationInfoMapper;
import com.zjhl.project.service.WaterStationInfoService;
import org.springframework.stereotype.Service;

@Service
public class WaterStationInfoServiceImpl extends ServiceImpl<WaterStationInfoMapper, WaterStationInfo> implements WaterStationInfoService {
}
