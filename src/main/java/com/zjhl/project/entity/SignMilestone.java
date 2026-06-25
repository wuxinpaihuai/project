package com.zjhl.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("sign_milestone")
public class SignMilestone {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String milestoneName;
    private LocalDate expectFinishDate;
    private String milestoneDesc;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getMilestoneName() { return milestoneName; }
    public void setMilestoneName(String milestoneName) { this.milestoneName = milestoneName; }
    public LocalDate getExpectFinishDate() { return expectFinishDate; }
    public void setExpectFinishDate(LocalDate expectFinishDate) { this.expectFinishDate = expectFinishDate; }
    public String getMilestoneDesc() { return milestoneDesc; }
    public void setMilestoneDesc(String milestoneDesc) { this.milestoneDesc = milestoneDesc; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
