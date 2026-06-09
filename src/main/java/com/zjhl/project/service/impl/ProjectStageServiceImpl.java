package com.zjhl.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjhl.project.entity.ProjectStage;
import com.zjhl.project.mapper.ProjectStageMapper;
import com.zjhl.project.service.ProjectStageService;
import org.springframework.stereotype.Service;

@Service
public class ProjectStageServiceImpl extends ServiceImpl<ProjectStageMapper, ProjectStage> implements ProjectStageService {
}
