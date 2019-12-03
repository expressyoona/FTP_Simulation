public class FTPCommand {
    public static final String CONNECT = "connect";
    public static final String MAKE_DIRECTORY = "mkdir";
    public static final String CHANGE_DIRECTORY = "cd";
    public static final String GO_UP_DIRECTORY = "cdup";
    public static final String REMOVE_FILE = "delete";
    public static final String REMOVE_DIRECTORY = "rmdir";
    public static final String RENAME_FILE = "rename";
    public static final String READ_ONE_FILE = "cat";
    public static final String LIST_DIRECTORY = "ls"; // --> 226: Directory send OK.

    public static final String GET_WORKING_DIRECTORY = "pwd"; // --> 257: "/path/to/file" is the current directory
    public static final String SPECIFY_PASSWORD = "password";
}
