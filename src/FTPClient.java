import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class FTPClient {

    private Socket socket;
    private String host;
    private String username;
    private String password;
    private boolean loggedIn;
    private String currentServerPath;

    private Scanner scanner;
    private String command;

    public FTPClient() {
        try {
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

                if (command.split(" ")[0].equals(FTPCommand.GET_ONE_FILE)) {
                    String fileName = command.split(" ")[1];
                    byte[] byteArr = new byte[1024];
                    FileOutputStream fos = new FileOutputStream("client/" + fileName);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                    int bytesReadLength = inputStream.read(byteArr, 0, byteArr.length);
                    bos.write(byteArr, 0, bytesReadLength);
                    bos.close();
                }

                String responseStr = (String) objectInputStream.readObject();
                response = new FTPResponse(responseStr);
                System.out.println(response);
                if (response.getResponseCode() == FTPResponseCode.NOT_LOGGED_IN) {
                    username = "";
                    password = "";
                } else if (response.getResponseCode() == FTPResponseCode.USER_LOGGED_IN) {
                    loggedIn = true;
                } else if (response.getResponseCode() == FTPResponseCode.LOGGED_OUT) {
                    System.exit(0);
                }


                scanner = scanner.reset();
            }
        } catch (UnknownHostException hostException) {
            System.out.println("The host IP is unknown. Please try again!");
        } catch (IOException iOException) {
            System.out.println("Stream error!");
        } catch (ClassNotFoundException classNotFoundException) {
            System.out.println("You have forgot the Response Class. Please attach it to the project!");
        }
    }

    private void sendAuthenticateInformation() {
        if (username.isEmpty()) {
            System.out.print("ftp/" + currentServerPath + "> Enter username: ");
            username = scanner.nextLine();
            command += FTPCommand.USER + " " + username;
        } else if (password.isEmpty()) {
            System.out.print("ftp/" + currentServerPath + "> Enter password: ");
            password = scanner.nextLine();
            command += FTPCommand.PASS + " " + password;
        }
    }

    private void receiveFileFromServer() {

    }

    public static void main(String[] args) {
        FTPClient ftpClient = new FTPClient();
    }
}
