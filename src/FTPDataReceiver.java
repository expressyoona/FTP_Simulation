import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.BindException;
import java.net.Socket;

public class FTPDataReceiver extends Thread {

    private Socket socket;
    private String fileName;

    public FTPDataReceiver(String host, int port, String fileName) {
        try {
            // System.out.println("[RECEIVER] " + host + ":" + port);
            this.socket = new Socket(host, port);
            this.fileName = fileName;
        } catch (BindException b) {
            System.out.println("Port already in use: " + port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            byte[] mybytearray = new byte[1024];
            InputStream is = socket.getInputStream();
            FileOutputStream fos = new FileOutputStream(fileName);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            int bytesRead = is.read(mybytearray, 0, mybytearray.length);
            bos.write(mybytearray, 0, bytesRead);
            bos.close();
            socket.close();
            // System.out.println("Received & closed socket...");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}


