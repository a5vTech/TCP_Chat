import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.Scanner;

/**
 * Created by Jesper Tang Petersen.
 */
public class TCPClient {
    static Socket socket;

    public static void main(String[] args) {
        System.out.println("=============CLIENT==============");

        Scanner sc = new Scanner(System.in);
        System.out.print("What is the IP for the server (type 0 for localhost): ");
        String ipToConnect = args.length >= 1 ? args[0] : sc.nextLine();

        System.out.print("What is the PORT for the server: ");
        int portToConnect = args.length >= 2 ? Integer.parseInt(args[1]) : sc.nextInt();
//        System.out.print("Please enter your desired username: ");
        String username = checkUsername();
//        String username = sc.next();
        final int PORT_SERVER = portToConnect;
        final String IP_SERVER_STR = ipToConnect.equals("0") ? "127.0.0.1" : ipToConnect;
        System.out.println("\nConnecting...");
        System.out.println("SERVER IP: " + IP_SERVER_STR);
        System.out.println("SERVER PORT: " + PORT_SERVER + "\n");
        InetAddress ip = null;
        try {
            ip = InetAddress.getByName(IP_SERVER_STR);
            //Create socket with servers ip and port
            socket = new Socket(ip, PORT_SERVER);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Thread read = new Thread(() -> {
            Boolean run = true;
            while (run) {
                try {
                    InputStream input = socket.getInputStream();
                    byte[] dataIn = new byte[1024];
                    input.read(dataIn);
                    String msgIn = new String(dataIn);

                    msgIn = msgIn.trim();
                    if (msgIn.equals("")) {
                        run = false;
                    }

                    if (msgIn.contains("LIST")) {
                        String[] activeUsers = msgIn.split(" ");
                        String currentlyConnectedUsers = "Currently connected users: [";
                        currentlyConnectedUsers += activeUsers[1];
                        for (int i = 2; i <= activeUsers.length - 1; i++) {
                            currentlyConnectedUsers += ", " + activeUsers[i];
                        }
                        System.out.println(currentlyConnectedUsers + "]");
                    } else if (msgIn.contains("DATA")) {
                        System.out.println(msgIn.substring(5));
                    } else {
                        System.out.print(msgIn + "\n");
                    }


                } catch (IOException e) {
//                e.printStackTrace();
                }


            }
        });
        Thread IMAV = new Thread(() -> {
            while (!socket.isClosed()) {
                try {
                    Thread.sleep(30000);
                    OutputStream outAlive = socket.getOutputStream();
                    String iAmAlive = "IMAV";
                    byte[] dataToSend = iAmAlive.getBytes();
                    //Send data to server

                    try {
                        outAlive.write(dataToSend);
                    } catch (SocketException ex) {
                        System.out.println("Connection lost to server");
                        System.out.println("Shutting down client");
                        System.exit(0);
                    }
                } catch (IOException e) {
                    System.out.println("CLIENT EXCEPTION ");
                    e.printStackTrace();

                } catch (InterruptedException e) {
//                    e.printStackTrace();
                }


            }
        });

        Boolean joinedChat = false;
        while (!socket.isClosed()) {
            try {


                //Create input and output readers
                InputStream input = socket.getInputStream();
                OutputStream output = socket.getOutputStream();
                while (!joinedChat) {
                    String joinMsg = "JOIN " + username + ", " + IP_SERVER_STR + ":" + PORT_SERVER;
                    //Convert message to bytes
                    byte[] dataToSend = joinMsg.getBytes();
                    //Send data to server
                    output.write(dataToSend);

                    //Prepare byte array to store incoming message
                    byte[] dataIn = new byte[1024];
                    //Read data from server into the byte array
                    input.read(dataIn);
                    //Convert data to msg
                    String msgIn = new String(dataIn);
                    //Remove trailing blank spaces
                    msgIn = msgIn.trim();

                    if (!msgIn.substring(0, 4).equalsIgnoreCase("J_OK")) {
                        System.out.println(msgIn.substring(5));
                        username = checkUsername();

                    } else {
                        System.out.println("You have joined the chat! ");
                        joinedChat = true;
                    }

                    //Start reading from server

                    if (joinedChat) {
                        read.start();
                        IMAV.start();
                    }
                }


                //Message to send
                sc = new Scanner(System.in);

                String msgToSend = sc.nextLine();
                if (!msgToSend.equals("QUIT") && !msgToSend.startsWith("IMAV")) {
                    msgToSend = "DATA " + username + ": " + msgToSend;

                    //Convert message to bytes
                    byte[] dataToSend = msgToSend.getBytes();
                    //Send data to server

                    output.write(dataToSend);
                } else if (msgToSend.equals("QUIT")) {

                    output.write(msgToSend.getBytes());
                    System.out.println("Shutting down chat");
                    System.exit(0);
                }

            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                try {
                    socket.close();
                    System.out.println("Connection closed");
                } catch (IOException e1) {
                    System.out.println("Connection failed to close");
                }
            }
        }
    }

    public static String checkUsername() {
        Scanner inputUsername = new Scanner(System.in);
        boolean verified = false;
        String username = "";
        while (!verified) {
            System.out.print("Please enter your desired username: ");
            username = inputUsername.next();
            if (username.length() > 12 || !username.matches("[A-Z-ÆØÅa-zæøå0-9_-]+")) {
                System.out.println("Username does not meet the requirements!");
                System.out.println("Max length is: 12\nCan contain letters, digits, '-' and '_'");
                verified = false;
            } else {

                verified = true;
            }
            verified = true;
        }

        return username;

    }
}