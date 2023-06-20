package org.example;

import java.io.*;
import java.util.HashMap;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.net.URLDecoder;

import static org.example.RoomServer.availability;

public class RoomServer {
    static ArrayList<Room> rooms = new ArrayList<>();
    public static HashMap<String, String> availability;

    public static void main(String[] args) throws IOException {
        // Set the port number
        int portNumber = 8081;
        availability = new HashMap<>();
        // Create a new ServerSocket to listen for incoming connections
        ServerSocket serverSocket = new ServerSocket(portNumber);

        System.out.println("Server listening on port " + portNumber);

        try (BufferedReader reader = new BufferedReader(new FileReader("src/main/java/org/example/rooms.txt"))) {
            String line;
            Room room = null;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                if (lineNumber % 8 == 1) {
                    // this is the first line of a new room, create the Room object using the values from this line
                    String[] values = line.split(",");
                    room = new Room(values[0], values[1], Integer.parseInt(values[2]), Integer.parseInt(values[3]), Integer.parseInt(values[4]));
                    rooms.add(room);
                } else {
                    // this is a day line, set the hours for the day
                    String[] values = line.split(",");
                    int day = Integer.parseInt(values[0]);
                    boolean[] hours = new boolean[values.length - 1];
                    for (int i = 1; i < values.length; i++) {
                        hours[i - 1] = Boolean.parseBoolean(values[i]);
                    }
                    room.getDay(day).setHours(hours);
                }
                lineNumber++;
            }
        }



        // Listen for incoming connections and handle them in a separate thread
        while (true) {
            // Accept an incoming connection
            Socket clientSocket = serverSocket.accept();

            // Create a new thread to handle the request
            Thread thread = new Thread(new ClientHandler(clientSocket,rooms));
            thread.start();
        }


    }
}

class ClientHandler implements Runnable {
    private Socket clientSocket;
    private ArrayList<Room> rooms;

    public ClientHandler(Socket socket,ArrayList<Room> rooms) {
        this.clientSocket = socket;
        this.rooms = rooms;
    }

