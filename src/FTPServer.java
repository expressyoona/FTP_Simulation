/*
    Current: User connect directly
    Future: Server authorize user by something like SSH
 */


import com.sun.istack.internal.NotNull;

import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.List;

public class FTPServer {

    private String rootDirectory;
    private String databaseUrl;
    private long maxFileSize; // 5MB = 5 * 1024 bytes
    private boolean writeMode;
    private int maximumAuthenticationTime;
    private String datePattern;

    public FTPServer() {
        try {
            FileReader fileReader = new FileReader(FTPConfiguration.CONFIGURATION_FILE);
            Properties prop = new Properties();
            prop.load(fileReader);
            rootDirectory = prop.getProperty("ROOT_DIR");
            writeMode = prop.getProperty("WRITE_MODE").equals("YES");
            databaseUrl = prop.getProperty("DATABASE_URL");
            maxFileSize = Long.valueOf(prop.getProperty("MAX_FILE_SIZE"));
            maximumAuthenticationTime = Integer.valueOf(prop.getProperty("MAX_RETRY_LOGIN"));
            datePattern = prop.getProperty("DATE_PATTERN");
            fileReader.close();

            File root = new File(rootDirectory);
            if (!root.exists()) {
                System.out.println("Root folder does not exist. Creating...");
                if (!root.mkdir()) {
                    System.out.println("Failed when create " + rootDirectory);
                }
            }
            ServerSocket serverSocket = new ServerSocket(FTPConfiguration.DEFAULT_PORT);
            System.out.println("FTP server ready for running...");
            while (true) {
                Socket socket = serverSocket.accept();
                ServerService serverService = new ServerService(this, socket, rootDirectory);
                serverService.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public boolean isWriteMode() {
        return writeMode;
    }

    public int getMaximumAuthenticationTime() {
        return maximumAuthenticationTime;
    }

    public String getDatePattern() {
        return datePattern;
    }

    private Connection getDatabaseConnection() {
        try {
            return DriverManager.getConnection(databaseUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getRootDirectory() {
        return rootDirectory;
    }

    public UserEntity verifyUser(UserEntity user) throws SQLException {
        Connection con = null;
        try {
            con = getDatabaseConnection();
            if (Objects.isNull(con)) {
                System.out.println("Can't establish connection to database!");
            }
            Statement statement = con.createStatement();
            String SQL = String.format("SELECT password, id FROM tbluser WHERE username LIKE '%s'", user.getUsername());
            ResultSet resultSet = statement.executeQuery(SQL);
            if (resultSet.next()) {
                String password = resultSet.getString("password");
                if (password.equals(Utils.toMd5(user.getPassword()))) {
                    user.setId(resultSet.getInt("id"));
                }
            }
        } catch (Exception e) {
            System.out.println("Error");
            e.printStackTrace();
        } finally {
            if (Objects.nonNull(con)) {
                con.close();
            }
            return user;
        }
    }

    public void updateActivity(int userId, String content) {
        try {
            Connection conn = getDatabaseConnection();
            String sql = "INSERT INTO tblactivity(user_id, content) VALUES(?, ?)";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setInt(1, userId);
            statement.setString(2, content);
            statement.executeUpdate();
            statement.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        FTPServer ftpServer = new FTPServer();
    }

}

class ServerService extends Thread {

    private Socket socket;
    private OutputStream outputStream;

    private FTPServer server;
    private String rootPath;
    private String currentPath;
    private boolean isVerified = false;
    private boolean isDisconnected = false;
    private int count;
    private UserEntity client;

    public ServerService(FTPServer server, Socket socket, String rootPath) {
        try {
            this.count = 0;
            this.server = server;
            this.socket = socket;
            this.rootPath = rootPath;
            this.currentPath = this.rootPath;
            this.client = new UserEntity();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                String command;
                if (!isDisconnected) {
                    InputStream inputStream = socket.getInputStream();
                    ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                    command = ((String) objectInputStream.readObject()).trim();

                    outputStream = socket.getOutputStream();
                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(this.outputStream);
                    String response = handleCommand(command).toJson();
                    objectOutputStream.writeObject(response);
                    objectOutputStream.flush();
                } else {
                    socket.close();
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public FTPResponse handleCommand(String s) throws IOException, SQLException {
        FTPResponse response = new FTPResponse();
        s = s.toLowerCase();
        String[] params = s.split(" ");
        String command = params[0];
        if (!isVerified) {
            if (command.equals(FTPCommand.USER)) {
                if (params.length == 1) {
                    response.setResponseCode(FTPResponseCode.PARAMETER_ERROR);
                    response.setMessage(FTPMessage.SPECIFY_USERNAME);
                } else {
                    client.setUsername(params[1]);
                    response.setResponseCode(FTPResponseCode.USERNAME_OK_NEED_PASSWORD);
                    response.setMessage(FTPMessage.SPECIFY_PASSWORD);
                }
            } else if (command.equals(FTPCommand.PASS)) {
                if (params.length > 1) {
                    client.setPassword(params[1]);
                    client = server.verifyUser(client);
                    // System.out.println("Client ID = " + client.getId());
                    if (client.getId() != -1) {
                        response.setResponseCode(FTPResponseCode.USER_LOGGED_IN);
                        response.setMessage(FTPMessage.LOGIN_CORRECT);
                        isVerified = true;
                    } else {
                        count++;
                        if (count < server.getMaximumAuthenticationTime()) {
                            response.setResponseCode(FTPResponseCode.NOT_LOGGED_IN);
                            response.setMessage(FTPMessage.LOGIN_INCORRECT);
                        } else {
                            response.setResponseCode(FTPResponseCode.LOGGED_OUT);
                            response.setMessage(FTPMessage.GOODBYE);
                            isDisconnected = true;
                        }
                    }
                } else {
                    response.setResponseCode(FTPResponseCode.PARAMETER_ERROR);
                    response.setMessage(FTPMessage.SPECIFY_USERNAME);
                }
            } else {
                response.setResponseCode(FTPResponseCode.NEED_LOG_IN);
                response.setMessage(FTPMessage.NEED_LOGIN);
            }
            return response;
        } else {
            // Verify == True
            switch (command) {
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
                    return goToParentDirectory();
                }
                case FTPCommand.MAKE_DIRECTORY: {
                    if (server.isWriteMode()) {
                        if (params.length == 1) {
                            response.setResponseCode(FTPResponseCode.PARAMETER_ERROR);
                            response.setMessage(FTPMessage.NO_DIRECTORY_PROVIDED);
                        } else {
                            String newFolder = params[1];
                            return createFolder(newFolder);
                        }
                    } else {
                        return FTPResponse.permissionDenied();
                    }
                    break;
                }
                case FTPCommand.REMOVE_DIRECTORY: {
                    if (server.isWriteMode()) {
                        if (params.length == 1) {
                            response.setResponseCode(FTPResponseCode.PARAMETER_ERROR);
                            response.setMessage(FTPMessage.NO_DIRECTORY_PROVIDED);
                        } else {
                            String folder = params[1];
                            return deleteFolder(folder);
                        }
                    } else {
                        return FTPResponse.permissionDenied();
                    }
                    break;
                }
                case FTPCommand.REMOVE_FILE: {
                    if (server.isWriteMode()) {
                        if (params.length == 1) {
                            response.setResponseCode(FTPResponseCode.PARAMETER_ERROR);
                            response.setMessage(FTPMessage.NO_DIRECTORY_PROVIDED);
                        } else {
                            String file = params[1];
                            return removeFile(file);
                        }
                    } else {
                        return FTPResponse.permissionDenied();
                    }
                    break;
                }
                case FTPCommand.RENAME_FILE: {
                    if (server.isWriteMode()) {
                        if (params.length < 3) {
                            response.setResponseCode(FTPResponseCode.PARAMETER_ERROR);
                            response.setMessage(FTPMessage.NO_DIRECTORY_PROVIDED);
                        } else {
                            String currentFilename = params[1];
                            String newFilename = params[2];
                            return renameFile(currentFilename, newFilename);
                        }
                    } else {
                        return FTPResponse.permissionDenied();
                    }
                    break;
                }
                case FTPCommand.GET_WORKING_DIRECTORY: {
                    return getWorkingDirectory();
                }
                case FTPCommand.LIST_DIRECTORY: {
                    if (params.length > 1) {
                        return listSpecificDirectory(params[1]);
                    }
                    return listCurrentDirectory();
                }
                case FTPCommand.READ_ONE_FILE: {
                    if (params.length == 1) {
                        response.setResponseCode(FTPResponseCode.PARAMETER_ERROR);
                        response.setMessage(FTPMessage.FILE_NOT_FOUND);
                    } else {
                        String file = params[1];
                        return readTextFile(file);
                    }
                    break;
                }
                case FTPCommand.GET_ONE_FILE: {
                    if (params.length == 1) {
                        response.setResponseCode(FTPResponseCode.PARAMETER_ERROR);
                        response.setMessage(FTPMessage.NO_DIRECTORY_PROVIDED);
                    } else {
                        String file = Paths.get(currentPath, params[1]).toString();
                        response.setResponseCode(FTPResponseCode.READY_FOR_TRANSFER);
                        new FTPDataSender(FTPConfiguration.DATA_CONNECTION_PORT, file).start();
                        response.setMessage(FTPMessage.TRANSFER_COMPLETE);
                    }
                    break;
                }
                // Client send one file to server
                case FTPCommand.SEND_ONE_FILE: {
                    if (params.length == 1) {
                        response.setResponseCode(FTPResponseCode.PARAMETER_ERROR);
                        response.setMessage(FTPMessage.NO_DIRECTORY_PROVIDED);
                    } else {
                        String userStorageFolder = Paths.get(server.getDatePattern(), "client_storage", String.valueOf(client.getId())).toString();
                        File f = new File(userStorageFolder);
                        if (!f.exists()) {
                            f.mkdirs();
                        }
                        String p[] = params[1].split("/");
                        String fileName = p[p.length - 1];
                        String file = Paths.get(f.toString(), fileName).toString();
                        // System.out.println("File = " + file);
                        new FTPDataReceiver(socket.getInetAddress().toString().replace("/", ""), FTPConfiguration.DATA_CONNECTION_PORT, file).start();
                        response.setResponseCode(FTPResponseCode.CLOSING_DATA_CONNECTION_OR_SEND_FILE_FOLDER_OK);
                        response.setMessage(FTPMessage.TRANSFER_COMPLETE);
                    }
                    break;
                }
                case FTPCommand.EXIT:
                case FTPCommand.QUIT:
                case FTPCommand.BYE: {
                    this.isDisconnected = true;
                    response.setResponseCode(FTPResponseCode.LOGGED_OUT);
                    response.setMessage(FTPMessage.LOGGED_OUT);
                    Thread.currentThread().interrupt();
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

    // Rename a file or folder
    private FTPResponse renameFile(String currentFilename, String newFilename) {
        FTPResponse response = new FTPResponse();
        File sourceFile = new File(Paths.get(currentPath, currentFilename).toString());
        File destinationFile = new File(Paths.get(currentPath, newFilename).toString());
        if (sourceFile.exists()) {
            if (destinationFile.exists()) {
                response.setResponseCode(FTPResponseCode.PERMISSION_DENIED);
                response.setMessage(FTPMessage.DUPLICATED_FILE_FOLDER_NAME_WHEN_CREATE);
            } else {
                sourceFile.renameTo(destinationFile);
                response.setResponseCode(FTPResponseCode.COMMAND_SUCCESSFULLY);
                response.setMessage(FTPMessage.CHANGED_FILENAME);
                server.updateActivity(client.getId(), String.format("Renamed %s to %s", newFilename));
            }
        } else {
            response.setResponseCode(FTPResponseCode.PERMISSION_DENIED);
            response.setMessage(FTPMessage.NO_SUCH_FILE_OR_DIRECTORY);
        }
        return response;
    }

    // Remove a file
    private FTPResponse removeFile(String file) {
        FTPResponse response = new FTPResponse();
        File fi = new File(Paths.get(currentPath, file).toString());
        if (!fi.exists()) {
            response.setResponseCode(FTPResponseCode.PERMISSION_DENIED);
            response.setMessage(FTPMessage.NO_SUCH_FILE_OR_DIRECTORY);
        } else {
            if (fi.delete()) {
                response.setResponseCode(FTPResponseCode.COMMAND_SUCCESSFULLY);
                response.setMessage(String.format(FTPMessage.DELETED_FOLDER_OR_FILE, file));
                server.updateActivity(client.getId(), String.format("Deleted %s", file));
            }
        }
        return response;
    }

    // Create a new folder
    private FTPResponse createFolder(String newFolder) {
        FTPResponse response = new FTPResponse();
        File fi = new File(Paths.get(currentPath, newFolder).toString());
        if (fi.exists()) {
            response.setResponseCode(FTPResponseCode.PERMISSION_DENIED);
            response.setMessage(FTPMessage.DUPLICATED_FILE_FOLDER_NAME_WHEN_CREATE);
        } else {
            if (fi.mkdir()) {
                response.setResponseCode(FTPResponseCode.CREATED_DIRECTORY_OR_GET_WORKING_DIRECTORY);
                response.setMessage(String.format(FTPMessage.CREATED_FOLDER, newFolder));
                server.updateActivity(client.getId(), String.format("Created %s", newFolder));
            }
        }
        return response;
    }

    // Change to another directory
    private FTPResponse changeDirectory(String nextDirectory) {
        FTPResponse response = new FTPResponse();
        if ((nextDirectory.equals("..")) || (nextDirectory.equals("../"))) {
            return goToParentDirectory();
        } else {
            String nextPath = Paths.get(currentPath, nextDirectory).toString();
            File fi = new File(nextPath);
            if (!fi.exists()) {
                response.setResponseCode(FTPResponseCode.PERMISSION_DENIED);
                response.setMessage(FTPMessage.FAILED_TO_CHANGE_DIRECTORY);
            } else {
                currentPath = nextPath;
                response.setResponseCode(FTPResponseCode.COMMAND_SUCCESSFULLY);
                response.setMessage(FTPMessage.CHANGED_DIRECTORY);
            }
        }
        return response;
    }

    private FTPResponse goToParentDirectory() {
        FTPResponse response = new FTPResponse();
        if (currentPath.equals(rootPath)) {
            response.setResponseCode(FTPResponseCode.PERMISSION_DENIED);
            response.setMessage(FTPMessage.PERMISSION_DENIED);
        } else {
            File fi = new File(currentPath);
            response.setResponseCode(FTPResponseCode.CLOSING_DATA_CONNECTION_OR_SEND_FILE_FOLDER_OK);
            response.setMessage(FTPMessage.CHANGED_DIRECTORY);
            currentPath = fi.getParent();
        }
        return response;
    }

    // Get relative path of current directory
    private FTPResponse getWorkingDirectory() {
        FTPResponse response = new FTPResponse();
        response.setResponseCode(FTPResponseCode.CREATED_DIRECTORY_OR_GET_WORKING_DIRECTORY);
        response.setMessage(currentPath + " is the current directory");
        return response;
    }

    // List all file, folder in current directory
    private FTPResponse listCurrentDirectory() {
        FTPResponse response = new FTPResponse();
        response.setResponseCode(FTPResponseCode.FILE_STATUS_OK);
        response.setMessage(FTPMessage.DIRECTORY_LISTING);

        List<String> listFileInString = new ArrayList<>();
        File fi = new File(currentPath);
        for (File f : fi.listFiles()) {
            listFileInString.add(getSize(f) + "\t" + formatDate(f.lastModified()) + "\t" + f.getName());
        }
        response.setListFile(listFileInString);
        return response;
    }

    private FTPResponse listSpecificDirectory(String directory) {
        FTPResponse response = new FTPResponse();
        List<String> listFileInString = new ArrayList<String>();
        String path = Paths.get(currentPath, directory).toString();
        File fi = new File(path);
        if (!fi.exists()) {
            response.setResponseCode(FTPResponseCode.PERMISSION_DENIED);
            response.setMessage(FTPMessage.NO_SUCH_FILE_OR_DIRECTORY);
        } else {
            response.setResponseCode(FTPResponseCode.FILE_STATUS_OK);
            response.setMessage(FTPMessage.DIRECTORY_LISTING);
            for (File f : fi.listFiles()) {
                listFileInString.add(getSize(f) + "\t" + formatDate(f.lastModified()) + "\t" + f.getName());
            }
            response.setListFile(listFileInString);
        }
        return response;
    }

    private long getSize(File fi) {
        if (fi.isFile()) {
            return fi.length();
        }
        long S = 0;
        for (File subFile : fi.listFiles()) {
            S += getSize(subFile);
        }
        return S;
    }

    private String formatDate(long ms) {
        Date d = new Date(ms);
        SimpleDateFormat format = new SimpleDateFormat(server.getDatePattern());
        return format.format(d);
    }

    private FTPResponse deleteFolder(String folder) throws IOException {
        FTPResponse response = new FTPResponse();
        Path p = Paths.get(currentPath, folder);
        File fi = new File(p.toString());
        if (fi.exists()) {
            Files.walk(p).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            response.setResponseCode(FTPResponseCode.COMMAND_SUCCESSFULLY);
            response.setMessage(String.format(FTPMessage.DELETED_FOLDER_OR_FILE, folder));
            server.updateActivity(client.getId(), String.format("Deleted %s", folder));
        } else {
            response.setResponseCode(FTPResponseCode.PERMISSION_DENIED);
            response.setMessage(FTPMessage.NO_SUCH_FILE_OR_DIRECTORY);
        }
        return response;
    }

    // OK
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
            List<String> result = new ArrayList<>();
            result.add(content);
            response.setListFile(result);
        }
        return response;
    }

    /*
    private FTPResponse sendOneFile(String fileName) {
        try {
            FTPResponse response = new FTPResponse();
            String path = Paths.get(currentPath, fileName).toString();
            File fi = new File(path);
            if (!fi.exists()) {
                response.setResponseCode(FTPResponseCode.PARAMETER_ERROR);
                response.setMessage(FTPMessage.FILE_NOT_FOUND);
            } else {
                long start = System.currentTimeMillis();
                byte[] byteArr = new byte[(int) fi.length()];
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fi));
                bis.read(byteArr, 0, byteArr.length);
                OutputStream os = outputStream;
                os.write(byteArr, 0, byteArr.length);
                os.flush();
                long end = System.currentTimeMillis();
                float total = (end - start) / 1000.0f;
                response.setResponseCode(FTPResponseCode.FILE_STATUS_OK);
                response.setMessage(String.format("Transferred %d bytes in %d seconds.", byteArr.length, total));
            }
            return response;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
     */

}
