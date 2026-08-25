package com.zjhl.project.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 项目考核人员分配表 project_assessment_user
 */
@Data
@TableName("project_assessment_user")
public class ProjectAssessmentUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 项目id
     */
    private Long projectId;

    /**
     * 人员id
     */
    private Long userId;

    /**
     * 人员姓名
     */
    private String userName;

    /**
     * 占比(数值，20代表20%)
     */
    private BigDecimal ratio;

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

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public BigDecimal getRatio() {
		return ratio;
	}

	public void setRatio(BigDecimal ratio) {
		this.ratio = ratio;
	}

	public LocalDateTime getCreateTime() {
		return createTime;
	}

	public void setCreateTime(LocalDateTime createTime) {
		this.createTime = createTime;
	}
    
    
}
