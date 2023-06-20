package org.example;

import java.net.Socket;

public class ClientServer1 {
    public static void main(String[] args) {
        try{
            System.out.println("Client started");
            Socket socket = new Socket("localhost", 9876);

        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
