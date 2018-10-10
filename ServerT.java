import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalDate;
import java.util.Date;

public class ServerT implements Runnable {
    Socket socket;
    String clientIp;
    String username;
    long lastHeartbeat = 0;


    public ServerT(Socket socket, String ip) {
        this.socket = socket;
        this.clientIp = ip;
        lastHeartbeat = System.currentTimeMillis();
    }


    @Override
    public void run() {

        while (!socket.isClosed()) {
            try {
                //Create input and output streams
                InputStream input = socket.getInputStream();
                OutputStream output = socket.getOutputStream();

                //Receive msg from client
                byte[] dataIn = new byte[1024];
                input.read(dataIn);
                String msgIn = new String(dataIn);
                msgIn = msgIn.trim();


                //Prepare to send data
                String msgToSend = "";
                byte[] dataToSend;


                //Check if input is larger than 4 characters (Makes sure we are receiving a full command
                if (msgIn.length() >= 4) {
//                    System.out.println("["+clientIp+", "+username+"-> IN--->> " + msgIn);
                    //Switch based on command given (EX. JOIN, IMAV, ...)
//remove anything more than 250 characters
                    if (msgIn.length() > 250) {
                        msgIn = msgIn.substring(0, 251);
                    }


                    switch (msgIn.substring(0, 4)) {

                        //Join chat
                        case "JOIN":
                            int commaIndex = msgIn.indexOf(",");
                            int colonIndex = msgIn.indexOf(":");
                            username = msgIn.substring(5, commaIndex);
                            String ip = msgIn.substring(commaIndex + 2, colonIndex);
                            int port = Integer.parseInt(msgIn.substring(colonIndex + 1));

                            Boolean usernameAvailable = TCPServer.addUser(username);

                            if ((ip.equals(TCPServer.serverIp) || ip.equals("127.0.0.1")) && port == TCPServer.PORT_LISTEN && usernameAvailable) {
                                msgToSend = "J_OK";
                            } else {
                                msgToSend = "J_ER 5:A user with that name already exists or username is in the wrong format!";
                            }


                            //Send J_OK or J_ERR
                            dataToSend = msgToSend.getBytes();
                            output.write(dataToSend);

                            if (msgToSend.equals("J_OK")) {
                                String activeUsers = "LIST";

                                for (String username : TCPServer.usernames) {
                                    activeUsers += " " + username;
                                }


                                msgToSend = activeUsers;
                                TCPServer.broadcast(this, msgToSend);
                                dataToSend = msgToSend.getBytes();
                                output.write(dataToSend);
                            }

                            break;

                        //Message
                        case "DATA":
                            //Send this insted of null if you dont want the server to mirror the clients messages to himself
//                            TCPServer.broadcast(this, msgIn);
                            TCPServer.broadcast(null, msgIn);
                            break;
                        //I am alive
                        case "IMAV":
                            lastHeartbeat = System.currentTimeMillis();
                            break;

                        //Quit chat
                        case "QUIT":
                            quit();
                            break;

                        default:

                            break;

                    }
                    System.out.println("IN-->> " + msgIn + " [Sender: " + username + ", " + clientIp + "]");
                }

                //If socket is closed call quit
            } catch (IOException ex) {
                quit();
            }
        }

    }


    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public synchronized String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String receiveFromClient(InputStream input) {
        String msgIn = "";
        try {
            byte[] dataIn = new byte[1024];
            input.read(dataIn);
            msgIn = new String(dataIn);
            msgIn = msgIn.trim();
            return msgIn;
        } catch (IOException e) {
            e.printStackTrace();
            return msgIn;
        }

    }

    public void quit() {
        try {
            OutputStream output = socket.getOutputStream();
            TCPServer.removeUser(username);
            TCPServer.removeClientSocket(this);
            String activeUsers = "LIST";

            for (String username : TCPServer.usernames) {
                activeUsers += " " + username;
            }

            TCPServer.broadcast(this, activeUsers);
            output.write(activeUsers.getBytes());
            socket.close();
        } catch (IOException e) {
            System.out.println("Lost connection to client!" + "[" + clientIp + ", " + username + "]");
            try {
                socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
