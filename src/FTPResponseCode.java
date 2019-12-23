public class FTPResponseCode {
    public static final int FILE_STATUS_OK = 150;

    public static final int OKAY = 200;
    public static final int DIRECTORY_STATUS = 211;
    public static final int FILE_STATUS = 212;
    public static final int SERVICE_READY = 220; // When a new user send a request
    public static final int LOGGED_OUT = 221; // when user close/disconnect
    public static final int HELP_MESSAGE = 214;
    public static final int CLOSING_DATA_CONNECTION_OR_SEND_FILE_FOLDER_OK = 226; // Requested file
    public static final int USER_LOGGED_IN = 230;
    public static final int COMMAND_SUCCESSFULLY = 250;
    public static final int CREATED_DIRECTORY_OR_GET_WORKING_DIRECTORY = 257; // Or get cwd

    public static final int USERNAME_OK_NEED_PASSWORD = 331; // When username is existed
    public static final int NEED_LOG_IN = 332;

    public static final int SERVICE_NOT_AVAILABLE = 421;

    public static final int SYNTAX_ERROR = 500;
    public static final int PARAMETER_ERROR = 501;
    public static final int COMMAND_NOT_IMPLEMENTED = 502;
    public static final int BAD_SEQUENCE = 503;
    public static final int NOT_LOGGED_IN = 530; // Wrong username or password
    public static final int PERMISSION_DENIED = 550; // Not allowed, failed to change directory
    public static final int INVALID_FILENAME = 553;

    public static final int READY_FOR_TRANSFER = 2019;
}
