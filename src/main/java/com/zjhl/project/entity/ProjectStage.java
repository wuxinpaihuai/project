package com.zjhl.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("project_stage")
public class ProjectStage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Integer stageType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer stageStatus;
    private String stageRemark;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Integer getStageType() { return stageType; }
    public void setStageType(Integer stageType) { this.stageType = stageType; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public Integer getStageStatus() { return stageStatus; }
    public void setStageStatus(Integer stageStatus) { this.stageStatus = stageStatus; }
    public String getStageRemark() { return stageRemark; }
    public void setStageRemark(String stageRemark) { this.stageRemark = stageRemark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
