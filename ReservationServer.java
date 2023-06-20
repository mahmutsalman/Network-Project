package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReservationServer {
    static HashMap<Integer, ReservationInfo> reservationMap = new HashMap<>();

    public static void main(String[] args) throws IOException {

        // Set the port number
        int portNumber = 8080;
        // Create a new ServerSocket to listen for incoming connections
        ServerSocket serverSocket = new ServerSocket(portNumber);
        System.out.println("Server listening on port " + portNumber);

        // use a try-with-resources block to automatically close the BufferedReader
        try (BufferedReader reader = new BufferedReader(new FileReader("src/main/java/org/example/reservations.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // split the line into fields
                String[] fields = line.split(",");
                int id = Integer.parseInt(fields[0]);
                String roomName = fields[1];
                String activityName = fields[2];
                String time = fields[3];
                // create a ReservationInfo object for the reservation
                ReservationInfo reservationInfo = new ReservationInfo(id, roomName, activityName,time);

                // add the reservation to the reservationMap
                reservationMap.put(reservationInfo.hashCode(), reservationInfo);
            }
        }

        // Listen for incoming connections and handle them in a separate thread
        while (true) {
            // Accept an incoming connection
            Socket clientSocket = serverSocket.accept();
            // Create a new thread to handle the request
            Thread thread = new Thread(new ClientHandler3(clientSocket,reservationMap));
            thread.start();
        }
    }
}
class ClientHandler3 implements Runnable {
    private Socket clientSocket;
    private ArrayList<String> activityNames;
    static HashMap<Integer, ReservationInfo> reservationMap;

    int reservationId = 0;


    public ClientHandler3(Socket socket, HashMap<Integer, ReservationInfo> reservationMap) {
        this.clientSocket = socket;
        this.reservationMap = reservationMap;
    }

    @Override
    public void run() {
        try {
            //region /Get the input and output streams for the socket
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            String request = in.readLine();



            if(!request.contains("favicon")) {

                //region /reserve?room=roomname&activity=activityname&day=x&hour=y&duration=z
                if (request.contains("reserve") && request.contains("room") && request.contains("activity")) {
                    //region if request is "reserve"
                    //region /1- Contact the activity server to check if the activity exists
                    //request like this come: "reserve?room=roomname&activity=activityname&day=x&hour=y&duration=z"
                    // Extract the parameters from the request
                    Map<String, String> params = parseRequest(request);
                    String room = params.get("room");
                    String activity = params.get("activity");
                    int day = Integer.parseInt(params.get("day"));
                    int duration = Integer.parseInt(params.get("duration"));
                    int hour = Integer.parseInt(params.get("hour"));

                    //Connect to Activity Server and check if the activity exists
                    Socket activitySocket = new Socket("localhost", 8082);
                    BufferedReader activityIn = new BufferedReader(new InputStreamReader(activitySocket.getInputStream()));
                    PrintWriter activityOut = new PrintWriter(activitySocket.getOutputStream(), true);
                    activityOut.println("GET /activity?name=" + activity + " HTTP/1.1");
                    String activityResponse = activityIn.readLine();
                    System.out.println("Coming from Activity server response for GET /activity?name=\"" + activity + " \" HTTP/1.1 is : " + activityResponse);
                    //endregion
                    //region /2- Contact the room server to reserve the room
                    if (activityResponse.startsWith("HTTP/1.1 200 OK")) {
                        //print to console that the activity exists
                        System.out.println("Activity exists. Coming from Activity Server,response is: 200 OK");
                        // The activity exists, so now try to reserve the room
                        Socket activitySocketForRoom = new Socket("localhost", 8081);
                        BufferedReader activityInForRoom = new BufferedReader(new InputStreamReader(activitySocketForRoom.getInputStream()));
                        PrintWriter activityOutForRoom = new PrintWriter(activitySocketForRoom.getOutputStream(), true);

                        //reserve?name=roomname&day=x&hour=y&duration=z
                        // try reserving the room. Room server will first check if the room is available, if available it will reserve it
                        activityOutForRoom.println("GET /reserve?name=" + room + "&day=" + day + "&hour=" + hour + "&duration=" + duration);
                        String responseForRoom = activityInForRoom.readLine();
                        if (responseForRoom.startsWith("HTTP/1.1 200 OK")) {
                            System.out.println("Room is available.200 OK response from RoomServer");
                            // The room is available, so now try to reserve the room
                            //out print that room has been reserved
                            out.println("HTTP/1.1 200 OK");
                            out.println("Content-Type: text/plain");
                            out.println();
                            out.println("Room " + room + " reserved for " + activity + " on day " + day + " for " + duration + " hours");
                            //print to console that room is reserved
                            System.out.println("Room " + room + " reserved for " + activity + " on day " + day + " for " + duration + " hours");

                            try (BufferedReader reader = new BufferedReader(new FileReader("src/main/java/org/example/reservationId.txt"))) {
                                String line = reader.readLine();
                                reservationId = Integer.parseInt(line);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            // increment the reservationId
                            reservationId++;

                            //Add this reservation to the reservationMap. This will be used for /display?id=reservation_id request
                            ReservationInfo reservationInfo = new ReservationInfo(reservationId, room, activity, String.valueOf(day) + " " + String.valueOf(hour) + " " + duration);
                            // read the current reservationId from the file

                            // add the reservation to the reservationMap
                            reservationMap.put(reservationInfo.hashCode(), reservationInfo);

                            // use a try-with-resources block to automatically close the BufferedWriter
                            try (BufferedWriter writer = new BufferedWriter(new FileWriter("src/main/java/org/example/reservations.txt", true))) {
                                // write the reservation info to the file
                                writer.write(reservationInfo.getReservationId() + "," + reservationInfo.getRoomInfo() + "," + reservationInfo.getActivityName() + "," + reservationInfo.getTime() + "\n");
                            }

                            // write the updated reservationId to the file
                            try (BufferedWriter writer = new BufferedWriter(new FileWriter("src/main/java/org/example/reservationId.txt"))) {
                                writer.write(String.valueOf(reservationId));
                            }


                        } else if (responseForRoom.startsWith("HTTP/1.1 403 Forbidden")) {
                            // The room is not available, so return an error
                            //Print to console that room is not available
                            System.out.println("Room is not available. 403 Forbidden response from RoomServer");
                            out.println("HTTP/1.1 403 Forbidden");
                            out.println("Content-Type: text/plain");
                            out.println();
                            out.println("Room " + room + " is not available for " + activity + " on day " + day + " for " + duration + " hours");
                            //print to console that room is not available
                            System.out.println("");
                            System.out.println("Room " + room + " is not available for " + activity + " on day " + day + " for " + duration + " hours");
                        } else {
                            //Print to console that bad request happened
                            System.out.println("Bad request happened. 400 Bad Request response from RoomServer");
                            //Return bad request 400
                            out.println("HTTP/1.1 400 Bad Request");
                            out.println("Content-Type: text/plain");
                            out.println();

                        }

                    }
                    //endregion
                    //region /if the room is not available
                    else if (activityResponse.startsWith("HTTP/1.1 400")) {
                        //Print that not found  to the console
                        System.out.println("Activity not found, please try again.Response is:  " + activityResponse);
                        out.println("HTTP/1.1 400 Bad Request");
                        out.println("Content-Type: text/plain");
                        out.println();
                        out.println("Activity not found.please try again.Response is:  " + activityResponse);
                    }
                    //endregion
                    else {
                        // The activity does not exist, so we send back a 404 Not Found message
                        out.println("HTTP/1.1 404 Not Found");
                        out.println("Content-Type: text/plain");
                        out.println();
                        out.println("Activity not found.");
                    }
                    // Close the sockets
                    activitySocket.close();
                    clientSocket.close();
                    //endregion
                }
                //endregion
                //region /listavailability?room=roomname&day=x
                else if (request.contains("GET /listavailability") && request.contains("day") && request.contains("room")) {
                    //region if request is "listavailability"
                    // Extract the parameters from the request
                    Map<String, String> params = parseRequest(request);
                    //room parameter
                    String room = params.get("room");
                    int day = Integer.parseInt(params.get("day"));
                    // Contact the room server to get the list of available rooms
                    Socket roomSocket = new Socket("localhost", 8081);
                    BufferedReader roomIn = new BufferedReader(new InputStreamReader(roomSocket.getInputStream()));
                    PrintWriter roomOut = new PrintWriter(roomSocket.getOutputStream(), true);
                    roomOut.println("GET /listavailability?room=" + room + "&day=" + day);

                    String line = "";
                    String roomResponse = "";
                    while ((line = roomIn.readLine()) != null) {
                        roomResponse = roomResponse + line;
                    }

                    //region /if the room is available
                    if (roomResponse.startsWith("HTTP/1.1 200 OK")) {
                        String content = "";
                        //Extract the available hours from the response
                        int contentLengthIndex = roomResponse.indexOf("Content-Length:");

                        if (contentLengthIndex != -1) {
                            // Find the index of the first digit after "Content-Length:"
                            int startIndex = contentLengthIndex + "Content-Length:".length();
                            while (startIndex < roomResponse.length() && !Character.isDigit(roomResponse.charAt(startIndex))) {
                                startIndex++;
                            }
                            // Find the index of the next space character after the value of "Content-Length"
                            int endIndex = startIndex;
                            while (endIndex < roomResponse.length() && roomResponse.charAt(endIndex) != ' ') {
                                endIndex++;
                            }
                            // Extract the substring after the space character following the value of "Content-Length"
                            content = roomResponse.substring(endIndex);
                        }

                        System.out.println("Coming from Room server response for GET /listavailability?room=" + room + "&day=" + day + " is : " + content);
                        List<Integer> numbers = new ArrayList<Integer>();
                        int startIndex = roomResponse.indexOf("9");
                        if (startIndex >= 0) {
                            String body = roomResponse.substring(startIndex);
                            Pattern pattern = Pattern.compile("\\d+");
                            Matcher matcher = pattern.matcher(body);
                            while (matcher.find()) {
                                numbers.add(Integer.parseInt(matcher.group()));
                            }
                        }
                        // The room is available, so return the list of available rooms
                        out.println("HTTP/1.1 200 OK");
                        out.println("Content-Type: text/plain");
                        out.println();
                        out.println("Room " + room + " is available on day " + day + " at hours " + numbers);
                        //print to console that room is available
                    }
                    //endregion
                    //region /if the room is not available
                    else if (roomResponse.startsWith("HTTP/1.1 403 Forbidden")) {
                        //Print that not found  to the console
                        System.out.println("Room is not available. 403 Forbidden response from RoomServer");
                        out.println("HTTP/1.1 403 Forbidden");
                        out.println("Content-Type: text/plain");
                        out.println();
                        out.println(roomResponse);
                        //print to console that room is not available
                        System.out.println(roomResponse);
                    } else {
                        //Print to console that bad request happened
                        System.out.println("Bad request happened. 400 Bad Request response from RoomServer");
                        //Return bad request 400
                        out.println("HTTP/1.1 400 Bad Request");
                        out.println("Content-Type: text/plain");
                        out.println();
                    }
                    // Close the sockets
                    roomSocket.close();
                    clientSocket.close();
                    //endregion
                }
                //endregion
                //region /listavailability?room=roomname: Lists all the available hours for all days of the week
                else if (request.contains("GET /listavailability") && request.contains("room")) {

                    // Extract the parameters from the request
                    Map<String, String> params = parseRequest(request);
                    //room parameter
                    String room = params.get("room");
                    // Contact the room server to get the list of available rooms
                    Socket roomSocket = new Socket("localhost", 8081);
                    BufferedReader roomIn = new BufferedReader(new InputStreamReader(roomSocket.getInputStream()));
                    PrintWriter roomOut = new PrintWriter(roomSocket.getOutputStream(), true);
                    //Send request to room server to get the all the available hours for all days of the week
                    roomOut.println("GET /listavailability?room=" + room);
                    //Read the response from room server
                    String line = "";
                    String roomResponse = "";
                    while ((line = roomIn.readLine()) != null) {
                        roomResponse = roomResponse + line;
                    }
                    //region /if the room is available
                    if (roomResponse.startsWith("HTTP/1.1 200 OK")) {
                        //Lists all the available hours for all days of the week
                        System.out.println(roomResponse);
                        out.println("HTTP/1.1 200 OK");
                        out.println("Content-Type: text/plain");
                        out.println();
                        String body = "";
                        int startIndex = roomResponse.indexOf("Day");
                        if (startIndex >= 0) {
                            body = roomResponse.substring(startIndex);
                            System.out.println(body);
                        }
                        out.println("Listing all the available hours for all days of the week");
                        String formattedBody = body.replaceAll("(Day\\s\\d:)", "\n$1");
                        out.println(formattedBody);
                        out.flush();
                    }
                    //endregion
                    //region /if the room is not available
                    else if (roomResponse.startsWith("HTTP/1.1 403 Forbidden")) {
                        //Print that not found  to the console
                        System.out.println("Room is not available. 403 Forbidden response from RoomServer");
                        out.println("HTTP/1.1 403 Forbidden");
                        out.println("Content-Type: text/plain");
                        out.println();
                        out.println(roomResponse);
                        //print to console that room is not available
                        System.out.println(roomResponse);
                    } else {
                        //Print to console that bad request happened
                        System.out.println("Bad request happened. 400 Bad Request response from RoomServer");
                        //Return bad request 400
                        out.println("HTTP/1.1 400 Bad Request");
                        out.println("Content-Type: text/plain");
                        out.println();
                    }
                    // Close the sockets
                    roomSocket.close();
                    clientSocket.close();


                }
                //endregion
                else if (request.contains("/display?id")) {
                    // Extract the parameters from the request
                    Map<String, String> params = parseRequest(request);
                    //room parameter
                    String id = params.get("id");

                    //Find the reservation with id from reservationMap
                    for (ReservationInfo reservationInfo : reservationMap.values()) {
                        if (reservationInfo.getReservationId() == Integer.parseInt(id)) {
                            //Print to console that reservation is found
                            System.out.println("Reservation is found");
                            //Return the reservation
                            out.println("HTTP/1.1 200 OK");
                            out.println("Content-Type: text/plain");
                            out.println();
                            out.println(reservationInfo.roomInfo + " ");
                            out.flush();
                            // Close the sockets

                            //Print to console that reservation is found
                            out.println("<html><body><h1>Reservation is found</h1></body></html>");
                            //Print details of the reservation
                            out.println("<html><body><h1>Reservation details</h1></body></html>");
                            out.println(reservationInfo.getRoomInfo());
                            //add line break

                            out.println(reservationInfo.getActivityName());
                            
                            out.println(reservationInfo.getTime());

                            //Print to console reservationInfo
                            System.out.println("Reservation id is : " + reservationInfo.getReservationId());
                            clientSocket.close();

                            // reservationInfo is the reservation with the specified id
                            break;
                        }
                        else{
                            //Send 404 not found inside html tags
                            out.println("HTTP/1.1 404 Not Found");
                            out.println("Content-Type: text/html");
                            out.println();
                            out.println("<html><body><h1>404 Not Found</h1></body></html>");
                            out.flush();
                            // Close the sockets
                            clientSocket.close();

                        }
                    }

                    clientSocket.close();


                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // Parses the parameters from a GET request
    public static Map<String, String> parseRequest(String request) {
        Map<String, String> params = new HashMap<>();

        // Split the request into parts separated by '?'
        String[] parts = request.split("\\?");
        if (parts.length < 2) {
            // Return an empty map if there are no parameters
            return params;
        }
        // Split the parameters on '&'
        String[] paramParts = parts[1].split("&");
        for (String paramPart : paramParts) {
            // Split each part into a key-value pair separated by '='
            String[] pair = paramPart.split("=");
            String key = pair[0];
            String value = pair[1];
            // Split the value on ' ' and only keep the first part
            value = value.split(" ")[0];
            params.put(key, value);
        }

        return params;
    }


}

