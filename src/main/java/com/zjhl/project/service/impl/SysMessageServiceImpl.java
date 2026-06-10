package com.zjhl.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjhl.project.entity.SysMessage;
import com.zjhl.project.mapper.SysMessageMapper;
import com.zjhl.project.service.SysMessageService;
import org.springframework.stereotype.Service;

@Service
public class SysMessageServiceImpl extends ServiceImpl<SysMessageMapper, SysMessage> implements SysMessageService {
}
