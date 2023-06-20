package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ActivityServer {
    // Create an arraylist to hold Activity names
    static ArrayList<String> activityNames = new ArrayList<>();

    public static void main(String[] args) throws IOException {

        //Read activityNames.txt file and add all activity names to activityNames arraylist
        // use a try-with-resources block to automatically close the BufferedReader
        try (BufferedReader reader = new BufferedReader(new FileReader("src/main/java/org/example/activityNames.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                activityNames.add(line);
            }
        }
        int portNumber = 8082;
        // Create a new ServerSocket to listen for incoming connections
        ServerSocket serverSocket = new ServerSocket(portNumber);
        System.out.println("Server listening on port " + portNumber);

        // Listen for incoming connections and handle them in a separate thread
        while (true) {
            // Accept an incoming connection
            Socket clientSocket = serverSocket.accept();

            // Create a new thread to handle the request
            Thread thread = new Thread(new ClientHandler2(clientSocket,activityNames));
            thread.start();
        }
    }
}
class ClientHandler2 implements Runnable {
    private Socket clientSocket;
    private ArrayList<String> activityNames;

    public ClientHandler2(Socket socket,ArrayList<String> activityNames) {
        this.clientSocket = socket;
        this.activityNames = activityNames;
    }
    @Override
    public void run() {
        try {
            //region Get the input and output streams for the socket
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            String request;
            // Read the incoming request
            request= in.readLine();

            //Print the request to the console
            System.out.println("Request is:  "+request);
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

            //TODO header is removed. Maybe problem? It is commented
            // Read and ignore the headers
            String headerLine;
//            while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
//            }
            //print the query string to the console
            System.out.println("query string is:  "+queryString);
            System.out.println("requestParts[1] is:  "+requestParts[1]);
            //endregion
            // Check if the request is a GET request
            if (requestParts[0].equals("GET") & !request.contains("favicon")) {
                System.out.println("GET request is received");
                // Extract the name parameter from the query string
                String name = "";
                if (queryString.contains("day") && queryString.contains("duration")) {

                }
                index = queryString.indexOf("name=");
                if (index != -1) {
                    name = queryString.substring(index + 5);
                }

                //region /add?name=activityname
                if (requestParts[1].contains("add")) {
                    //Check if the activity name is already in the list. Return 403 if it already exists, 200 if it doesn't
                    if (activityNames.contains(name)) {
                        out.println("HTTP/1.1 403 Forbidden");
                        out.println("Content-Type: text/html");
                        out.println("Content-Length: 0");
                        out.println("");
                        //Print to console that the activity name already exists
                        System.out.println("Activity name already exists");
                    } else {
                        activityNames.add(name);
                        out.println("HTTP/1.1 200 OK");
                        out.println("Content-Type: text/html");
                        out.println("Content-Length: 0");
                        out.println("");
                        //Print to output stream that the activity name was added using tags like <h1> and <p>
                        out.println("<h1>Activity name was added</h1>");
                        out.println("<p>Activity name: " + name + "</p>");
                        //flush the output stream
                        out.flush();
                        //Print that the name was added to the arraylist
                        System.out.println("Added " + name + " to the arraylist");
                    }
                }
                //endregion
                //region /remove?name=activityname
                else if(requestParts[1].contains("remove")){
                    //If the arraylist contains the name then remove it
                    if(activityNames.contains(name)){
                        activityNames.remove(name);
                        //print that the name was removed from the arraylist
                        System.out.println("Removed " + name + " from the arraylist");
                        //Print to the console all the names in the arraylist
                        for (String activityName : activityNames) {
                            System.out.println(activityName);
                        }
                        // Send the response
                        out.println("HTTP/1.1 200 OK");
                        out.println("Content-Type: text/html");
                        out.println("");
                        out.println("<html><head><title>Activity Server</title></head><body>");
                        out.println("<h1>Activity Server</h1>");
                        out.println("<p>Activity removed: " + name + "</p>");
                    }else {
                        //print to console that the name was not found in the arraylist
                        System.out.println("Activity not found. Could not remove " + name + " from the arraylist");
                        //print to console 403 error
                        System.out.println("403 error");
                        //Print HTTP 403 Forbidden
                        out.println("HTTP/1.1 403 Forbidden");
                        out.println("Content-Type: text/html");
                        out.println("");
                        out.println("<html><head><title>Activity Server</title></head><body>");
                        out.println("<h1>Activity Server</h1>");
                        out.println("<p>Activity not found: " + name + "</p>");

                    }
                    out.println("<p><a href=\"http://localhost:8082/\">Remove another activity</a></p>");
                    out.println("</body></html>");
                    out.flush();

                }
                //endregion
                //region /check?name=activityname
                else if(requestParts[1].contains("check")){
                    if(activityNames.contains(name)){
                        //Send the response
                        out.println("HTTP/1.1 200 OK");
                        out.println("Content-Type: text/html");
                        out.println("");
                        out.println("<html><head><title>Activity Server</title></head><body>");
                        out.println("<h1>Activity Server</h1>");
                        out.println("<p>Activity exists: " + name + "</p>");
                        out.println("<p><a href=\"http://localhost:8082/\">Check another activity</a></p>");
                        out.println("</body></html>");
                        out.flush();
                    }
                    else{
                        //If it does not exist
                        //Send the response as HTTP 404 not found  if it does not exist
                        out.println("HTTP/1.1 404 Not Found");
                        out.println("Content-Type: text/html");
                        out.println("");
                        out.println("<html><head><title>Activity Server</title></head><body>");
                        out.println("<h1>Activity Server</h1>");
                        out.println("<p>Activity does not exist: " + name + "</p>");
                        out.println("<p><a href=\"http://localhost:8082/\">Check another activity</a></p>");
                        out.println("</body></html>");
                        out.flush();
                        //Print this result to the console as well.
                        System.out.println("Activity does not exist: " + name);

                    }
                }
                //endregion
                //region /For handling GET request. This will send message indicating. This is for communication between client and server
                //To respond whether wanted Activity exist or not
                else if(requestParts[0].equals("GET") & !request.contains("favicon")){
                    // Extract the name parameter from the query string
                    String name2 = "";
                    if (queryString.contains("name=")) {
                        name2 = queryString.split("name=")[1];
                    }

                    if(activityNames.contains(name2)){
                        // Send the response back to the client
                        out.println("HTTP/1.1 200 OK");
                        out.println("Content-Type: text/plain");
                        out.println();
                        out.println("Activity "+name2+" exist");
                        out.flush();
                    }
                    else{
                        // Send the response back to the client
                        out.println("HTTP/1.1 400 Bad Request");
                        out.println("Content-Type: text/plain");
                        out.println();
                        out.println("Activity "+name2+" does not exist");
                        out.flush();
                    }
                }
                //endregion
                else {
                    // Send a "405 Method Not Allowed" response if the request is not a GET request
                    out.println("HTTP/1.1 405 Method Not Allowed");
                    out.println("Content-Type: text/plain");
                    out.println();
                    out.println("Only GET requests are allowed");
                }
            }
            else{
                System.out.println("Something wrong happened");
            }

            // Close the socket
            clientSocket.close();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("activityNames.txt"))) {
                for (String activityName : activityNames) {
                    // write the activity name to the file
                    writer.write(activityName + "\n");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

