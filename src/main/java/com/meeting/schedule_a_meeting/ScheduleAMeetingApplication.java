package com.meeting.schedule_a_meeting;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


@SpringBootApplication()
public class ScheduleAMeetingApplication {
	public static void main(String[] args) {
		SpringApplication.run(ScheduleAMeetingApplication.class, args);
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		String password = "Admin@123";
		System.out.println(encoder.encode(password));
	}

}
