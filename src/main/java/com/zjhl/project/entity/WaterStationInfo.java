package com.zjhl.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("water_station_info")
public class WaterStationInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 站点名称 */
    private String stationName;

    /** 站点编号（唯一标识） */
    private String stationCode;

    /** 站点类型（RR=雨量站 ZZ=水位站 ZQ=流量站 TT=潮位站 DD=堰闸站） */
    private String stationType;

    /** 经度 */
    private String longitude;

    /** 纬度 */
    private String latitude;

    /** 地市 */
    private String city;

    /** 区县 */
    private String county;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }
    public String getStationCode() { return stationCode; }
    public void setStationCode(String stationCode) { this.stationCode = stationCode; }
    public String getStationType() { return stationType; }
    public void setStationType(String stationType) { this.stationType = stationType; }
    public String getLongitude() { return longitude; }
    public void setLongitude(String longitude) { this.longitude = longitude; }
    public String getLatitude() { return latitude; }
    public void setLatitude(String latitude) { this.latitude = latitude; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getCounty() { return county; }
    public void setCounty(String county) { this.county = county; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