    @Override
    public void run() {
        try {
            // Get the input and output streams for the socket
            //region set up input and output streams
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            String request;
            // Read the incoming request
            request= in.readLine();

            //Print the request to the console
            System.out.println("request is:  "+request);
            // Parse the request to extract the query string
            String[] requestParts = request.split(" ");
            String url = requestParts[1];
            //print the url to the console
            System.out.println("url is:  "+url);
            String queryString = "";
            int index = url.indexOf("?");
            if (index != -1) {
                queryString = url.substring(index + 1);
            }

            //TODO while loop commented. Later this may be needed
            // Read and ignore the headers
//            String headerLine;
//            while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
//            }
            //print the query string to the console
            System.out.println("query string is:  "+queryString);
            //endregion


            if (requestParts[0].equals("GET") & !request.contains("favicon")) {
                //region parse the query string
                System.out.println("GET request has been received. Room Server is processing the request...");
                //Print to console query string
                System.out.println("query string is:  "+queryString);
                // Extract the name parameter from the query string
                String name = "";
                // Create variables to store the extracted values
                int day = 0;
                int hour = 0;
                int duration = 0;
                //Getting name value from query string
                int index2 = queryString.indexOf("name=");
                if (index2 != -1) {
                    // "name=" was found in the input string
                    int endIndex = queryString.indexOf("&", index2);
                    if (endIndex == -1) {
                        // There is no "&" after "name="
                        endIndex = queryString.length();
                    }
                    String nameValue = queryString.substring(index2 + "name=".length(), endIndex);
                    name = nameValue;
                }
                //region reserve?room=room1&day=1&hour=1&duration=1
                //reserve?room=roomname&activity=activityname&day=x&hour=y&duration=z
                if (queryString.contains("day") && queryString.contains("duration")) {
                    // Split the query string into an array of key-value pairs
                    String[] pairs = queryString.split("&");

                    // Iterate through the array of pairs
                    for (String pair : pairs) {
                        // Split each pair into a key and a value
                        String[] parts = pair.split("=");
                        String key = parts[0];
                        String value = parts[1];

                        // Extract the values of the day, hour, and duration parameters
                        if (key.equals("day")) {
                            day = Integer.parseInt(value);
                        } else if (key.equals("hour")) {
                            hour = Integer.parseInt(value);
                        } else if (key.equals("duration")) {
                            duration = Integer.parseInt(value);
                        }
                    }

                    System.out.println("Day: " + day);
                    System.out.println("Hour: " + hour);
                    System.out.println("Duration: " + duration);
                }
                //endregion

                //region /add?name=roomname
                if (request.contains("add")){
                    //If the same name is already in the rooms arraylist, then don't add it
                    int flagForSameRoom=0;
                    for (Room room : rooms) {
                        if (room.getName().equals(name)) {
                            flagForSameRoom=1;
//                            System.out.println("Room with the same name already exists");
//                            //Bad request
//                            out.println("HTTP/1.1 400 Bad Request");
//                            out.println("Content-Type: text/html");
//                            out.println();
//                            out.println("Room with the same name already exists");
//                            out.println("");
//                            out.flush();
//                            return;//todo return or break?
                        }
                    }
                    //If the room is already exists
                    if(flagForSameRoom==1){
                        System.out.println("Room with the same name already exists");
                        //Bad request
                        out.println("HTTP/1.1 403 Forbidden Request");
                        out.println("Content-Type: text/html");
                        out.println();
                        out.println("<h1>403 Forbidden Request</h1>");
                        out.println("<p>Room with the same name already exists  </p>");
                        out.println("");
                        out.flush();
                    }
                    //Means the room is new so add it
                    else{
                        //If the name is not in the rooms arraylist, then add it
                        Room room = new Room(name, "available", 0,hour,hour+duration);
                        rooms.add(room);
                        // Send the response back to the client
                        out.println("HTTP/1.1 200 OK");
                        out.println("Content-Type: text/plain");
                        out.println();
                        out.println("<html> <head> <title>Room Added</title> </head> <body> <h1>Room Added " +room.getName() +"</h1> </body> </html>");
                        System.out.println(rooms);
                        //Create a room with name, availability, and reservationStartDate. name is the value of the name parameter
                    }
                }
                //endregion
                //region /remove?name=roomname
                else if (request.contains("remove")){

                    //If the name exists in the rooms arraylist
                    int flagForSameRoom=0;
                    for (Room room : rooms) {
                        if (room.getName().equals(name)) {
                            flagForSameRoom=1;
                            rooms.remove(room);
                            // Send the response back to the client
                            out.println("HTTP/1.1 200 OK");
                            out.println("Content-Type: text/plain");
                            out.println();
                            out.println("<html> <head> <title>Room Removed</title> </head> <body> <h1>Room Removed " +room.getName() +"</h1> </body> </html>");
                            out.flush();
                            System.out.println(rooms);
                            break;
                        }
                    }
                    //If the room is not exists
                    if(flagForSameRoom==0){
                        System.out.println("Room with the same name does not exist");
                        //Bad request
                        out.println("HTTP/1.1 403 Forbidden Request");
                        out.println("Content-Type: text/html");
                        out.println();
                        out.println("Room with the same name does not exist");
                        out.println("");
                        out.flush();
                    }
//                    rooms.remove(getRoomByName(name));
//                    // Send the response back to the client
//                    out.println("HTTP/1.1 200 OK");
//                    out.println("Content-Type: text/plain");
//                    out.println();
//                    out.println("Room removed successfully"+ "Current rooms are: "+rooms);
//                    out.println(rooms.toString());
//                    System.out.println(rooms.toString());
                }
                //endregion
                //region /reserve?name=roomname&day=x&hour=y&duration=z
                else if (request.contains("reserve")){
                    System.out.println("Room server has received a reservation request for room " + name + " on day " + day + " at " + hour + " for " + duration + " hours.");
                    System.out.println("Room server is reserving the room...");
                    // reserve the room using day,hour and duration
                    // Get the room by name
                    Room room = getRoomByName(name);
                    //Check if the room is available
                    if(room.isAvailable(day,hour,duration)){
                        //If the room is available
                        //TODO this part may be changed later
//                        room.setReservationDate(day);
//                        room.setReservationStartHour(hour);
//                        room.setReservationEndHour(hour+duration);

                        //Make the room's hours true between the hour-9 and hour+duration-9,indicating that
                        //the room is reserved for that hour
                        //Since the reservation starts at 9am, the index of the array is hour-9
                        for(int i = hour-9; i < hour+duration-9; i++){
                            room.getDay(day).getHours()[i] = true;
                        }
                        //print to the console the room that was reserved
                        System.out.println("Reserved room: " + room.getName()+"(Room Server)");
                        // Send the response back to the client
                        out.println("HTTP/1.1 200 OK");
                        out.println("Content-Type: text/plain");
                        out.println();
                        //Use html tag
                        out.println("<html> <head> <title>Room Reserved</title> </head> <body> <h1>Room Reserved " +room.getName() +"</h1> </body> </html>");
                        out.println("Room reserved successfully"+ "Current rooms are: "+rooms);
                        out.println(rooms.toString());
                        System.out.println(rooms.toString());

                        //flush
                    }
                    else{
                        //If the room is not available
                        System.out.println("Room is not available.(Room Server)");
                        // Send the response back to the client
                        //TODO check if this is the correct response
                        out.println("HTTP/1.1 403 Forbidden");
                        out.println("Content-Type: text/html");
                        out.println();
                        out.println("<html> <head> <title>Room Not Available</title> </head> <body> <h1>Room Not Available " +room.getName() +"</h1> </body> </html>");
                        //flush
                    }
                    out.println("Content-Type: text/plain");
                    out.println();
                    out.flush();
                }
                //endregion
                //region /checkavailability?day=x&hour=y&duration=z
                else if (request.contains("checkavailability") && request.contains("day") && request.contains("room")){
                    //Get day value from query string
                    int dayValue = 0;
                    int index3 = queryString.indexOf("day=");
                    if (index3 != -1) {
                        // "day=" was found in the input string
                        int endIndex = queryString.indexOf("&", index3);
                        if (endIndex == -1) {
                            // There is no "&" after "day="
                            endIndex = queryString.length();
                        }
                        String dayValueString = queryString.substring(index3 + "day=".length(), endIndex);
                        dayValue = Integer.parseInt(dayValueString);
                    }

                    //If the day value is not between 1 and 5, send a 400 Bad Request response
                    if(dayValue < 1 || dayValue > 5){
                        // Send the response back to the client
                        out.println("HTTP/1.1 400 Bad Request");
                        out.println("Content-Type: text/plain");
                        out.println();
                        //Use html tag
                        out.println("<html> <head> <title>Bad Request</title> </head> <body> <h1>Bad Request</h1> </body> </html>");
                        out.println("Day value is not between 1 and 5");
                        out.flush();
                    }
                    else{
                        //Check if there exists a room with the given name if so, print available hours to console and send response to client
                        if(rooms.contains(getRoomByName(name))){
                            //There exists a room with the given name
                            String availableHours = getRoomByName(name).getAvailableHours(dayValue);
                            System.out.println("Available hours for room "+name+" on day "+dayValue+" are: "+availableHours);
                            // Send the response back to the client
                            out.println("HTTP/1.1 200 OK");
                            out.println("Content-Type: text/plain");
                            out.println();
                            out.println(availableHours);
                            out.flush();

                        }

                        //If no such room exists it sends back an HTTP 404 Not Found message
                        if(!rooms.contains(getRoomByName(name))){
                            // Send the response back to the client HTTP 404 Not Found
                            out.println("HTTP/1.1 404 Not Found");
                            out.println("Content-Type: text/plain");
                            out.println();
                            out.println("Room not found");
                            out.flush();
                            System.out.println("Room not found");
                        }
                    }

                }
                //endregion
                //region /checkavailability?name=roomname&day=x
                //This endpoint is for ReservationServer
               else if(request.contains("listavailability") && request.contains("room") && request.contains("day")){

                   //region Get day value from query string and other settings
                    //Get day value from query string
                    int dayValue = 0;
                    int index3 = queryString.indexOf("day=");
                    if (index3 != -1) {
                        // "day=" was found in the input string
                        int endIndex = queryString.indexOf("&", index3);
                        if (endIndex == -1) {
                            // There is no "&" after "day="
                            endIndex = queryString.length();
                        }
                        String dayValueString = queryString.substring(index3 + "day=".length(), endIndex);
                        dayValue = Integer.parseInt(dayValueString);
                    }
                    //Get room value from query string
                    String roomValue = "";
                    int index4 = queryString.indexOf("room=");
                    if (index4 != -1) {
                        // "room=" was found in the input string
                        int endIndex = queryString.indexOf("&", index4);
                        if (endIndex == -1) {
                            // There is no "&" after "room="
                            endIndex = queryString.length();
                        }
                        roomValue = queryString.substring(index4 + "room=".length(), endIndex);
                    }

                    //endregion and
                    //If the day value is not between 1 and 7, send a 400 Bad Request response
                    if(dayValue < 1 || dayValue > 7){
                        // Send the response back to the client
                        out.println("HTTP/1.1 400 Bad Request");
                        out.println("Content-Type: text/plain");
                        out.println();
                        out.flush();
                    }
                    else{
                        //roomValue is the name of the room
                        //Check if there exists a room with the given name if so, print available hours to console and send response to client
                        if(rooms.contains(getRoomByName(roomValue))){
                            //There exists a room with the given name
                            String availableHours = getRoomByName(roomValue).getAvailableHours(dayValue);
                            System.out.println("Available hours for room "+roomValue+" on day "+dayValue+" are: "+availableHours);
                            // Send the response back to the client
                            out.println("HTTP/1.1 200 OK ");
                            out.println("Content-Type: text/plain ");
                            out.println("Content-Length: " + availableHours.length()+ " ");
                            out.println(availableHours);
                            out.println();
                            out.flush();

                        }

                        //If no such room exists it sends back an HTTP 404 Not Found message
                        else{
                            // Send the response back to the client HTTP 404 Not Found
                            out.println("HTTP/1.1 404 Not Found");
                            out.println("Content-Type: text/plain");
                            out.println();
                            out.println("Room not found");
                            out.flush();
                            System.out.println("Room not found");
                        }
                    }
                }

                //endregion

                //Lists all the available hours for 'ALL' days of the week
                else if(request.contains("listavailability") && request.contains("room")){
                    //Get room value from query string
                    String roomValue = "";
                    int index4 = queryString.indexOf("room=");
                    if (index4 != -1) {
                        // "room=" was found in the input string
                        int endIndex = queryString.indexOf("&", index4);
                        if (endIndex == -1) {
                            // There is no "&" after "room="
                            endIndex = queryString.length();
                        }
                        roomValue = queryString.substring(index4 + "room=".length(), endIndex);
                    }
                    //Check if there exists a room with the given name if so, print available hours to console and send response to client
                    if(rooms.contains(getRoomByName(roomValue))){
                        //There exists a room with the given name
                        String availableHours = getRoomByName(roomValue).getAvailableHoursForTheWeek();
                        System.out.println("Available hours for room "+roomValue+" are: "+availableHours);
                        // Send the response back to the client
                        out.println("HTTP/1.1 200 OK ");
                        out.println("Content-Type: text/plain ");
                        out.println("Content-Length: " + availableHours.length()+ " ");
                        out.println(availableHours);
                        out.println();
                        out.flush();

                    }

                    //If no such room exists it sends back an HTTP 404 Not Found message
                    else{
                        // Send the response back to the client HTTP 404 Not Found
                        out.println("HTTP/1.1 404 Not Found");
                        out.println("Content-Type: text/plain");
                        out.println();
                        out.println("Room not found");
                        out.flush();
                        System.out.println("Room not found");
                    }
                }

                else {
                    //todo check if this is the correct response
                    // Send the response back to the client
                    out.println("HTTP/1.1 200 OK");
                    out.println("Content-Type: text/plain");
                    out.println();
                    out.println("Hello, " + name);
                }
            }
            else {
                // Send a "405 Method Not Allowed" response if the request is not a GET request
//                out.println("HTTP/1.1 405 Method Not Allowed");
//                out.println("Content-Type: text/plain");
//                out.println();
//                out.println("Only GET requests are allowed");
            }
            // Close the socket

            clientSocket.close();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("src/main/java/org/example/rooms.txt"))) {
                for (Room room : rooms) {
                    // write the first line of the room block
                    writer.write(room.getName() + "," + room.getAvailability() + "," + room.getReservationDate() + "," + room.getReservationStartHour() + "," + room.getReservationEndHour() + "\n");
                    // write the day lines
                    for (int i = 1; i <= 7; i++) {
                        writer.write(i + ",");
                        boolean[] hours = room.getDay(i).getHours();
                        for (int j = 0; j < hours.length; j++) {
                            writer.write(hours[j] + ",");
                        }
                        writer.write("\n");
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // Get the room by name
    public Room getRoomByName(String name) {
        for (Room room : rooms) {
            if (room.getName().equals(name)) {
                return room;
            }
        }
        return null;
    }
}
