package com.zjhl.project.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

public interface ProjectImportService {

    /**
     * 现有项目批量导入：解析Excel，逐行校验后写入 project_info / project_stage / project_extend 三张表
     *
     * @param bisnessUserId   当前登录用户ID（写入 project_info.bisness_user_id）
     * @param bisnessUserName 当前登录用户姓名（写入 project_info.bisness_user_name）
     * @return 导入结果 total/successCount/failCount/failList(row, projectName, reason)
     */
    Map<String, Object> importOldProjects(MultipartFile file, Long bisnessUserId, String bisnessUserName) throws IOException;
}
