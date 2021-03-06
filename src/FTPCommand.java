public class FTPCommand {
    public static final String USER = "user";
    public static final String PASS = "pass";
    public static final String MAKE_DIRECTORY = "mkdir";
    public static final String CHANGE_DIRECTORY = "cd";
    public static final String GO_UP_DIRECTORY = "cdup";

    public static final String READ_ONE_FILE = "cat";
    public static final String LIST_DIRECTORY = "ls"; // --> 226: Directory send OK.
    public static final String GET_ONE_FILE = "get";
    public static final String SEND_ONE_FILE = "put";
    public static final String EXIT = "exit";
    public static final String QUIT = "quit";
    public static final String BYE = "bye";

    public static final String GET_WORKING_DIRECTORY = "pwd"; // --> 257: "/path/to/file" is the current directory

    // Commands need write permission
    public static final String REMOVE_FILE = "delete";
    public static final String REMOVE_DIRECTORY = "rmdir";
    public static final String RENAME_FILE = "rename";
}
