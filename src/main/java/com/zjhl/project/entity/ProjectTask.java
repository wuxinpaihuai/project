package com.zjhl.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("project_task")
public class ProjectTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Integer stageType;
    private Long assignUserId;
    private String assignUserName;
    private String assignUserPhone;
    private Integer taskType;
    private String taskContent;
    private Long execUserId;
    private String execUserName;
    private String execUserPhone;
    private Integer needCar;
    private BigDecimal tollFee;
    private BigDecimal mileage;
    private LocalDateTime taskStartTime;
    private LocalDateTime taskEndTime;
    private LocalDateTime createTime;
    private Integer deliverType;
    private Integer taskStatus;
    private LocalDateTime execStartTime;
    private LocalDateTime execFinishTime;
    private String taskDesc;
    private String remark;
    
    private String workContent;
    private String relateUserIds;
    private String workAmount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Integer getStageType() { return stageType; }
    public void setStageType(Integer stageType) { this.stageType = stageType; }
    public Long getAssignUserId() { return assignUserId; }
    public void setAssignUserId(Long assignUserId) { this.assignUserId = assignUserId; }
    public String getAssignUserName() { return assignUserName; }
    public void setAssignUserName(String assignUserName) { this.assignUserName = assignUserName; }
    public String getAssignUserPhone() { return assignUserPhone; }
    public void setAssignUserPhone(String assignUserPhone) { this.assignUserPhone = assignUserPhone; }
    public Integer getTaskType() { return taskType; }
    public void setTaskType(Integer taskType) { this.taskType = taskType; }
    public String getTaskContent() { return taskContent; }
    public void setTaskContent(String taskContent) { this.taskContent = taskContent; }
    public Long getExecUserId() { return execUserId; }
    public void setExecUserId(Long execUserId) { this.execUserId = execUserId; }
    public String getExecUserName() { return execUserName; }
    public void setExecUserName(String execUserName) { this.execUserName = execUserName; }
    public String getExecUserPhone() { return execUserPhone; }
    public void setExecUserPhone(String execUserPhone) { this.execUserPhone = execUserPhone; }
    public Integer getNeedCar() { return needCar; }
    public void setNeedCar(Integer needCar) { this.needCar = needCar; }
    public BigDecimal getTollFee() { return tollFee; }
    public void setTollFee(BigDecimal tollFee) { this.tollFee = tollFee; }
    public BigDecimal getMileage() { return mileage; }
    public void setMileage(BigDecimal mileage) { this.mileage = mileage; }
    public LocalDateTime getTaskStartTime() { return taskStartTime; }
    public void setTaskStartTime(LocalDateTime taskStartTime) { this.taskStartTime = taskStartTime; }
    public LocalDateTime getTaskEndTime() { return taskEndTime; }
    public void setTaskEndTime(LocalDateTime taskEndTime) { this.taskEndTime = taskEndTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public Integer getDeliverType() { return deliverType; }
    public void setDeliverType(Integer deliverType) { this.deliverType = deliverType; }
    public Integer getTaskStatus() { return taskStatus; }
    public void setTaskStatus(Integer taskStatus) { this.taskStatus = taskStatus; }
    public LocalDateTime getExecStartTime() { return execStartTime; }
    public void setExecStartTime(LocalDateTime execStartTime) { this.execStartTime = execStartTime; }
    public LocalDateTime getExecFinishTime() { return execFinishTime; }
    public void setExecFinishTime(LocalDateTime execFinishTime) { this.execFinishTime = execFinishTime; }
    public String getTaskDesc() { return taskDesc; }
    public void setTaskDesc(String taskDesc) { this.taskDesc = taskDesc; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
	public String getWorkContent() {
		return workContent;
	}
	public void setWorkContent(String workContent) {
		this.workContent = workContent;
	}
	public String getRelateUserIds() {
		return relateUserIds;
	}
	public void setRelateUserIds(String relateUserIds) {
		this.relateUserIds = relateUserIds;
	}
	public String getWorkAmount() {
		return workAmount;
	}
	public void setWorkAmount(String workAmount) {
		this.workAmount = workAmount;
	}
    
}
