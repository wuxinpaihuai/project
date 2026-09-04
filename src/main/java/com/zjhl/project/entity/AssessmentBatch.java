package com.zjhl.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("assessment_batch")
public class AssessmentBatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 批次名称 */
    private String batchName;

    /** 考核开始日期 */
    private LocalDate assessBeginDay;

    /** 考核结束日期 */
    private LocalDate assessEndDay;

    /** 备注 */
    private String remark;

    /** 考核状态 1=未开始，2=统计中，3=已完成，4=统计失败 */
    private Integer assessStatus;

    /** 考核统计结果 */
    private String assessResult;

    /** 创建时间 */
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBatchName() { return batchName; }
    public void setBatchName(String batchName) { this.batchName = batchName; }
    public LocalDate getAssessBeginDay() { return assessBeginDay; }
    public void setAssessBeginDay(LocalDate assessBeginDay) { this.assessBeginDay = assessBeginDay; }
    public LocalDate getAssessEndDay() { return assessEndDay; }
    public void setAssessEndDay(LocalDate assessEndDay) { this.assessEndDay = assessEndDay; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Integer getAssessStatus() { return assessStatus; }
    public void setAssessStatus(Integer assessStatus) { this.assessStatus = assessStatus; }
    public String getAssessResult() { return assessResult; }
    public void setAssessResult(String assessResult) { this.assessResult = assessResult; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
