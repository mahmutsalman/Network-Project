package org.example;

public class ReservationInfo {
    int reservationId;
    String roomInfo;
    String activityName;
    String time;

    //Add constructor
    public ReservationInfo(int reservationId, String roomInfo, String activityName, String time) {
        this.reservationId = reservationId;
        this.roomInfo = roomInfo;
        this.activityName = activityName;
        this.time = time;
    }
    //Add getters and setters
    public int getReservationId() {
        return reservationId;
    }
    public void setReservationId(int reservationId) {
        this.reservationId = reservationId;
    }
    public String getRoomInfo() {
        return roomInfo;
    }
    public void setRoomInfo(String roomInfo) {
        this.roomInfo = roomInfo;
    }
    public String getActivityName() {
        return activityName;
    }
    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }
    public String getTime() {
        return time;
    }
    public void setTime(String time) {
        this.time = time;
    }


}
