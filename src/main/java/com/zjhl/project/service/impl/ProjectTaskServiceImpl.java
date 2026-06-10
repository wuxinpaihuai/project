package com.zjhl.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjhl.project.entity.ProjectTask;
import com.zjhl.project.mapper.ProjectTaskMapper;
import com.zjhl.project.service.ProjectTaskService;
import org.springframework.stereotype.Service;

@Service
public class ProjectTaskServiceImpl extends ServiceImpl<ProjectTaskMapper, ProjectTask> implements ProjectTaskService {
}
