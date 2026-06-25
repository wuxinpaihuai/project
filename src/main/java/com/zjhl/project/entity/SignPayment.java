package com.zjhl.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("sign_payment")
public class SignPayment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String paymentNode;
    private BigDecimal receiveAmount;
    private BigDecimal paymentRate;
    private LocalDate expectPayDate;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getPaymentNode() { return paymentNode; }
    public void setPaymentNode(String paymentNode) { this.paymentNode = paymentNode; }
    public BigDecimal getReceiveAmount() { return receiveAmount; }
    public void setReceiveAmount(BigDecimal receiveAmount) { this.receiveAmount = receiveAmount; }
    public BigDecimal getPaymentRate() { return paymentRate; }
    public void setPaymentRate(BigDecimal paymentRate) { this.paymentRate = paymentRate; }
    public LocalDate getExpectPayDate() { return expectPayDate; }
    public void setExpectPayDate(LocalDate expectPayDate) { this.expectPayDate = expectPayDate; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
