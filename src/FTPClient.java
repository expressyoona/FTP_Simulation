import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.Scanner;

public class FTPClient {

    public static String CLIENT_STORAGE_FOLDER = "client";

    private Socket socket;
    private String host;
    private String username;
    private String password;
    private boolean loggedIn;
    private String currentServerPath;

    private Scanner scanner;
    private String command;
    private String receiveFile;

    public FTPClient() {
        try {
            File f = new File(CLIENT_STORAGE_FOLDER);
            if (!f.exists()) {
                f.mkdir();
            }

            loggedIn = false;
            username = new String();
            password = new String();
            currentServerPath = "";

            scanner = new Scanner(System.in);
            System.out.print("(To) ");
            host = scanner.nextLine();

            socket = new Socket(host, FTPConfiguration.DEFAULT_PORT);
            System.out.println("Connected to " + host);


            InputStream inputStream;
            ObjectInputStream objectInputStream;
            OutputStream outputStream;
            ObjectOutputStream objectOutputStream;

            FTPResponse response = null;

            while (true) {
                // Reset command every sent a request
                command = "";
                // Check if logged in or not
                if (!loggedIn) {
                    sendAuthenticateInformation();
                } else {
                    System.out.print("ftp/" + currentServerPath + "> ");
                    command = scanner.nextLine();
                }


                // Send to server
                outputStream = socket.getOutputStream();
                objectOutputStream = new ObjectOutputStream(outputStream);
                objectOutputStream.writeObject(command);
                objectOutputStream.flush();

                // Receive from server
                inputStream = socket.getInputStream();
                objectInputStream = new ObjectInputStream(inputStream);

                if (Utils.getCommand(command).equals(FTPCommand.SEND_ONE_FILE)) {
                    // Client send file. So data connection need run first
                    String fileName = Utils.getCommandParameter(command);

                    new FTPDataSender(FTPConfiguration.DATA_CONNECTION_PORT, fileName).start();
                } else if (Utils.getCommand(command).equals(FTPCommand.GET_ONE_FILE)) {
                    receiveFile = Utils.getCommandParameter(command);
                }

                String responseStr = (String) objectInputStream.readObject();
                response = new FTPResponse(responseStr);
                System.out.println(response);
                switch (response.getResponseCode()) {
                    case FTPResponseCode.NOT_LOGGED_IN: {
                        username = "";
                        password = "";
                        break;
                    }
                    case FTPResponseCode.USER_LOGGED_IN: {
                        loggedIn = true;
                        break;
                    }
                    case FTPResponseCode.LOGGED_OUT: {
                        System.exit(0);
                    }
                    case FTPResponseCode.READY_FOR_TRANSFER: {
                        // Ready for receive file
                        new FTPDataReceiver(host, FTPConfiguration.DATA_CONNECTION_PORT, "client/download/" + receiveFile).start();
                        break;
                    }
                }
                scanner = scanner.reset();
            }
        } catch (UnknownHostException hostException) {
            System.out.println("The host IP is unknown. Please try again!");
        } catch (IOException iOException) {
            System.out.println("Stream error!");
        } catch (ClassNotFoundException classNotFoundException) {
            System.out.println("You have forgot the Response Class. Please attach it to the project and run again!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendAuthenticateInformation() {
        if (username.isEmpty()) {
            System.out.print("ftp/" + currentServerPath + "> Enter username: ");
            username = scanner.nextLine();
            command += FTPCommand.USER + " " + username;
        } else if (password.isEmpty()) {
            System.out.print("ftp/" + currentServerPath + "> Enter password: ");
            // password = scanner.nextLine();
            password = Utils.inputPassword();
            command += FTPCommand.PASS + " " + password;
        }
    }

    public static void main(String[] args) {
        new FTPClient();
    }
}
