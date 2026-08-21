package com.zjhl.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("project_income")
public class ProjectIncome {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联项目ID(project_info.id)
     */
    private Long projectId;

    /**
     * 到账金额
     */
    private BigDecimal receiveAmount;

    /**
     * 到账时间
     */
    private LocalDate receiveTime;

    /**
     * 付款单位名字
     */
    private String payerCompanyName;

    /**
     * 付款方式 1=银承，2=转账
     */
    private Integer payType;

    /**
     * 是否与合同节点一致 0=否，1=是
     */
    private Integer isMatchnode;

    /**
     * 收款节点id(关联sign_payment.id)
     */
    private Long paymentNodeId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getProjectId() {
		return projectId;
	}

	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	public BigDecimal getReceiveAmount() {
		return receiveAmount;
	}

	public void setReceiveAmount(BigDecimal receiveAmount) {
		this.receiveAmount = receiveAmount;
	}

	public LocalDate getReceiveTime() {
		return receiveTime;
	}

	public void setReceiveTime(LocalDate receiveTime) {
		this.receiveTime = receiveTime;
	}

	public String getPayerCompanyName() {
		return payerCompanyName;
	}

	public void setPayerCompanyName(String payerCompanyName) {
		this.payerCompanyName = payerCompanyName;
	}

	public Integer getPayType() {
		return payType;
	}

	public void setPayType(Integer payType) {
		this.payType = payType;
	}

	public Integer getIsMatchnode() {
		return isMatchnode;
	}

	public void setIsMatchnode(Integer isMatchnode) {
		this.isMatchnode = isMatchnode;
	}

	public Long getPaymentNodeId() {
		return paymentNodeId;
	}

	public void setPaymentNodeId(Long paymentNodeId) {
		this.paymentNodeId = paymentNodeId;
	}

	public LocalDateTime getCreateTime() {
		return createTime;
	}

	public void setCreateTime(LocalDateTime createTime) {
		this.createTime = createTime;
	}
    
    
}