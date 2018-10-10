import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Jesper Tang Petersen
 */
public class TCPServer {
    static final int PORT_LISTEN = 5656;
    static Set<String> usernames = new HashSet<>();
    static ArrayList<ServerT> clients = new ArrayList<>();
    static String serverIp = "";

    public static void main(String[] args) {
        System.out.println("=============SERVER==============");


        Thread alive = new Thread(() -> {

            while (true) {
                ArrayList<Integer> clientsToRemove = new ArrayList<>();


                try {
                    int timeout = 60000; //Timeout = 1 min
                    Thread.sleep(10000);
                    if (clients.size() >= 1) {
                        for (int i = 0; i < clients.size(); i++) {
                            if (System.currentTimeMillis() - clients.get(i).lastHeartbeat > timeout) {

//                                System.out.println(clients);
                                clientsToRemove.add(i);
                            }


                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


                for (int i = 0; i < clientsToRemove.size(); i++) {
                    int temp = clientsToRemove.get(i);
                    try {
                        String inactiveMsg = "You have been disconnected due to inactivity or loss of connection";
                        byte[] dataToSend;
                        OutputStream output = null;
                        output = clients.get(i).socket.getOutputStream();
                        dataToSend = inactiveMsg.getBytes();
                        output.write(dataToSend);

                        usernames.remove(clients.get(temp).getUsername());
                        clients.get(i).socket.close();
                        clients.remove(temp);
                        TCPServer.broadcast(null, "Currently connected users: " + usernames.toString());
                    } catch (IOException e) {
//                        e.printStackTrace();
                    }

                }


            }
        });
        alive.start();


        try {
            serverIp = InetAddress.getLocalHost().getHostAddress();
            ServerSocket server = new ServerSocket(PORT_LISTEN);
            System.out.println("Server starting...\nIP: " + serverIp + "\nPORT: " + PORT_LISTEN + "\n");


            while (true) {
                Socket socket = server.accept();
                System.out.println("Client connected");
                String clientIp = socket.getInetAddress().getHostAddress();
                System.out.println("IP: " + clientIp);
                System.out.println("PORT: " + socket.getPort() + "\n");
                ServerT thread = new ServerT(socket, clientIp);
                Thread client = new Thread(thread);
                clients.add(thread);
                client.start();


            }
        } catch (IOException e) {
            System.out.println("SERVER IO EXCEPTION IN MAIN");
        }
    }


    public static Boolean addUser(String username) {
        Boolean validUsername = false;

        if (username.length() > 12 || !username.matches("[A-Z-ÆØÅa-zæøå0-9_-]+")) {
            validUsername = false;
        } else {
            validUsername = true;
        }

        if (validUsername && usernames.add(username)) {
            return true;
        } else {
            return false;
        }
    }

    public static Boolean removeUser(String username) {
        return usernames.remove(username);
    }

    public static Boolean removeClientSocket(ServerT client) {
        return clients.remove(client);
    }


    public static Set<String> getusers() {
        return usernames;
    }


    public static void broadcast(ServerT client, String msgToSend) {
        for (int i = 0; i < clients.size(); i++) {
            if (!clients.get(i).equals(client)) {
                try {
                    if (!clients.get(i).socket.isClosed()) {
                        byte[] dataToSend;
                        Socket socket = clients.get(i).socket;
                        OutputStream output = socket.getOutputStream();
                        dataToSend = msgToSend.getBytes();
                        output.write(dataToSend);

                    }

                } catch (IOException e) {
//                    e.printStackTrace();
                }

            }
        }
    }
}