package com.example.ma2025.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DateUtils {

    public static String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_DISPLAY, Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static String formatDateTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_TIME, Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.TIME_FORMAT, Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static boolean isToday(long timestamp) {
        Calendar today = Calendar.getInstance();
        Calendar targetDate = Calendar.getInstance();
        targetDate.setTimeInMillis(timestamp);

        return today.get(Calendar.YEAR) == targetDate.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == targetDate.get(Calendar.DAY_OF_YEAR);
    }

    public static boolean isYesterday(long timestamp) {
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        Calendar targetDate = Calendar.getInstance();
        targetDate.setTimeInMillis(timestamp);

        return yesterday.get(Calendar.YEAR) == targetDate.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == targetDate.get(Calendar.DAY_OF_YEAR);
    }

    public static long getDaysDifference(long startTime, long endTime) {
        return TimeUnit.MILLISECONDS.toDays(endTime - startTime);
    }

    public static String getTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < TimeUnit.MINUTES.toMillis(1)) {
            return "Upravo sada";
        } else if (diff < TimeUnit.HOURS.toMillis(1)) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            return minutes + " min";
        } else if (diff < TimeUnit.DAYS.toMillis(1)) {
            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            return hours + " h";
        } else if (diff < TimeUnit.DAYS.toMillis(7)) {
            long days = TimeUnit.MILLISECONDS.toDays(diff);
            return days + " dan" + (days == 1 ? "" : "a");
        } else {
            return formatDate(timestamp);
        }
    }

    public static String getRemainingTime(long endTime) {
        long now = System.currentTimeMillis();
        long remaining = endTime - now;

        if (remaining <= 0) {
            return "Vreme je isteklo";
        }

        long days = TimeUnit.MILLISECONDS.toDays(remaining);
        long hours = TimeUnit.MILLISECONDS.toHours(remaining) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60;

        if (days > 0) {
            return days + " dan" + (days == 1 ? "" : "a") + " " + hours + "h";
        } else if (hours > 0) {
            return hours + "h " + minutes + "min";
        } else {
            return minutes + " min";
        }
    }

    public static long getStartOfDay(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public static long getEndOfDay(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }

    // ========== NEW METHODS FOR XP QUOTAS ==========

    /**
     * Returns the start of the current week (Monday 00:00:00)
     * Following ISO 8601 standard where Monday is the first day of the week
     */
    public static long getStartOfWeek(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);

        // Set to start of day
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // Get current day of week (Sunday = 1, Monday = 2, ..., Saturday = 7)
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

        // Calculate days to subtract to get to Monday
        int daysToSubtract;
        if (dayOfWeek == Calendar.SUNDAY) {
            // Sunday is 6 days after Monday
            daysToSubtract = 6;
        } else {
            // Monday = 2, so subtract (dayOfWeek - 2) to get to Monday
            daysToSubtract = dayOfWeek - Calendar.MONDAY;
        }

        cal.add(Calendar.DAY_OF_YEAR, -daysToSubtract);
        return cal.getTimeInMillis();
    }

    /**
     * Returns the start of the current month (1st day 00:00:00)
     */
    public static long getStartOfMonth(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);

        // Set to first day of month at 00:00:00
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTimeInMillis();
    }

    /**
     * Returns the end of the current month (last day 23:59:59.999)
     */
    public static long getEndOfMonth(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);

        // Set to last day of month at 23:59:59.999
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);

        return cal.getTimeInMillis();
    }

    // ========== EXISTING METHODS ==========

    public static boolean isWithinDays(long timestamp, int days) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        return diff <= TimeUnit.DAYS.toMillis(days);
    }

    public static long addDays(long timestamp, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.add(Calendar.DAY_OF_YEAR, days);
        return cal.getTimeInMillis();
    }

    public static long addHours(long timestamp, int hours) {
        return timestamp + TimeUnit.HOURS.toMillis(hours);
    }

    // ========== UTILITY METHODS FOR DEBUGGING ==========

    /**
     * Helper method for debugging - formats timestamp to readable string
     */
    public static String debugTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    /**
     * Helper method to check if two timestamps are in the same week
     */
    public static boolean isSameWeek(long timestamp1, long timestamp2) {
        long startOfWeek1 = getStartOfWeek(timestamp1);
        long startOfWeek2 = getStartOfWeek(timestamp2);
        return startOfWeek1 == startOfWeek2;
    }

    /**
     * Helper method to check if two timestamps are in the same month
     */
    public static boolean isSameMonth(long timestamp1, long timestamp2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTimeInMillis(timestamp1);
        cal2.setTimeInMillis(timestamp2);

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH);
    }
}