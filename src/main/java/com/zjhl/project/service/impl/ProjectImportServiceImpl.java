package com.zjhl.project.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zjhl.project.dto.ProjectImportRow;
import com.zjhl.project.entity.ProjectExtend;
import com.zjhl.project.entity.ProjectInfo;
import com.zjhl.project.entity.ProjectStage;
import com.zjhl.project.entity.SysUser;
import com.zjhl.project.service.ProjectExtendService;
import com.zjhl.project.service.ProjectImportService;
import com.zjhl.project.service.ProjectInfoService;
import com.zjhl.project.service.ProjectStageService;
import com.zjhl.project.service.SysUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ProjectImportServiceImpl implements ProjectImportService {

    private static final Logger log = LoggerFactory.getLogger(ProjectImportServiceImpl.class);

    @Autowired
    private ProjectInfoService projectInfoService;

    @Autowired
    private ProjectStageService projectStageService;

    @Autowired
    private ProjectExtendService projectExtendService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /** 每行记录独立事务（REQUIRES_NEW），单行失败回滚该行三张表的写入，不影响其他行 */
    private TransactionTemplate txTemplate;

    @PostConstruct
    public void init() {
        DefaultTransactionAttribute attr = new DefaultTransactionAttribute();
        attr.setPropagationBehavior(Propagation.REQUIRES_NEW.value());
        this.txTemplate = new TransactionTemplate(transactionManager, attr);
    }

    /** 项目类型：名称 → 编码（project_info.project_type） */
    private static final Map<String, Integer> PROJECT_TYPE_MAP = new LinkedHashMap<>();
    static {
        PROJECT_TYPE_MAP.put("环评", 1);
        PROJECT_TYPE_MAP.put("场调", 2);
        PROJECT_TYPE_MAP.put("应急预案", 3);
        PROJECT_TYPE_MAP.put("验收", 4);
        PROJECT_TYPE_MAP.put("环评入围", 5);
        PROJECT_TYPE_MAP.put("场调入围", 6);
        PROJECT_TYPE_MAP.put("环保咨询", 7);
        PROJECT_TYPE_MAP.put("环评+验收", 8);
        PROJECT_TYPE_MAP.put("水污染治理", 10);
        PROJECT_TYPE_MAP.put("大气污染治理", 20);
        PROJECT_TYPE_MAP.put("污染土壤修复", 30);
        PROJECT_TYPE_MAP.put("其他", 99);
    }

    /** 项目来源：名称 → 编码（project_info.project_source） */
    private static final Map<String, String> PROJECT_SOURCE_MAP = new LinkedHashMap<>();
    static {
        PROJECT_SOURCE_MAP.put("自拓非招投标", "1");
        PROJECT_SOURCE_MAP.put("自拓+招投标", "2");
        PROJECT_SOURCE_MAP.put("渠道维护非招投标", "3");
        PROJECT_SOURCE_MAP.put("渠道维护+招投标", "4");
        PROJECT_SOURCE_MAP.put("公开招投标", "5");
    }

    @Override
    public Map<String, Object> importOldProjects(MultipartFile file, Long bisnessUserId, String bisnessUserName) throws IOException {
        // ========== 1. 解析Excel（表头在第1行，逐行读取并回填Excel实际行号） ==========
        List<ProjectImportRow> rows = new ArrayList<>();
        EasyExcel.read(file.getInputStream(), ProjectImportRow.class, new AnalysisEventListener<ProjectImportRow>() {
            @Override
            public void invoke(ProjectImportRow row, AnalysisContext context) {
                row.setRowNum(context.readRowHolder().getRowIndex() + 1);
                rows.add(row);
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
            }
        }).sheet().doRead();

        Map<String, Object> result = new HashMap<>();
        if (rows.isEmpty()) {
            result.put("total", 0);
            result.put("successCount", 0);
            result.put("failCount", 0);
            result.put("failList", new ArrayList<>());
            return result;
        }

        // ========== 2. 预加载全部用户（real_name → id）与已有项目名称，避免逐行查询数据库 ==========
        Map<String, Long> userMap = loadUserMap();
        Set<String> existingNames = loadExistingProjectNames();

        int successCount = 0;
        List<Map<String, Object>> failList = new ArrayList<>();

        for (ProjectImportRow row : rows) {
            // 跳过整行为空的行
            if (isBlankRow(row)) {
                continue;
            }

            // ========== 3. 校验（含项目名称去重） ==========
            List<String> errors = validate(row, userMap, existingNames);
            if (!errors.isEmpty()) {
                failList.add(buildFailItem(row, String.join("；", errors)));
                continue;
            }

            // ========== 4. 逐行独立事务写入三张表 ==========
            try {
                txTemplate.execute(status -> {
                    importOneRow(row, userMap, bisnessUserId, bisnessUserName);
                    return null;
                });
                successCount++;
                // 导入成功后将名称加入去重集合，防止同一文件内重名项目重复导入
                existingNames.add(trim(row.getProjectName()));
            } catch (Exception e) {
                log.error("现有项目批量导入失败，Excel行号={}", row.getRowNum(), e);
                failList.add(buildFailItem(row, "写入数据库失败：" + e.getMessage()));
            }
        }

        result.put("total", successCount + failList.size());
        result.put("successCount", successCount);
        result.put("failCount", failList.size());
        result.put("failList", failList);
        return result;
    }

    /**
     * 单行校验，返回错误信息列表（空表示通过）
     */
    private List<String> validate(ProjectImportRow row, Map<String, Long> userMap, Set<String> existingNames) {
        List<String> errors = new ArrayList<>();
        String projectName = trim(row.getProjectName());
        if (projectName == null) {
            errors.add("项目名称不能为空");
        } else if (existingNames.contains(projectName)) {
            errors.add("项目名称【" + projectName + "】在系统中已存在，不允许重复导入");
        }
        if (isBlank(row.getProjectNo())) {
            errors.add("项目编号不能为空");
        }
        // 项目类型：留空不填，填了必须是有效值（数字编码或名称）
        String typeRaw = trim(row.getProjectType());
        if (typeRaw != null && resolveProjectType(typeRaw) == null) {
            errors.add("项目类型【" + typeRaw + "】无效，请填写数字编码或名称：" + String.join("/", PROJECT_TYPE_MAP.keySet()));
        }
        // 工期（天）：留空不填，填了必须是整数
        String cycleRaw = trim(row.getProjectCycle());
        if (cycleRaw != null && !cycleRaw.matches("\\d+")) {
            errors.add("工期（天）【" + cycleRaw + "】必须是整数");
        }
        // 项目来源：留空不填，填了必须是有效值（数字编码或名称）
        String sourceRaw = trim(row.getProjectSource());
        if (sourceRaw != null && resolveProjectSource(sourceRaw) == null) {
            errors.add("项目来源【" + sourceRaw + "】无效，请填写数字编码或名称：" + String.join("/", PROJECT_SOURCE_MAP.keySet()));
        }
        // 技术负责人：必填，且必须能在sys_user中通过real_name匹配到用户
        String techName = trim(row.getTechUserName());
        if (techName == null) {
            errors.add("技术负责人不能为空");
        } else if (!userMap.containsKey(techName)) {
            errors.add("技术负责人【" + techName + "】在系统中不存在");
        }
        return errors;
    }

    /**
     * 单行数据入库：
     *  1) project_info 项目基本信息（技术负责人real_name → sys_user.id → tech_user_id）
     *  2) project_stage 三条记录：投标(1,已完成)、签约(2,进行中)、实施(3,进行中)
     *  3) project_extend 一条记录：is_win_bid=1、win_bid_amount=合同金额、is_sign=0
     * 由调用方（逐行REQUIRES_NEW事务）保证三张表同生共死
     */
    private void importOneRow(ProjectImportRow row, Map<String, Long> userMap,
                              Long bisnessUserId, String bisnessUserName) {
        LocalDateTime now = LocalDateTime.now();
        String techName = trim(row.getTechUserName());

        // ========== 1. 项目基本信息 ==========
        ProjectInfo project = new ProjectInfo();
        project.setProjectName(trim(row.getProjectName()));
        project.setProjectNo(trim(row.getProjectNo()));
        project.setOwnerCompany(trim(row.getOwnerCompany()));
        project.setOwnerContact(trim(row.getOwnerContact()));
        project.setOwnerPhone(trim(row.getOwnerPhone()));
        project.setProjectType(resolveProjectType(trim(row.getProjectType())));
        String cycleRaw = trim(row.getProjectCycle());
        project.setProjectCycle(cycleRaw != null ? Integer.valueOf(cycleRaw) : null);
        project.setProjectSource(resolveProjectSource(trim(row.getProjectSource())));
        project.setBisnessUserId(bisnessUserId);
        project.setBisnessUserName(bisnessUserName);
        project.setTechUserId(userMap.get(techName));
        project.setTechUserName(techName);
        project.setProjectAmount(new BigDecimal(row.getWinBidAmount()));
        project.setCreateTime(now);
        project.setUpdateTime(now);
        projectInfoService.save(project);
        Long projectId = project.getId();

        // ========== 2. 项目阶段三条记录 ==========
        List<ProjectStage> stages = new ArrayList<>(3);
        stages.add(buildStage(projectId, 1, 2, now));
        stages.add(buildStage(projectId, 2, 1, now));
        stages.add(buildStage(projectId, 3, 1, now));
        projectStageService.saveBatch(stages);

        // ========== 3. 项目拓展信息（已中标、未签约） ==========
        ProjectExtend extend = new ProjectExtend();
        extend.setProjectId(projectId);
        extend.setIsWinBid(1);
        extend.setWinBidAmount(trim(row.getWinBidAmount()));
        extend.setIsSign(0);
        extend.setCreateTime(now);
        projectExtendService.save(extend);
    }

    private ProjectStage buildStage(Long projectId, Integer stageType, Integer stageStatus, LocalDateTime now) {
        ProjectStage stage = new ProjectStage();
        stage.setProjectId(projectId);
        stage.setStageType(stageType);
        stage.setStageStatus(stageStatus);
        stage.setCreateTime(now);
        return stage;
    }

    /**
     * 项目类型：数字编码直接解析，名称查映射表；无法识别返回null
     */
    private Integer resolveProjectType(String value) {
        if (value.matches("\\d+")) {
            return Integer.valueOf(value);
        }
        return PROJECT_TYPE_MAP.get(value);
    }

    /**
     * 项目来源：数字编码直接使用，名称查映射表；无法识别返回null
     */
    private String resolveProjectSource(String value) {
        if (value.matches("\\d+")) {
            return value;
        }
        return PROJECT_SOURCE_MAP.get(value);
    }

    /**
     * 预加载 sys_user（real_name → id），同名取ID最小的一个
     */
    private Map<String, Long> loadUserMap() {
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.select("id", "real_name");
        wrapper.isNotNull("real_name");
        wrapper.ne("real_name", "");
        wrapper.orderByAsc("id");
        List<SysUser> users = sysUserService.list(wrapper);
        Map<String, Long> map = new HashMap<>();
        for (SysUser u : users) {
            map.putIfAbsent(u.getRealName().trim(), u.getId());
        }
        return map;
    }

    /**
     * 预加载 project_info 中已有的项目名称，用于插入前去重
     */
    private Set<String> loadExistingProjectNames() {
        QueryWrapper<ProjectInfo> wrapper = new QueryWrapper<>();
        wrapper.select("project_name");
        wrapper.isNotNull("project_name");
        wrapper.ne("project_name", "");
        List<ProjectInfo> list = projectInfoService.list(wrapper);
        Set<String> names = new HashSet<>();
        for (ProjectInfo p : list) {
            if (p.getProjectName() != null) {
                names.add(p.getProjectName().trim());
            }
        }
        return names;
    }

    private Map<String, Object> buildFailItem(ProjectImportRow row, String reason) {
        Map<String, Object> item = new HashMap<>();
        item.put("row", row.getRowNum());
        item.put("projectName", trim(row.getProjectName()));
        item.put("reason", reason);
        return item;
    }

    private boolean isBlankRow(ProjectImportRow row) {
        return isBlank(row.getProjectName())
                && isBlank(row.getProjectNo())
                && isBlank(row.getOwnerCompany())
                && isBlank(row.getOwnerContact())
                && isBlank(row.getOwnerPhone())
                && isBlank(row.getWinBidAmount())
                && isBlank(row.getProjectType())
                && isBlank(row.getProjectCycle())
                && isBlank(row.getProjectSource())
                && isBlank(row.getTechUserName());
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** 去除首尾空白，空白串归一为null */
    private String trim(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
