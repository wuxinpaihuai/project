package com.zjhl.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjhl.project.entity.ProjectFile;
import com.zjhl.project.mapper.ProjectFileMapper;
import com.zjhl.project.service.ProjectFileService;
import org.springframework.stereotype.Service;

@Service
public class ProjectFileServiceImpl extends ServiceImpl<ProjectFileMapper, ProjectFile> implements ProjectFileService {
}
