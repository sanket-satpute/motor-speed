package com.example.motorspeed;

import java.util.ArrayList;
import java.util.List;

public class MPojo {
    public int date;
    public int date_reading;
    public int month;
    public int year;
    public List<InnerPojo> hours;

    public MPojo(int date, int date_reading, int month, int year, List<InnerPojo> hours) {
        this.date = date;
        this.date_reading = date_reading;
        this.month = month;
        this.year = year;
        this.hours = hours;
    }

    public MPojo(int date, int date_reading, int month, int year, int hour) {
        this.date = date;
        this.date_reading = date_reading;
        this.month = month;
        this.year = year;
        this.hours = new ArrayList<>();
        this.hours.add(new InnerPojo(hour));
    }

    class InnerPojo {
        public int reading;

        public InnerPojo(int reading) {
            this.reading = reading;
        }
    }
}
