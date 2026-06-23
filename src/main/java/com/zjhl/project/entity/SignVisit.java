package com.zjhl.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sign_visit")
public class SignVisit {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String visitTarget;
    private String communicateContent;
    private LocalDateTime visitStartTime;
    private LocalDateTime visitEndTime;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getVisitTarget() { return visitTarget; }
    public void setVisitTarget(String visitTarget) { this.visitTarget = visitTarget; }
    public String getCommunicateContent() { return communicateContent; }
    public void setCommunicateContent(String communicateContent) { this.communicateContent = communicateContent; }
    public LocalDateTime getVisitStartTime() { return visitStartTime; }
    public void setVisitStartTime(LocalDateTime visitStartTime) { this.visitStartTime = visitStartTime; }
    public LocalDateTime getVisitEndTime() { return visitEndTime; }
    public void setVisitEndTime(LocalDateTime visitEndTime) { this.visitEndTime = visitEndTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
