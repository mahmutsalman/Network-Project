package org.example;

public class Day {
    private int day;
    private boolean[] hours;
    //Add constructor
    public Day(int day) {
        this.day = day;
        this.hours = new boolean[9];
        //Assign all hours to false
        for (int i = 0; i < hours.length; i++) {
            hours[i] = false;
        }
    }
    //Add getters and setters
    public int getDay() {
        return day;
    }
    public void setDay(int day) {
        this.day = day;
    }
    public boolean[] getHours() {
        return hours;
    }
    public void setHours(boolean[] hours) {
        this.hours = hours;
    }


}