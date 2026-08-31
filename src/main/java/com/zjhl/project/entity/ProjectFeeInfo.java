package com.zjhl.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("project_fee_info")
public class ProjectFeeInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联项目ID(project_info.id) */
    private Long projectId;

    /** 费用类型（1=监测费/工程直接成本，2=评审费，3=协作费/业务费，4=公示公告费，5=材料费，6=差旅费，7=装订费/快递费，8=其他直接费用） */
    private Integer feeType;

    /** 费用金额（元） */
    private BigDecimal feeAmount;

    /** 费用状态（0=未提交，1=已提交） */
    private Integer feeStatus;

    /** 报销人用户ID */
    private Long submitUserId;

    /** 报销人姓名 */
    private String submitUserName;

    /** 报销人手机号 */
    private String submitUserPhone;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Integer getFeeType() { return feeType; }
    public void setFeeType(Integer feeType) { this.feeType = feeType; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }
    public Integer getFeeStatus() { return feeStatus; }
    public void setFeeStatus(Integer feeStatus) { this.feeStatus = feeStatus; }
    public Long getSubmitUserId() { return submitUserId; }
    public void setSubmitUserId(Long submitUserId) { this.submitUserId = submitUserId; }
    public String getSubmitUserName() { return submitUserName; }
    public void setSubmitUserName(String submitUserName) { this.submitUserName = submitUserName; }
    public String getSubmitUserPhone() { return submitUserPhone; }
    public void setSubmitUserPhone(String submitUserPhone) { this.submitUserPhone = submitUserPhone; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
