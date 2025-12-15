package com.meeting.schedule_a_meeting.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.meeting.schedule_a_meeting.entities.Meeting;

public class DashboardUtils {
    public static LocalDateTime calculateStartDate(String period) {
        LocalDate today = LocalDate.now();

        if ("WEEK".equalsIgnoreCase(period)) {
            return today.minusDays(7).atStartOfDay();
        } else if ("MONTH".equalsIgnoreCase(period)) {
            return today.minusDays(30).atStartOfDay();
        }

        return LocalDateTime.MIN;
    }

    /**
     * Get weekly trend data (Monday to Sunday)
     */
    public static Map<String, Integer> getWeeklyTrend(List<Meeting> meetings) {
        Map<String, Integer> trend = new LinkedHashMap<>();

        String[] days = { "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY" };
        for (String day : days) {
            trend.put(day, 0);
        }

        meetings.forEach(m -> {
            if (m.getCreatedAt() != null) {
                String dayName = m.getCreatedAt().getDayOfWeek().name();
                trend.put(dayName, trend.getOrDefault(dayName, 0) + 1);
            }
        });

        return trend;
    }

    /**
     * Get monthly trend data (last 30 days)
     */
    public static Map<String, Integer> getMonthlyTrend(List<Meeting> meetings) {
        Map<String, Integer> trend = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < 30; i++) {
            LocalDate date = today.minusDays(29 - i);
            trend.put(date.toString(), 0);
        }

        meetings.forEach(m -> {
            if (m.getCreatedAt() != null) {
                LocalDate date = m.getCreatedAt().toLocalDate();
                String dateStr = date.toString();
                trend.put(dateStr, trend.getOrDefault(dateStr, 0) + 1);
            }
        });

        // Sort by key
        Map<String, Integer> sortedTrend = new LinkedHashMap<>();
        trend.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEachOrdered(e -> sortedTrend.put(e.getKey(), e.getValue()));

        return sortedTrend;
    }

    /**
     * Get yearly trend data (last 12 months)
     */
    public static Map<String, Integer> getYearlyTrend(List<Meeting> meetings) {
        Map<String, Integer> trend = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < 12; i++) {
            YearMonth month = YearMonth.from(today.minusMonths(11 - i));
            trend.put(month.toString(), 0);
        }

        meetings.forEach(m -> {
            if (m.getCreatedAt() != null) {
                YearMonth month = YearMonth.from(m.getCreatedAt().toLocalDate());
                String monthStr = month.toString();
                trend.put(monthStr, trend.getOrDefault(monthStr, 0) + 1);
            }
        });

        return trend;
    }

    /**
     * Filter meetings by date range
     */
    public static List<Meeting> filterMeetingsByDateRange(
            List<Meeting> meetings,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        return meetings.stream()
                .filter(m -> m.getCreatedAt() != null &&
                        !m.getCreatedAt().isBefore(startDate) &&
                        !m.getCreatedAt().isAfter(endDate))
                .collect(Collectors.toList());
    }
}
