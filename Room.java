package org.example;

public class Room {
    // Add name,avaliability,reservationStartDate
    private String name;
    private String availability;
    private int reservationDate;

    //Add reservation start hour
    private int reservationStartHour;

    //Add reservation end hour
    private int reservationEndHour;
    //Day array
    private Day[] days;

    //Add constructor
    public Room(String name, String availability, int reservationDate, int reservationStartHour, int reservationEndHour) {
        this.name = name;
        this.availability = availability;
        this.reservationDate = reservationDate;
        this.reservationStartHour = reservationStartHour;
        this.reservationEndHour = reservationEndHour;
        this.days = new Day[7];
        //Assign 1 to 7 to each day's day variable
        for (int i = 0; i < days.length; i++) {
            days[i] = new Day(i + 1);
        }
        //Assign all hours to false
        for (int i = 0; i < days.length; i++) {
            for (int j = 0; j < days[i].getHours().length; j++) {
                days[i].getHours()[j] = false;
            }
        }

    }

    // Add all getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public int getReservationDate() {
        return reservationDate;
    }

    public void setReservationDate(int reservationDate) {
        this.reservationDate = reservationDate;
    }

    //Setter getter for reservation start hour
    public int getReservationStartHour() {
        return reservationStartHour;
    }

    //Setter for reservation start hour
    public void setReservationStartHour(int reservationStartHour) {
        this.reservationStartHour = reservationStartHour;
    }

    //Setter getter for reservation end hour
    public int getReservationEndHour() {
        return reservationEndHour;
    }

    //Setter for reservation end hour
    public void setReservationEndHour(int reservationEndHour) {
        this.reservationEndHour = reservationEndHour;
    }


    //isAvailable method
    public boolean isAvailable(int reservationDate, int reservationStartHour, int duration) {

        //reservationStartHour-9

        // for loop from reservationStartHour-9 to reservationStartHour-9+duration
        // if any of the hours is true return false
        // else return true
        for (int i = reservationStartHour - 9; i < reservationStartHour - 9 + duration; i++) {
            if (this.days[reservationDate - 1].getHours()[i] == true) {
                return false;
            }
        }
        return true;
    }

    // Get method for getting the day using the reservation date
    public Day getDay(int reservationDate) {
        return this.days[reservationDate - 1];
    }

    public String getAvailableHours(int reservationDate) {
        String availableHours = "";
        for (int i = 0; i < this.getDay(reservationDate).getHours().length; i++) {
            if (this.days[reservationDate - 1].getHours()[i] == false) {
                availableHours += (i + 9) + " ";
            }
        }
        return availableHours;
    }

    //Iterate through each day of each hour and pick the hours with value of false.
    public String getAvailableHoursForTheWeek() {
        String availableHours = "";
        for (int i = 0; i < this.days.length; i++) {
            availableHours += "Day " + (i + 1) + ": ";
            for (int j = 0; j < this.days[i].getHours().length; j++) {
                if (this.days[i].getHours()[j] == false) {
                    availableHours += "for hour: " + (j + 9) + "";
                }
            }
            //Add a new line
            availableHours += "";
        }
        return availableHours;


    }
    @Override
    public String toString() {
        return this.name+"" ;
    }
}
