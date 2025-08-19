package com.example.TrainingProject2.config;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class TimeSlotGenerator {
    public static List<TimeSlot> generate() {
        List<TimeSlot> slots = new ArrayList<>();
        LocalTime start = LocalTime.of(8, 0);
        while (start.isBefore(LocalTime.of(20, 0))) {
            LocalTime end = start.plusMinutes(120);
            slots.add(new TimeSlot(start.toString(), end.toString(), start + " - " + end));
            start = end;
        }
        return slots;
    }

    public static class TimeSlot {
        public String startTime;
        public String endTime;
        public String label;

        public TimeSlot(String startTime, String endTime, String label) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.label = label;
        }

        public String getStartTime() {
            return startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public String getLabel() {
            return label;
        }
    }
}

