package com.zjhl.project.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("project_extend")
public class ProjectExtend {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Integer isWinBid;
    private String winBidAmount;
    private Integer isSign;
    
    private LocalDateTime signEndTime;
    private LocalDate signTime;
    
    private Integer isReceiveMoney;
    private Integer isDeliver;
    private Integer isFinish;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String filePath;
    private String fileName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Integer getIsWinBid() { return isWinBid; }
    public void setIsWinBid(Integer isWinBid) { this.isWinBid = isWinBid; }
    public String getWinBidAmount() { return winBidAmount; }
    public void setWinBidAmount(String winBidAmount) { this.winBidAmount = winBidAmount; }
    public Integer getIsSign() { return isSign; }
    public void setIsSign(Integer isSign) { this.isSign = isSign; }
    public Integer getIsReceiveMoney() { return isReceiveMoney; }
    public void setIsReceiveMoney(Integer isReceiveMoney) { this.isReceiveMoney = isReceiveMoney; }
    public Integer getIsDeliver() { return isDeliver; }
    public void setIsDeliver(Integer isDeliver) { this.isDeliver = isDeliver; }
    public Integer getIsFinish() { return isFinish; }
    public void setIsFinish(Integer isFinish) { this.isFinish = isFinish; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public LocalDateTime getSignEndTime() {
		return signEndTime;
	}
	public void setSignEndTime(LocalDateTime signEndTime) {
		this.signEndTime = signEndTime;
	}
	public LocalDate getSignTime() {
		return signTime;
	}
	public void setSignTime(LocalDate signTime) {
		this.signTime = signTime;
	}
    
    
}
