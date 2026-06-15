package com.zjhl.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("project_bid_result")
public class ProjectBidResult {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String bidCompanyName;
    private String bidPrice;
    private BigDecimal priceScore;
    private BigDecimal businessScore;
    private BigDecimal techScore;
    private BigDecimal finalScore;
    private Integer finalRank;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getBidCompanyName() { return bidCompanyName; }
    public void setBidCompanyName(String bidCompanyName) { this.bidCompanyName = bidCompanyName; }
    public String getBidPrice() { return bidPrice; }
    public void setBidPrice(String bidPrice) { this.bidPrice = bidPrice; }
    public BigDecimal getPriceScore() { return priceScore; }
    public void setPriceScore(BigDecimal priceScore) { this.priceScore = priceScore; }
    public BigDecimal getBusinessScore() { return businessScore; }
    public void setBusinessScore(BigDecimal businessScore) { this.businessScore = businessScore; }
    public BigDecimal getTechScore() { return techScore; }
    public void setTechScore(BigDecimal techScore) { this.techScore = techScore; }
    public BigDecimal getFinalScore() { return finalScore; }
    public void setFinalScore(BigDecimal finalScore) { this.finalScore = finalScore; }
    public Integer getFinalRank() { return finalRank; }
    public void setFinalRank(Integer finalRank) { this.finalRank = finalRank; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
