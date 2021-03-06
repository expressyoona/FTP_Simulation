import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;

public class FTPDataSender extends Thread {

    private ServerSocket serverSocket;
    private File myFile;

    public FTPDataSender(int port, String file) {
        try {
            // System.out.println("[SENDER] Serving at port " + port);
            myFile = new File(file);
            serverSocket = new ServerSocket(port);
        } catch (BindException b) {
            System.out.println("Port already in use: " + port);
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
            serverSocket.close();
            // System.out.println("Sent & closed socket...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
