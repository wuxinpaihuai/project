package com.zjhl.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 水利站点信息表 water_station_data 对应实体
 */
@Data
@TableName("water_station_data")
public class WaterStationData {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 数据日期（yyyy-MM-dd）。
     * DATE 列本身不含时间，用 Date 承载时零点落在东八区（UTC+8），
     * 若按 UTC 序列化会变成前一天 16:00，前端截取日期就会出现"少一天"。
     * 故必须显式指定 GMT+8。
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date dataDay;

    /** 站名 */
    private String stationName;

    /** 站号 */
    private String stationCode;

    /** 水位（m） */
    private BigDecimal waterLevel;

    /** 汛限水位（m） */
    private BigDecimal waterLimitLevel;

    /** 是否超汛限：1=是，0=否 */
    private Integer isOut;

    /** 经度 */
    private BigDecimal longitude;

    /** 纬度 */
    private BigDecimal latitude;

    /** 省 */
    private String province;

    /** 市 */
    private String city;

    /** 区县 */
    private String county;

    /** 上报时间 */
    private Date reportTime;

    /** 创建时间（数据库默认值填充） */
    private Date createTime;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getDataDay() {
		return dataDay;
	}

	public void setDataDay(Date dataDay) {
		this.dataDay = dataDay;
	}

	public String getStationName() {
		return stationName;
	}

	public void setStationName(String stationName) {
		this.stationName = stationName;
	}

	public String getStationCode() {
		return stationCode;
	}

	public void setStationCode(String stationCode) {
		this.stationCode = stationCode;
	}

	public BigDecimal getWaterLevel() {
		return waterLevel;
	}

	public void setWaterLevel(BigDecimal waterLevel) {
		this.waterLevel = waterLevel;
	}

	public BigDecimal getWaterLimitLevel() {
		return waterLimitLevel;
	}

	public void setWaterLimitLevel(BigDecimal waterLimitLevel) {
		this.waterLimitLevel = waterLimitLevel;
	}

	public Integer getIsOut() {
		return isOut;
	}

	public void setIsOut(Integer isOut) {
		this.isOut = isOut;
	}

	public BigDecimal getLongitude() {
		return longitude;
	}

	public void setLongitude(BigDecimal longitude) {
		this.longitude = longitude;
	}

	public BigDecimal getLatitude() {
		return latitude;
	}

	public void setLatitude(BigDecimal latitude) {
		this.latitude = latitude;
	}

	public String getProvince() {
		return province;
	}

	public void setProvince(String province) {
		this.province = province;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getCounty() {
		return county;
	}

	public void setCounty(String county) {
		this.county = county;
	}

	public Date getReportTime() {
		return reportTime;
	}

	public void setReportTime(Date reportTime) {
		this.reportTime = reportTime;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}


}
