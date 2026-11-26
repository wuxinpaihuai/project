package com.zjhl.project.dto;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 现有项目批量导入行模型（对应模板表头，模板文件：/file/现有项目信息批量导入模板.xlsx）
 */
@Data
public class ProjectImportRow {

    /** 项目名称 → project_info.project_name */
    @ExcelProperty("项目名称")
    private String projectName;

    /** 项目编号 → project_info.project_no */
    @ExcelProperty("项目编号")
    private String projectNo;

    /** 业主单位 → project_info.owner_company */
    @ExcelProperty("业主单位")
    private String ownerCompany;

    /** 业主联系人 → project_info.owner_contact */
    @ExcelProperty("业主联系人")
    private String ownerContact;

    /** 业主联系方式 → project_info.owner_phone */
    @ExcelProperty("业主联系方式")
    private String ownerPhone;

    /** 合同金额（元） → project_extend.win_bid_amount */
    @ExcelProperty("合同金额（元）")
    private String winBidAmount;

    /** 项目类型 → project_info.project_type（填数字编码或名称，如：环评/场调/应急预案等） */
    @ExcelProperty("项目类型")
    private String projectType;

    /** 工期（天） → project_info.project_cycle */
    @ExcelProperty("工期（天）")
    private String projectCycle;

    /** 项目来源 → project_info.project_source */
    @ExcelProperty("项目来源")
    private String projectSource;

    /** 技术负责人（姓名，通过 sys_user.real_name 关联） → project_info.tech_user_name / tech_user_id */
    @ExcelProperty("技术负责人")
    private String techUserName;

    /** Excel 实际行号（表头为第1行），读取时由监听器回填 */
    @ExcelIgnore
    private Integer rowNum;

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getProjectNo() {
		return projectNo;
	}

	public void setProjectNo(String projectNo) {
		this.projectNo = projectNo;
	}

	public String getOwnerCompany() {
		return ownerCompany;
	}

	public void setOwnerCompany(String ownerCompany) {
		this.ownerCompany = ownerCompany;
	}

	public String getOwnerContact() {
		return ownerContact;
	}

	public void setOwnerContact(String ownerContact) {
		this.ownerContact = ownerContact;
	}

	public String getOwnerPhone() {
		return ownerPhone;
	}

	public void setOwnerPhone(String ownerPhone) {
		this.ownerPhone = ownerPhone;
	}

	public String getWinBidAmount() {
		return winBidAmount;
	}

	public void setWinBidAmount(String winBidAmount) {
		this.winBidAmount = winBidAmount;
	}

	public String getProjectType() {
		return projectType;
	}

	public void setProjectType(String projectType) {
		this.projectType = projectType;
	}

	public String getProjectCycle() {
		return projectCycle;
	}

	public void setProjectCycle(String projectCycle) {
		this.projectCycle = projectCycle;
	}

	public String getProjectSource() {
		return projectSource;
	}

	public void setProjectSource(String projectSource) {
		this.projectSource = projectSource;
	}

	public String getTechUserName() {
		return techUserName;
	}

	public void setTechUserName(String techUserName) {
		this.techUserName = techUserName;
	}

	public Integer getRowNum() {
		return rowNum;
	}

	public void setRowNum(Integer rowNum) {
		this.rowNum = rowNum;
	}
    
    
}
