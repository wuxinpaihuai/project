package com.zjhl.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjhl.project.entity.ProjectTaskLog;
import com.zjhl.project.mapper.ProjectTaskLogMapper;
import com.zjhl.project.service.ProjectTaskLogService;
import org.springframework.stereotype.Service;

@Service
public class ProjectTaskLogServiceImpl extends ServiceImpl<ProjectTaskLogMapper, ProjectTaskLog> implements ProjectTaskLogService {
}
