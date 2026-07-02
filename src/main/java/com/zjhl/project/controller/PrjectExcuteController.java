package com.zjhl.project.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjhl.project.entity.*;
import com.zjhl.project.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/project/execute")
public class PrjectExcuteController {

    @Autowired
    private ProjectInfoService projectInfoService;

    @Autowired
    private ProjectExtendService projectExtendService;

    @Autowired
    private SignVisitService signVisitService;

    @Autowired
    private SignVisitFileService signVisitFileService;

    @Autowired
    private ProjectFileService projectFileService;

    @Autowired
    private SignMilestoneService signMilestoneService;

    @Autowired
    private ProjectTaskService projectTaskService;
    
    @Autowired
    private SysMessageService sysMessageService;
    
    @Autowired
    private  SysUserService   sysUserService;

    /**
     * 项目实施列表 - 查询已中标的项目（分页）
     */
    @GetMapping("/executeList")
    public Map<String, Object> executeList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String projectName,
            @RequestParam(required = false) Integer isSign) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        // 1. 查询已中标的项目ID
        QueryWrapper<ProjectExtend> extendWrapper = new QueryWrapper<>();
        extendWrapper.eq("is_win_bid", 1);
        if (isSign != null) {
            extendWrapper.eq("is_sign", isSign);
        }
        List<ProjectExtend> extend = projectExtendService.list(extendWrapper);
        List<Long> projectIds = extend.stream().map(ProjectExtend::getProjectId).collect(Collectors.toList());

        if (projectIds.isEmpty()) {
            result.put("code", 200);
            result.put("msg", "查询成功");
            result.put("total", 0);
            result.put("records", Collections.EMPTY_LIST);
            return result;
        }

        // 2. 查询项目信息
        QueryWrapper<ProjectInfo> wrapper = new QueryWrapper<>();
        wrapper.in("id", projectIds);
        if (projectName != null && !projectName.isEmpty()) {
            wrapper.like("project_name", projectName);
        }
        wrapper.orderByDesc("create_time");

        Page<ProjectInfo> page = new Page<>(pageNum, pageSize);
        Page<ProjectInfo> resultPage = projectInfoService.page(page, wrapper);

        // 3. 为每条记录拼接extend信息
        List<Map<String, Object>> records = new ArrayList<>();
        for (ProjectInfo info : resultPage.getRecords()) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", info.getId());
            row.put("projectName", info.getProjectName());
            row.put("projectNo", info.getProjectNo());
            row.put("area", info.getArea());
            row.put("projectAmount", info.getProjectAmount());
            row.put("bisnessUserName", info.getBisnessUserName());
            row.put("bisnessUserPhone", info.getBisnessUserPhone());

            // 查找对应的extend
            ProjectExtend ext = extend.stream()
                    .filter(e -> e.getProjectId().equals(info.getId()))
                    .findFirst().orElse(null);
            if (ext != null) {
                row.put("winBidAmount", ext.getWinBidAmount());
                row.put("fileName", ext.getFileName());
                row.put("filePath", ext.getFilePath());
                row.put("isSign", ext.getIsSign());
            } else {
                row.put("winBidAmount", null);
                row.put("fileName", null);
                row.put("filePath", null);
                row.put("isSign", 0);
            }
            records.add(row);
        }

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("total", resultPage.getTotal());
        result.put("records", records);
        return result;
    }

    /**
     * 项目实施详情 - 返回项目信息、拜访记录、签约信息、里程碑、实施任务
     */
    @GetMapping("/executeDetail/{projectId}")
    public Map<String, Object> executeDetail(@PathVariable Long projectId) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        // 1. 项目基本信息
        ProjectInfo project = projectInfoService.getById(projectId);
        if (project == null) {
            result.put("code", 404);
            result.put("msg", "项目不存在");
            return result;
        }
        result.put("project", project);

        // 2. project_extend
        QueryWrapper<ProjectExtend> extendWrapper = new QueryWrapper<>();
        extendWrapper.eq("project_id", projectId);
        ProjectExtend extend = projectExtendService.getOne(extendWrapper);
        result.put("extend", extend);

        // 3. 拜访记录（按开始时间倒序）
        QueryWrapper<SignVisit> visitWrapper = new QueryWrapper<>();
        visitWrapper.eq("project_id", projectId);
        visitWrapper.orderByDesc("visit_start_time");
        List<SignVisit> visits = signVisitService.list(visitWrapper);

        List<Map<String, Object>> visitList = new ArrayList<>();
        for (SignVisit v : visits) {
            Map<String, Object> visitMap = new HashMap<>();
            visitMap.put("id", v.getId());
            visitMap.put("visitTarget", v.getVisitTarget());
            visitMap.put("communicateContent", v.getCommunicateContent());
            visitMap.put("visitStartTime", v.getVisitStartTime() != null ? v.getVisitStartTime().toString() : null);
            visitMap.put("visitEndTime", v.getVisitEndTime() != null ? v.getVisitEndTime().toString() : null);

            // 查询附件
            QueryWrapper<SignVisitFile> fileWrapper = new QueryWrapper<>();
            fileWrapper.eq("visit_id", v.getId());
            List<SignVisitFile> files = signVisitFileService.list(fileWrapper);
            visitMap.put("files", files);

            visitList.add(visitMap);
        }
        result.put("visits", visitList);

        // 4. 合同附件（stage_type=2）
        QueryWrapper<ProjectFile> contractFileWrapper = new QueryWrapper<>();
        contractFileWrapper.eq("project_id", projectId).eq("stage_type", 2);
        List<ProjectFile> contractFiles = projectFileService.list(contractFileWrapper);
        result.put("contractFiles", contractFiles);

        // 5. 里程碑节点
        QueryWrapper<SignMilestone> milestoneWrapper = new QueryWrapper<>();
        milestoneWrapper.eq("project_id", projectId);
        milestoneWrapper.orderByAsc("expect_finish_date");
        List<SignMilestone> milestones = signMilestoneService.list(milestoneWrapper);
        result.put("milestones", milestones);

        // 6. 实施任务列表（stage_type=3）
        QueryWrapper<ProjectTask> taskWrapper = new QueryWrapper<>();
        taskWrapper.eq("project_id", projectId).eq("stage_type", 3);
        taskWrapper.orderByAsc("task_end_time");
        List<ProjectTask> tasks = projectTaskService.list(taskWrapper);
        result.put("tasks", tasks);

        result.put("code", 200);
        result.put("msg", "查询成功");
        return result;
    }

    /**
     * 新增实施任务
     */
    @PostMapping("/addTask")
    public Map<String, Object> addTask(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        Long projectId = Long.parseLong(params.get("projectId").toString());
        ProjectTask task = new ProjectTask();
        task.setProjectId(projectId);
        task.setStageType(3); // 实施阶段
        
        // 设置当前登录用户为任务分配人
        Long loginUserId = StpUtil.getLoginIdAsLong();
        SysUser loginUser = sysUserService.getById(loginUserId);
        if (loginUser != null) {
            task.setAssignUserId(loginUser.getId());
            task.setAssignUserName(loginUser.getRealName());
            task.setAssignUserPhone(loginUser.getPhone());
        }
        
        task.setExecUserName((String) params.get("execUserName"));
        task.setTaskContent((String) params.get("taskContent"));
        task.setWorkAmount((String) params.get("workAmount"));
        task.setWorkContent((String) params.get("workContent"));
        task.setRelateUserIds((String) params.get("relateUserIds"));
        task.setNeedCar(params.get("needCar") != null ? Integer.parseInt(params.get("needCar").toString()) : 0);
        task.setTollFee(params.get("tollFee") != null ? new java.math.BigDecimal(params.get("tollFee").toString()) : java.math.BigDecimal.ZERO);
        task.setMileage(params.get("mileage") != null ? new java.math.BigDecimal(params.get("mileage").toString()) : java.math.BigDecimal.ZERO);
        task.setTaskStatus(0); // 未开始

        // 任务截止时间
        String taskEndTimeStr = (String) params.get("taskEndTime");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (taskEndTimeStr != null && !taskEndTimeStr.isEmpty()) {
            if (taskEndTimeStr.length() == 10) {
                taskEndTimeStr += " 00:00:00";
            }
            task.setTaskEndTime(LocalDateTime.parse(taskEndTimeStr, formatter));
        }

        task.setCreateTime(LocalDateTime.now());
        projectTaskService.save(task);

        result.put("code", 200);
        result.put("msg", "新增成功");
        result.put("taskId", task.getId());
        return result;
    }

    /**
     * 编辑实施任务
     */
    @PostMapping("/editTask")
    public Map<String, Object> editTask(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        Long taskId = Long.parseLong(params.get("id").toString());
        ProjectTask task = projectTaskService.getById(taskId);
        if (task == null) {
            result.put("code", 404);
            result.put("msg", "任务不存在");
            return result;
        }

     // 设置当前登录用户为任务分配人
        Long loginUserId = StpUtil.getLoginIdAsLong();
        SysUser loginUser = sysUserService.getById(loginUserId);
        if (loginUser != null) {
            task.setAssignUserId(loginUser.getId());
            task.setAssignUserName(loginUser.getRealName());
            task.setAssignUserPhone(loginUser.getPhone());
        }
        task.setExecUserName((String) params.get("execUserName"));
        task.setTaskContent((String) params.get("taskContent"));
        task.setWorkAmount((String) params.get("workAmount"));
        task.setWorkContent((String) params.get("workContent"));
        task.setRelateUserIds((String) params.get("relateUserIds"));
        task.setNeedCar(params.get("needCar") != null ? Integer.parseInt(params.get("needCar").toString()) : 0);
        task.setTollFee(params.get("tollFee") != null ? new java.math.BigDecimal(params.get("tollFee").toString()) : java.math.BigDecimal.ZERO);
        task.setMileage(params.get("mileage") != null ? new java.math.BigDecimal(params.get("mileage").toString()) : java.math.BigDecimal.ZERO);

        // 任务截止时间
        String taskEndTimeStr = (String) params.get("taskEndTime");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (taskEndTimeStr != null && !taskEndTimeStr.isEmpty()) {
            if (taskEndTimeStr.length() == 10) {
                taskEndTimeStr += " 00:00:00";
            }
            task.setTaskEndTime(LocalDateTime.parse(taskEndTimeStr, formatter));
        } else {
            task.setTaskEndTime(null);
        }

        projectTaskService.updateById(task);

        result.put("code", 200);
        result.put("msg", "编辑成功");
        return result;
    }

    /**
     * 删除实施任务
     */
    @PostMapping("/deleteTask/{taskId}")
    public Map<String, Object> deleteTask(@PathVariable Long taskId) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        projectTaskService.removeById(taskId);

        result.put("code", 200);
        result.put("msg", "删除成功");
        return result;
    }
    
    /**
     * 任务发布 - 将该项目所有未开始(task_status=0)的实施任务更新为执行中(task_status=1)
     */
    @PostMapping("/publishTask/{projectId}")
    public Map<String, Object> publishTask(@PathVariable Long projectId) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        QueryWrapper<ProjectTask> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId).eq("stage_type", 3).eq("task_status", 0);
        List<ProjectTask> tasks = projectTaskService.list(wrapper);

        if (tasks.isEmpty()) {
            result.put("code", 200);
            result.put("msg", "没有需要发布的任务");
            result.put("count", 0);
            return result;
        }
        ProjectInfo  projectInfo = projectInfoService.getById(projectId);
        String projectName = projectInfo.getProjectName();

        for (ProjectTask task : tasks) {
            task.setTaskStatus(1); // 执行中
            projectTaskService.updateById(task);
            //需要发消息
            SysMessage sms = new SysMessage();
            sms.setContent("您需要在"+ task.getTaskEndTime()+"前，完成"+ projectName +" 工作：" + task.getTaskContent() );
            sms.setReadStatus(0);//未读
            sms.setMsgType(1);//任务提醒
            sms.setTaskId(task.getId());
            sms.setProjectId(task.getProjectId());
            sms.setReceiveUserId(task.getExecUserId());//责任人
            sms.setCreateTime(LocalDateTime.now());
            if(task.getRelateUserIds() != null && task.getRelateUserIds().split(",").length > 0) {
            	List<SysMessage> smsList = new ArrayList<>();
            	smsList.add(sms);
            	for (String id :task.getRelateUserIds().split(",")) {
            		SysMessage s = new SysMessage();
            		 s.setContent("项目："+ projectName +" 已为："+ task.getAssignUserName()+" 分配工作：" + task.getTaskContent() );
                     s.setReadStatus(0);//未读
                     s.setMsgType(2);//消息通知
                     s.setTaskId(task.getId());
                     s.setProjectId(task.getProjectId());
                     s.setReceiveUserId(Long.parseLong(id));//关联领导
                     s.setCreateTime(LocalDateTime.now());
                     smsList.add(s);
				}
            	sysMessageService.saveBatch(smsList);
            }
            else {
            	sysMessageService.save(sms);
			}
            
        }

        result.put("code", 200);
        result.put("msg", "发布成功");
        result.put("count", tasks.size());
        return result;
    }

    /**
     * 任务延期 - 更新耗时和截止时间，状态改为延时执行中(task_status=2)
     */
    @PostMapping("/delayTask")
    public Map<String, Object> delayTask(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        Long taskId = Long.parseLong(params.get("id").toString());
        ProjectTask task = projectTaskService.getById(taskId);
        if (task == null) {
            result.put("code", 404);
            result.put("msg", "任务不存在");
            return result;
        }

        // 只更新耗时和截止时间
        task.setWorkAmount((String) params.get("workAmount"));

        String taskEndTimeStr = (String) params.get("taskEndTime");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (taskEndTimeStr != null && !taskEndTimeStr.isEmpty()) {
            if (taskEndTimeStr.length() == 10) {
                taskEndTimeStr += " 00:00:00";
            }
            task.setTaskEndTime(LocalDateTime.parse(taskEndTimeStr, formatter));
        }

        task.setTaskStatus(2); // 延时执行中
        projectTaskService.updateById(task);

        result.put("code", 200);
        result.put("msg", "延期处理成功");
        return result;
    }
}
