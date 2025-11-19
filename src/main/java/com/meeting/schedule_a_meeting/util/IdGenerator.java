package com.meeting.schedule_a_meeting.util;

import java.util.Random;

public class IdGenerator {
    private static final Random random = new Random();

    public static String generate(String prefix) {
        int number = random.nextInt(1_000_000);
        return prefix + String.format("%06d", number);
    }
}
