import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/*
 * Wait for connecting
 * And send file
 */

public class FTPDataSender extends Thread {

    private ServerSocket serverSocket;
    private File myFile;

    public FTPDataSender(int port, String file) {
        try {
            myFile = new File(file);
            serverSocket = new ServerSocket(port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            Socket clientSocket = serverSocket.accept();
            byte[] mybytearray = new byte[(int) myFile.length()];
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(myFile));
            bis.read(mybytearray, 0, mybytearray.length);
            OutputStream os = clientSocket.getOutputStream();
            os.write(mybytearray, 0, mybytearray.length);
            os.flush();
            clientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
