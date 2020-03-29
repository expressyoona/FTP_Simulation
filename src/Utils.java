import java.io.Console;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

// Only usable outside IDE
public class Utils {

    public static String inputPassword() {
        Console console = System.console();
        if (Objects.isNull(console)) {
            System.out.println("Couldn't get Console instance");
            System.exit(0);
        }
        char[] passwordArray = console.readPassword();
        return new String(passwordArray);
    }

    public static String getCommand(String fullCommand) {
        return fullCommand.split(" ")[0];
    }

    public static String getCommandParameter(String fullCommand) throws Exception {
        String arr[] = fullCommand.split(" ");
        if (arr.length < 2) {
            throw new Exception("Parameter error");
        }
        return arr[1];
    }

    public static String toMd5(String rawText) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(rawText.getBytes());
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
        System.out.println(Utils.inputPassword());
    }
}
