package com.zjhl.project.entity;

import java.math.BigDecimal;
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
    private BigDecimal contractAmount;

    /**
     * 一审用户ID
     */
    private Long ysUserId;

    /**
     * 一审姓名
     */
    private String ysUserName;

    /**
     * 二审用户ID
     */
    private Long esUserId;

    /**
     * 二审姓名
     */
    private String esUserName;

    /**
     * 三审用户ID
     */
    private Long ssUserId;

    /**
     * 三审姓名
     */
    private String ssUserName;
    
    /**
     * 项目考核类型（ 1=报告书 ，7=报告表，8=登记表,2=场调,3=应急预案,4=验收,5=环评入围,6=场调入围,99=其他
     */
    private Integer assessType;

    /**
     * 监测费
     */
    private BigDecimal monitorFee;

    /**
     * 评审费
     */
    private BigDecimal reviewFee;

    /**
     * 协作费/业务费
     */
    private BigDecimal cooperationFee;

    /**
     * 公示公告费
     */
    private BigDecimal publicNoticeFee;

    /**
     * 材料费
     */
    private BigDecimal materialFee;

    /**
     * 差旅费
     */
    private BigDecimal travelFee;

    /**
     * 装订费/快递费
     */
    private BigDecimal bindExpressFee;

    /**
     * 其他直接费用
     */
    private BigDecimal otherDirectFee;

    /**
     * 是否通过政府部门审批或备案 0=否 1=是
     */
    private Integer isGovernmentApprove;

    /**
     * 过会审批(0=一次性过会、1=多次过会、2=其他等)
     */
    private Integer meetingApprove;

    /**
     * 存档是否完成 0=否 1=是
     */
    private Integer isArchiveFinish;

    /**
     * 进展情况
     */
    private String progressInfo;
    
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
	public BigDecimal getContractAmount() {
		return contractAmount;
	}
	public void setContractAmount(BigDecimal contractAmount) {
		this.contractAmount = contractAmount;
	}
	public Long getYsUserId() {
		return ysUserId;
	}
	public void setYsUserId(Long ysUserId) {
		this.ysUserId = ysUserId;
	}
	public String getYsUserName() {
		return ysUserName;
	}
	public void setYsUserName(String ysUserName) {
		this.ysUserName = ysUserName;
	}
	public Long getEsUserId() {
		return esUserId;
	}
	public void setEsUserId(Long esUserId) {
		this.esUserId = esUserId;
	}
	public String getEsUserName() {
		return esUserName;
	}
	public void setEsUserName(String esUserName) {
		this.esUserName = esUserName;
	}
	public Long getSsUserId() {
		return ssUserId;
	}
	public void setSsUserId(Long ssUserId) {
		this.ssUserId = ssUserId;
	}
	public String getSsUserName() {
		return ssUserName;
	}
	public void setSsUserName(String ssUserName) {
		this.ssUserName = ssUserName;
	}
	public Integer getAssessType() {
		return assessType;
	}
	public void setAssessType(Integer assessType) {
		this.assessType = assessType;
	}
	public BigDecimal getMonitorFee() {
		return monitorFee;
	}
	public void setMonitorFee(BigDecimal monitorFee) {
		this.monitorFee = monitorFee;
	}
	public BigDecimal getReviewFee() {
		return reviewFee;
	}
	public void setReviewFee(BigDecimal reviewFee) {
		this.reviewFee = reviewFee;
	}
	public BigDecimal getCooperationFee() {
		return cooperationFee;
	}
	public void setCooperationFee(BigDecimal cooperationFee) {
		this.cooperationFee = cooperationFee;
	}
	public BigDecimal getPublicNoticeFee() {
		return publicNoticeFee;
	}
	public void setPublicNoticeFee(BigDecimal publicNoticeFee) {
		this.publicNoticeFee = publicNoticeFee;
	}
	public BigDecimal getMaterialFee() {
		return materialFee;
	}
	public void setMaterialFee(BigDecimal materialFee) {
		this.materialFee = materialFee;
	}
	public BigDecimal getTravelFee() {
		return travelFee;
	}
	public void setTravelFee(BigDecimal travelFee) {
		this.travelFee = travelFee;
	}
	public BigDecimal getBindExpressFee() {
		return bindExpressFee;
	}
	public void setBindExpressFee(BigDecimal bindExpressFee) {
		this.bindExpressFee = bindExpressFee;
	}
	public BigDecimal getOtherDirectFee() {
		return otherDirectFee;
	}
	public void setOtherDirectFee(BigDecimal otherDirectFee) {
		this.otherDirectFee = otherDirectFee;
	}
	public Integer getIsGovernmentApprove() {
		return isGovernmentApprove;
	}
	public void setIsGovernmentApprove(Integer isGovernmentApprove) {
		this.isGovernmentApprove = isGovernmentApprove;
	}
	public Integer getMeetingApprove() {
		return meetingApprove;
	}
	public void setMeetingApprove(Integer meetingApprove) {
		this.meetingApprove = meetingApprove;
	}
	public Integer getIsArchiveFinish() {
		return isArchiveFinish;
	}
	public void setIsArchiveFinish(Integer isArchiveFinish) {
		this.isArchiveFinish = isArchiveFinish;
	}
	public String getProgressInfo() {
		return progressInfo;
	}
	public void setProgressInfo(String progressInfo) {
		this.progressInfo = progressInfo;
	}
    
    
}
