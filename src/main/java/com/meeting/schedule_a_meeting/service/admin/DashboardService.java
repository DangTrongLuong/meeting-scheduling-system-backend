package com.meeting.schedule_a_meeting.service.admin;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.meeting.schedule_a_meeting.dto.response.admin.Dashboard.MeetingStatisticsDTO;
import com.meeting.schedule_a_meeting.entities.Meeting;
import com.meeting.schedule_a_meeting.enums.MeetingStatus;
import com.meeting.schedule_a_meeting.repositories.UserRepository;
import com.meeting.schedule_a_meeting.repositories.meeting.MeetingRepository;
import com.meeting.schedule_a_meeting.util.DashboardUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {
    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;

    /**
     * Get meeting statistics by period
     */
    public MeetingStatisticsDTO getMeetingStatistics(String period) {
        log.info("Getting meeting statistics for period: {}", period);

        LocalDateTime startDate = DashboardUtils.calculateStartDate(period);
        LocalDateTime endDate = LocalDateTime.now();

        List<Meeting> meetings = DashboardUtils.filterMeetingsByDateRange(
                meetingRepository.findAll(),
                startDate,
                endDate);

        long totalMeetings = meetings.size();
        long scheduledMeetings = meetings.stream()
                .filter(m -> m.getStatus() == MeetingStatus.SCHEDULED)
                .count();
        long cancelledMeetings = meetings.stream()
                .filter(m -> m.getStatus() == MeetingStatus.CANCELLED)
                .count();
        long pendingMeetings = meetings.stream()
                .filter(m -> m.getStatus() == MeetingStatus.PENDING_APPROVAL)
                .count();

        return MeetingStatisticsDTO.builder()
                .totalMeetings(totalMeetings)
                .scheduledMeetings(scheduledMeetings)
                .cancelledMeetings(cancelledMeetings)
                .pendingMeetings(pendingMeetings)
                .period(period)
                .build();
    }

    /**
     * Get meeting trend data
     */
    public Map<String, Object> getMeetingTrend(String period) {
        log.info("Getting meeting trend for period: {}", period);

        LocalDateTime startDate = DashboardUtils.calculateStartDate(period);
        LocalDateTime endDate = LocalDateTime.now();

        List<Meeting> meetings = DashboardUtils.filterMeetingsByDateRange(
                meetingRepository.findAll(),
                startDate,
                endDate);

        Map<String, Integer> trendData = new HashMap<>();

        if ("WEEK".equalsIgnoreCase(period)) {
            trendData = DashboardUtils.getWeeklyTrend(meetings);
        } else if ("MONTH".equalsIgnoreCase(period)) {
            trendData = DashboardUtils.getMonthlyTrend(meetings);
        } else {
            trendData = DashboardUtils.getYearlyTrend(meetings);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("data", trendData);
        response.put("period", period);

        return response;
    }

    /**
     * Get meeting status distribution
     */
    public Map<String, Object> getMeetingStatusDistribution(String period) {
        log.info("Getting meeting status distribution for period: {}", period);

        LocalDateTime startDate = DashboardUtils.calculateStartDate(period);
        LocalDateTime endDate = LocalDateTime.now();

        List<Meeting> meetings = DashboardUtils.filterMeetingsByDateRange(
                meetingRepository.findAll(),
                startDate,
                endDate);

        Map<String, Object> distribution = new HashMap<>();
        distribution.put("SCHEDULED",
                meetings.stream().filter(m -> m.getStatus() == MeetingStatus.SCHEDULED).count());
        distribution.put("CANCELLED",
                meetings.stream().filter(m -> m.getStatus() == MeetingStatus.CANCELLED).count());
        distribution.put("PENDING_APPROVAL",
                meetings.stream().filter(m -> m.getStatus() == MeetingStatus.PENDING_APPROVAL).count());
        distribution.put("period", period);

        return distribution;
    }

    /**
     * Get user statistics
     */
    public Map<String, Object> getUserStatistics() {
        log.info("Getting user statistics");

        long totalUsers = userRepository.count();
        long activeUsers = userRepository.findAll().stream()
                .filter(u -> u.isActive())
                .count();
        long inactiveUsers = totalUsers - activeUsers;

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalUsers", totalUsers);
        statistics.put("activeUsers", activeUsers);
        statistics.put("inactiveUsers", inactiveUsers);

        return statistics;
    }
}
