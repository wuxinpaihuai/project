package com.zjhl.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("project_info")
public class ProjectInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String projectName;
    private String projectNo;
    private Integer projectType;
    private Integer bidType;
    private Integer bidWay;
    private String area;
    private String projectSource;
    private String ownerCompany;
    private String ownerContact;
    private String ownerPhone;
    private String ownerPost;
    private String bidWebsite;
    private String bidUrl;
    private Integer priceType;
    private Integer projectCycle;
    private BigDecimal projectAmount;
    private BigDecimal maxPrice;
    private BigDecimal minPrice;
    private String priceUnit;
    private String scoreMethod;
    private BigDecimal bidDocumentFee;
    private BigDecimal platformFee;
    private BigDecimal agencyFee;
    private BigDecimal bidDeposit;
    private BigDecimal contractDeposit;
    private LocalDateTime questionEndTime;
    private LocalDateTime bidTime;
    private String qualificationRequire;
    private BigDecimal documentBindingFee;
    private String remark;
    private String contentDetail;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private Long bisnessUserId;
    private String bisnessUserName;
    private String bisnessUserPhone;
    
    /** 项目状态（非数据库字段，来自project_stage.stage_status） */
    @TableField(exist = false)
    private Integer stageStatus;
    
    @TableField(exist = false)
    private Integer stageType;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getProjectNo() { return projectNo; }
    public void setProjectNo(String projectNo) { this.projectNo = projectNo; }
    public Integer getProjectType() { return projectType; }
    public void setProjectType(Integer projectType) { this.projectType = projectType; }
    public Integer getBidType() { return bidType; }
    public void setBidType(Integer bidType) { this.bidType = bidType; }
    public Integer getBidWay() { return bidWay; }
    public void setBidWay(Integer bidWay) { this.bidWay = bidWay; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public String getProjectSource() { return projectSource; }
    public void setProjectSource(String projectSource) { this.projectSource = projectSource; }
    public String getOwnerCompany() { return ownerCompany; }
    public void setOwnerCompany(String ownerCompany) { this.ownerCompany = ownerCompany; }
    public String getOwnerContact() { return ownerContact; }
    public void setOwnerContact(String ownerContact) { this.ownerContact = ownerContact; }
    public String getOwnerPhone() { return ownerPhone; }
    public void setOwnerPhone(String ownerPhone) { this.ownerPhone = ownerPhone; }
    public String getOwnerPost() { return ownerPost; }
    public void setOwnerPost(String ownerPost) { this.ownerPost = ownerPost; }
    public String getBidWebsite() { return bidWebsite; }
    public void setBidWebsite(String bidWebsite) { this.bidWebsite = bidWebsite; }
    public String getBidUrl() { return bidUrl; }
    public void setBidUrl(String bidUrl) { this.bidUrl = bidUrl; }
    public Integer getPriceType() { return priceType; }
    public void setPriceType(Integer priceType) { this.priceType = priceType; }
    public Integer getProjectCycle() { return projectCycle; }
    public void setProjectCycle(Integer projectCycle) { this.projectCycle = projectCycle; }
    public BigDecimal getProjectAmount() { return projectAmount; }
    public void setProjectAmount(BigDecimal projectAmount) { this.projectAmount = projectAmount; }
    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }
    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }
    public String getPriceUnit() { return priceUnit; }
    public void setPriceUnit(String priceUnit) { this.priceUnit = priceUnit; }
    public String getScoreMethod() { return scoreMethod; }
    public void setScoreMethod(String scoreMethod) { this.scoreMethod = scoreMethod; }
    public BigDecimal getBidDocumentFee() { return bidDocumentFee; }
    public void setBidDocumentFee(BigDecimal bidDocumentFee) { this.bidDocumentFee = bidDocumentFee; }
    public BigDecimal getPlatformFee() { return platformFee; }
    public void setPlatformFee(BigDecimal platformFee) { this.platformFee = platformFee; }
    public BigDecimal getAgencyFee() { return agencyFee; }
    public void setAgencyFee(BigDecimal agencyFee) { this.agencyFee = agencyFee; }
    public BigDecimal getBidDeposit() { return bidDeposit; }
    public void setBidDeposit(BigDecimal bidDeposit) { this.bidDeposit = bidDeposit; }
    public BigDecimal getContractDeposit() { return contractDeposit; }
    public void setContractDeposit(BigDecimal contractDeposit) { this.contractDeposit = contractDeposit; }
    public LocalDateTime getQuestionEndTime() { return questionEndTime; }
    public void setQuestionEndTime(LocalDateTime questionEndTime) { this.questionEndTime = questionEndTime; }
    public LocalDateTime getBidTime() { return bidTime; }
    public void setBidTime(LocalDateTime bidTime) { this.bidTime = bidTime; }
    public String getQualificationRequire() { return qualificationRequire; }
    public void setQualificationRequire(String qualificationRequire) { this.qualificationRequire = qualificationRequire; }
    public BigDecimal getDocumentBindingFee() { return documentBindingFee; }
    public void setDocumentBindingFee(BigDecimal documentBindingFee) { this.documentBindingFee = documentBindingFee; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getContentDetail() { return contentDetail; }
    public void setContentDetail(String contentDetail) { this.contentDetail = contentDetail; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getStageStatus() { return stageStatus; }
    public void setStageStatus(Integer stageStatus) { this.stageStatus = stageStatus; }
	public Integer getStageType() {
		return stageType;
	}
	public void setStageType(Integer stageType) {
		this.stageType = stageType;
	}
	public Long getBisnessUserId() {
		return bisnessUserId;
	}
	public void setBisnessUserId(Long bisnessUserId) {
		this.bisnessUserId = bisnessUserId;
	}
	public String getBisnessUserName() {
		return bisnessUserName;
	}
	public void setBisnessUserName(String bisnessUserName) {
		this.bisnessUserName = bisnessUserName;
	}
	public String getBisnessUserPhone() {
		return bisnessUserPhone;
	}
	public void setBisnessUserPhone(String bisnessUserPhone) {
		this.bisnessUserPhone = bisnessUserPhone;
	}
    
    
}
