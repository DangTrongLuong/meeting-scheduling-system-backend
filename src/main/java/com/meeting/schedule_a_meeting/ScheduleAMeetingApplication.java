package com.meeting.schedule_a_meeting;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication()
@EnableAsync
public class ScheduleAMeetingApplication {
	public static void main(String[] args) {
		SpringApplication.run(ScheduleAMeetingApplication.class, args);
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		String passwordAdmin = "Admin@123";
        String passwordSPAdmin = "SpAdmin@123";
		System.out.println(encoder.encode(passwordAdmin));
        System.out.println(encoder.encode(passwordSPAdmin));
	}

}
