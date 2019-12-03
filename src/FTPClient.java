import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

public class FTPClient {
    public static void main(String[] args) {
        try {
            boolean loggedIn = false;
            String username = "", password = "";
            Socket socket = new Socket(FTPConfiguration.SERVER_HOST, FTPConfiguration.DEFAULT_PORT);
            System.out.println("Connected to " + FTPConfiguration.SERVER_HOST);
            String currentPath = "";
            InputStream inputStream;
            OutputStream outputStream;
            ObjectOutputStream objectOutputStream;
            ObjectInputStream objectInputStream;
            while (true) {
                Scanner scanner = new Scanner(System.in);
                String command = "";
                if (!loggedIn) {
                    if (username.isEmpty()) {
                        System.out.print("ftp/" + currentPath + "> Enter username: ");
                        username = scanner.nextLine();
                        command += FTPCommand.CONNECT + " " + username;
                    } else if (password.isEmpty()) {
                        System.out.print("ftp/" + currentPath + "> Enter password: ");
                        password = scanner.nextLine();
                        command += FTPCommand.SPECIFY_PASSWORD + " " + password;
                    }
                } else {
                    System.out.print("ftp/" + currentPath + "> ");
                    command = scanner.nextLine();
                }

                outputStream = socket.getOutputStream();
                objectOutputStream = new ObjectOutputStream(outputStream);
                objectOutputStream.writeObject(command);
                objectOutputStream.flush();

                inputStream = socket.getInputStream();
                objectInputStream = new ObjectInputStream(inputStream);
                String responseStr = (String) objectInputStream.readObject();
                FTPResponse response = new FTPResponse(responseStr);
                if (response.getResponseCode() == FTPResponseCode.NOT_LOGGED_IN) {
                    username = "";
                    password = "";
                } else if (response.getResponseCode() == FTPResponseCode.USER_LOGGED_IN) {
                    loggedIn = true;
                }
                System.out.println(response);
                scanner = scanner.reset();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
