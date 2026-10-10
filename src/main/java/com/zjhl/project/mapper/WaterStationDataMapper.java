package com.zjhl.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zjhl.project.entity.WaterStationData;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WaterStationDataMapper extends BaseMapper<WaterStationData> {

    /** 查询日期范围内已有数据的日期（用于补录时跳过完整日期） */
    @Select("SELECT DISTINCT data_day FROM water_station_data WHERE data_day BETWEEN #{start} AND #{end}")
    List<String> selectDataDays(@Param("start") String start, @Param("end") String end);

    /**
     * 查询最新一天的全站记录（补录兜底用：当站点在实时接口中缺失时，
     * 从已入库数据中取站名/省市区/经纬度/汛限水位）
     */
    @Select("SELECT station_code, station_name, province, city, county, "
            + "longitude, latitude, water_limit_level "
            + "FROM water_station_data "
            + "WHERE data_day = (SELECT MAX(data_day) FROM water_station_data)")
    List<WaterStationData> selectLatestStationInfo();

    /**
     * 批量 UPSERT（幂等写入）。
     *
     * 依赖唯一键 uk_day_station(data_day, station_code)：已存在则更新，不存在则插入。
     * 一条多值 INSERT 完成整批写入，既省掉了"先删后插"（DELETE 会持有大范围行锁，
     * 与唯一索引的插入意向锁互相等待，是之前锁表的直接原因），也避免了重复运行报
     * DuplicateKeyException。
     *
     * 注意：ON DUPLICATE KEY UPDATE 中不要更新唯一键本身（data_day / station_code）。
     */
    @Insert("<script>"
            + "INSERT INTO water_station_data ("
            + "data_day, station_name, station_code, water_level, water_limit_level, is_out, "
            + "longitude, latitude, province, city, county, report_time) VALUES "
            + "<foreach collection='list' item='it' separator=','>"
            + "(#{it.dataDay}, #{it.stationName}, #{it.stationCode}, #{it.waterLevel}, "
            + "#{it.waterLimitLevel}, #{it.isOut}, #{it.longitude}, #{it.latitude}, "
            + "#{it.province}, #{it.city}, #{it.county}, #{it.reportTime})"
            + "</foreach> "
            + "ON DUPLICATE KEY UPDATE "
            + "station_name = VALUES(station_name), "
            + "water_level = VALUES(water_level), "
            + "water_limit_level = VALUES(water_limit_level), "
            + "is_out = VALUES(is_out), "
            + "longitude = VALUES(longitude), "
            + "latitude = VALUES(latitude), "
            + "province = VALUES(province), "
            + "city = VALUES(city), "
            + "county = VALUES(county), "
            + "report_time = VALUES(report_time)"
            + "</script>")
    int upsertBatch(@Param("list") List<WaterStationData> list);
}
