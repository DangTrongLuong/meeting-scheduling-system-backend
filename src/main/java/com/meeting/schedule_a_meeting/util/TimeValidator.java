package com.meeting.schedule_a_meeting.util;

import java.time.LocalTime;

public class TimeValidator {

    private static final LocalTime MORNING_START = LocalTime.of(7, 0);
    private static final LocalTime MORNING_END = LocalTime.of(12, 0);
    private static final LocalTime AFTERNOON_START = LocalTime.of(13, 0);
    private static final LocalTime EVENING_END = LocalTime.of(23, 0);


    public static boolean isWithinWorkingHours(LocalTime time) {
        boolean isValidMorning = !time.isBefore(MORNING_START) && time.isBefore(MORNING_END);
        boolean isValidAfternoon = !time.isBefore(AFTERNOON_START) && !time.isAfter(EVENING_END);
        return isValidMorning || isValidAfternoon;
    }

    public static boolean isValidTimeRange(LocalTime startTime, LocalTime endTime) {
        if (!endTime.isAfter(startTime)) {
            return false;
        }

        boolean isValidMorning = !startTime.isBefore(MORNING_START) && !endTime.isAfter(MORNING_END);
        boolean isValidAfternoon = !startTime.isBefore(AFTERNOON_START) && !endTime.isAfter(EVENING_END);

        // Không được vượt qua giờ nghỉ trưa
        if (startTime.isBefore(MORNING_END) && endTime.isAfter(AFTERNOON_START)) {
            return false;
        }

        return isValidMorning || isValidAfternoon;
    }


    public static String getInvalidTimeRangeMessage(LocalTime startTime, LocalTime endTime) {
        if (!endTime.isAfter(startTime)) {
            return "End time must be after start time";
        }

        if (startTime.isBefore(MORNING_END) && endTime.isAfter(AFTERNOON_START)) {
            return "Meeting cannot span across lunch break (12:00-13:00)";
        }

        return "Meeting time must be within 7:00-12:00 or 13:00-23:00";
    }
}
