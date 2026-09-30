package com.zjhl.project.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class DateLocalUtil {
	public static Date getCurrentYmdDate() {
        // 获取当前日期(年月日)
        LocalDate localDate = LocalDate.now();
        // LocalDate -> Date，时间部分置为 00:00:00
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
