import com.sun.xml.internal.ws.api.model.wsdl.WSDLOutput;

import java.io.*;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class ConnectionHandler implements Runnable {

    private DataInputStream is;
    private DataOutputStream os;

    public ConnectionHandler(Socket socket) throws IOException, InterruptedException {
        System.out.println("Connection accepted");
        is = new DataInputStream(socket.getInputStream());
        os = new DataOutputStream(socket.getOutputStream());
        Thread.sleep(2000);
    }

    @Override
    public void run() {
        byte [] buffer = new byte[1024];
        while (true) {
            try {
                String command = is.readUTF();
                if (command.equals("./upload")) {
                    String fileName = is.readUTF();
                    System.out.println("fileName: " + fileName);
                    long fileLength = is.readLong();
                    System.out.println("fileLength: " + fileLength);
                    File file = new File(Server.serverPath + "/" + fileName);
                    if (!file.exists()) {
                        file.createNewFile();
                    }
                    try(FileOutputStream fos = new FileOutputStream(file)) {
                        for (long i = 0; i < (fileLength / 1024 == 0 ? 1 : fileLength / 1024); i++) {
                            int bytesRead = is.read(buffer);
                            fos.write(buffer, 0, bytesRead);
                        }
                    }
                    os.writeUTF("File upload on server. OK");
                }else if(command.startsWith("./download")){
                    String fileNameToDownload = command.split("\\s")[1];
                    System.out.println("запрашиваемый файл "+fileNameToDownload);

                    File fileToDownload = new File(Server.serverPath + "/" + fileNameToDownload);
                    if (fileToDownload.exists()) {
                        if (fileNameToDownload != null) {
                            try {
                                os.writeUTF("./download");
                                os.writeUTF(fileNameToDownload);
                                os.writeLong(fileToDownload.length());
                                FileInputStream fis = new FileInputStream(fileToDownload);
                                byte [] bufferDown = new byte[1024];
                                while (fis.available() > 0) {
                                    int bytesRead = fis.read(bufferDown);
                                    os.write(bufferDown, 0, bytesRead);
                                }
                                os.flush();
                                String response = is.readUTF();
                                System.out.println(response);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                    }else {
                        os.writeUTF(".err");
                        os.writeUTF("no such file to download");
                    }
                }
                else {

                    System.out.println(command);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
