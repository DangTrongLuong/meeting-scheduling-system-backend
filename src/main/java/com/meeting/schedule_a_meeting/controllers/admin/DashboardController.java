package com.meeting.schedule_a_meeting.controllers.admin;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.meeting.schedule_a_meeting.dto.response.admin.Dashboard.MeetingStatisticsDTO;
import com.meeting.schedule_a_meeting.dto.response.users.meeting.ApiResponse;
import com.meeting.schedule_a_meeting.service.admin.DashboardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {
    private final DashboardService dashboardService;

    /**
     * Get meeting statistics
     * 
     * @param period WEEK, MONTH, ALL
     */
    @GetMapping("/meeting-statistics")
    public ResponseEntity<ApiResponse<MeetingStatisticsDTO>> getMeetingStatistics(
            @RequestParam(defaultValue = "MONTH") String period) {

        MeetingStatisticsDTO statistics = dashboardService.getMeetingStatistics(period);
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    /**
     * Get meeting trend data
     * 
     * @param period WEEK, MONTH, ALL
     */
    @GetMapping("/meeting-trend")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMeetingTrend(
            @RequestParam(defaultValue = "MONTH") String period) {

        Map<String, Object> trendData = dashboardService.getMeetingTrend(period);
        return ResponseEntity.ok(ApiResponse.success(trendData));
    }

    /**
     * Get meeting status distribution
     * 
     * @param period WEEK, MONTH, ALL
     */
    @GetMapping("/meeting-status-distribution")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMeetingStatusDistribution(
            @RequestParam(defaultValue = "MONTH") String period) {

        Map<String, Object> distribution = dashboardService.getMeetingStatusDistribution(period);
        return ResponseEntity.ok(ApiResponse.success(distribution));
    }

    /**
     * Get user statistics
     */
    @GetMapping("/user-statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserStatistics() {

        Map<String, Object> statistics = dashboardService.getUserStatistics();
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }
}
