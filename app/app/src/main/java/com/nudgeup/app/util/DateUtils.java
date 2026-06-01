package com.nudgeup.app.util;

import java.util.Calendar;

public class DateUtils {

    private DateUtils() {}

    /**
     * "yyyy-MM-dd" 형식의 날짜 문자열을 받아 오늘 기준 D-day 값을 반환.
     * 양수: 남은 일수, 0: 오늘 마감, 음수: 초과 일수, 999: 날짜 없음/파싱 실패
     */
    public static long calcDday(String dueDateStr) {
        if (dueDateStr == null) return 999L;
        try {
            String[] parts = dueDateStr.split("-");
            if (parts.length != 3) return 999L;
            int year  = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]) - 1;
            int day   = Integer.parseInt(parts[2]);

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            Calendar due = Calendar.getInstance();
            due.set(year, month, day, 0, 0, 0);
            due.set(Calendar.MILLISECOND, 0);

            long diffMs = due.getTimeInMillis() - today.getTimeInMillis();
            return diffMs / (24L * 60 * 60 * 1000);
        } catch (Exception e) {
            return 999L;
        }
    }
}
