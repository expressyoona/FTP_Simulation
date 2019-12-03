/*
    Current: User connect directly
    Future: Server authorize user by something like SSH
 */


import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class FTPServer {

    private String rootDirectory;
    private String databaseUrl;

    public FTPServer() {
        try {
            System.out.println("Loading config...");


            FileReader fileReader = new FileReader(FTPConfiguration.CONFIGURATION_FILE);
            Properties prop = new Properties();
            prop.load(fileReader);
            rootDirectory = prop.getProperty("ROOT_DIR", FTPConfiguration.DEFAULT_ROOT_DIR);
            boolean writeMode = prop.getProperty("WRITE_MODE").equals("YES");
            databaseUrl = prop.getProperty("DATABASE_URL");

            System.out.println("Loading database...");
            // verifyUser("123", "123");
            File root = new File(rootDirectory);
            if (!root.exists()) {
                System.out.println("Root folder does not exist. Creating...");
                if (root.mkdir()) {
                    System.out.println("Created " + rootDirectory + " successfully!");
                } else {
                    System.out.println("Failed when create " + rootDirectory);
                }
            }
            ServerSocket serverSocket = new ServerSocket(FTPConfiguration.DEFAULT_PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                ServerService serverService = new ServerService(this, socket, rootDirectory, writeMode);
                serverService.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Connection getDatabaseConnection(String url) {
        try {
            return DriverManager.getConnection(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean verifyUser(String username, String password) {
        try {
            Connection con = getDatabaseConnection(databaseUrl);
            if (Objects.isNull(con)) {
                // System.out.println("Something went wrong");
                return false;
            }
            Statement statement = con.createStatement();
            String SQL = "SELECT password FROM Account WHERE username LIKE '" + username + "';";
            System.out.println(SQL);
            ResultSet resultSet = statement.executeQuery(SQL);
            while (resultSet.next()) {
                String data = resultSet.getString("password");
                if (data.equals(toMd5(password))) {
                    System.out.println("Correct");
                    return true;
                } else {
                    System.out.println("Wrong password");
                    return false;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Authentication error!");
        return false;
    }

    private String toMd5(String sourceText) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(sourceText.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            String hashtext = no.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        FTPServer ftpServer = new FTPServer();

    }

}

class ServerService extends Thread {

    private Socket socket;
    private OutputStream outputStream;
    private ObjectOutputStream objectOutputStream;
    private InputStream inputStream;
    private ObjectInputStream objectInputStream;

    private FTPServer server;
    private String rootPath;
    private String currentPath;
    private boolean writeMode;
    private boolean isVerified = false;

    private String clientUsername;
    private String clientPassword;

    public ServerService(FTPServer server, Socket socket, String rootPath, boolean writeMode) {
        try {
            this.server = server;
            this.socket = socket;
            this.rootPath = rootPath;
            this.writeMode = writeMode;
            this.currentPath = this.rootPath;

            this.clientPassword = new String();
            this.clientUsername = new String();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                inputStream = socket.getInputStream();
                objectInputStream = new ObjectInputStream(inputStream);
                String command = ((String) objectInputStream.readObject()).trim();

                outputStream = socket.getOutputStream();
                objectOutputStream = new ObjectOutputStream(this.outputStream);
                String response = handleCommand(command).toJson();
                objectOutputStream.writeObject(response);
                objectOutputStream.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.stop();
        }
    }

    public void stopService() {

    }

    public FTPResponse handleCommand(String s) throws IOException {
        FTPResponse response = new FTPResponse();
        s = s.toLowerCase();
        String[] params = s.split(" ");
        String commandName = params[0];
        if (!isVerified) {
            if (commandName.equals(FTPCommand.CONNECT)) {
                if (params.length == 1) {
                    response.setResponseCode(FTPResponseCode.PARAMETER_ERROR);
                    response.setMessage(FTPMessage.SPECIFY_USERNAME);
                } else {
                    clientUsername = params[1];
                    response.setResponseCode(FTPResponseCode.USERNAME_OK_NEED_PASSWORD);
                    response.setMessage(FTPMessage.SPECIFY_PASSWORD);
                }
            } else if (commandName.equals(FTPCommand.SPECIFY_PASSWORD)) {
                if (params.length > 1) {
                    clientPassword = params[1];
                }
                // System.out.println("Verifying " + clientUsername + "/" + clientPassword);
                isVerified = server.verifyUser(clientUsername, clientPassword);
                if (isVerified) {
                    response.setResponseCode(FTPResponseCode.USER_LOGGED_IN);
                    response.setMessage(FTPMessage.LOGIN_CORRECT);
                } else {
                    response.setResponseCode(FTPResponseCode.NOT_LOGGED_IN);
                    response.setMessage(FTPMessage.LOGIN_INCORRECT);
                }
            } else {
                response.setResponseCode(FTPResponseCode.NEED_LOG_IN);
                response.setMessage(FTPMessage.NEED_LOGIN);
            }
            return response;
        } else {
            // Verify == True
            switch (commandName) {
                case FTPCommand.CHANGE_DIRECTORY: {
                    if (params.length == 1) {
                        response.setResponseCode(FTPResponseCode.PARAMETER_ERROR);
                        response.setMessage(FTPMessage.NO_DIRECTORY_PROVIDED);
                    } else {
                        String nextFolder = params[1];
                        return changeDirectory(nextFolder);
                    }
                    break;
                }
                case FTPCommand.GO_UP_DIRECTORY: {
                    return changeDirectory("..");
                }
                case FTPCommand.MAKE_DIRECTORY: {
                    System.out.println("Create a directory");
                    break;
                }
                case FTPCommand.REMOVE_DIRECTORY: {
                    System.out.println("Remove a directory");
                    break;
                }
                case FTPCommand.REMOVE_FILE: {
                    System.out.println("Remove a file");
                    break;
                }
                case FTPCommand.RENAME_FILE: {
                    System.out.println("Rename a file");
                    break;
                }
                case FTPCommand.GET_WORKING_DIRECTORY: {
                    return getWorkingDirectory();
                }
                case FTPCommand.LIST_DIRECTORY: {
                    return listDirectory();
                }
                case FTPCommand.READ_ONE_FILE: {
                    if (params.length == 1) {
                        response.setResponseCode(FTPResponseCode.PARAMETER_ERROR);
                        response.setMessage(FTPMessage.NO_DIRECTORY_PROVIDED);
                    } else {
                        String file = params[1];
                        return readTextFile(file);
                    }
                    break;
                }
                default: {
                    response.setResponseCode(FTPResponseCode.COMMAND_NOT_IMPLEMENTED);
                    response.setMessage(FTPMessage.INVALID_COMMAND);
                }
            }
        }
        return response;
    }

    // OK
    private FTPResponse changeDirectory(String nextDirectory) {
        FTPResponse response = new FTPResponse();
        if ((nextDirectory.equals("..")) || (nextDirectory.equals("../"))) {
            if (currentPath.equals(rootPath)) {
                response.setResponseCode(FTPResponseCode.PERMISSION_DENIED);
                response.setMessage(FTPMessage.PERMISSION_DENIED);
            } else {
                File fi = new File(currentPath);
                response.setResponseCode(FTPResponseCode.CLOSING_DATA_CONNECTION_OR_SEND_DIR_OK);
                response.setMessage(FTPMessage.CHANGED_DIRECTORY);
                currentPath = fi.getParent();
            }
        } else {
            String nextPath = Paths.get(currentPath, nextDirectory).toString();
            File fi = new File(nextPath);
            if (!fi.exists()) {
                response.setResponseCode(FTPResponseCode.PERMISSION_DENIED);
                response.setMessage(FTPMessage.FAILED_TO_CHANGE_DIRECTORY);
            } else {
                currentPath = nextPath;
                response.setResponseCode(FTPResponseCode.CHANGED_DIRECTORY);
                response.setMessage(FTPMessage.CHANGED_DIRECTORY);
            }
        }

        return response;
    }

    // OK
    private FTPResponse getWorkingDirectory() {
        FTPResponse response = new FTPResponse();
        response.setResponseCode(FTPResponseCode.CREATED_DIRECTORY_OR_GET_WORKING_DIRECTORY);
        response.setMessage(currentPath + " is the current directory");
        return response;
    }

    // OK
    private FTPResponse listDirectory() {
        FTPResponse response = new FTPResponse();
        response.setResponseCode(FTPResponseCode.FILE_STATUS_OK);
        response.setMessage(FTPMessage.DIRECTORY_LISTING);

        List<String> listFileInString = new ArrayList<String>();
        File fi = new File(currentPath);
        for(File f : fi.listFiles()) {
            listFileInString.add(f.length() + "\t" + formatDate(f.lastModified()) + "\t" + f.getName());
        }
        response.setListFile(listFileInString);
        return  response;
    }

    private String formatDate(long ms) {
        Date d = new Date(ms);
        SimpleDateFormat format = new SimpleDateFormat(FTPConfiguration.DATE_PATTERN);
        return format.format(d);
    }

    private FTPResponse readTextFile(String fileName) throws IOException {
        FTPResponse response = new FTPResponse();
        String content = null;
        String path = Paths.get(currentPath, fileName).toString();
        File f = new File(path);
        if ((!f.exists()) || (f.isDirectory())) {
            response.setResponseCode(FTPResponseCode.PERMISSION_DENIED);
            response.setMessage(FTPMessage.NO_SUCH_FILE_OR_DIRECTORY);
        } else {
            content = new String(Files.readAllBytes(Paths.get(f.getPath())));
            response.setResponseCode(FTPResponseCode.FILE_STATUS_OK);
            response.setMessage(FTPMessage.FILE_CONTENT);
            List<String> result = new ArrayList<String>();
            result.add(content);
            response.setListFile(result);
        }
        return response;
    }
}
