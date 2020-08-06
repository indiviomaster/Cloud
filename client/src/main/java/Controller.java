import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    public Button send;
    @FXML
    public ListView<String> listView;
    @FXML
    public TextField text;
    @FXML
    private List<File> clientFileList;

    protected static SocketChannel socketChannel;
    String clientPath = "./client/src/main/resources/";
    public void sendCommand(ActionEvent actionEvent) {

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        //try{
            InetSocketAddress serverAddress = new InetSocketAddress("localhost", 8189);
            try {
                SocketChannel socketChannel = SocketChannel.open(serverAddress);

                System.out.println("send file start");
                try {

                    RandomAccessFile randomAccessFile = new RandomAccessFile(clientPath+"djud.jpg","rw");
                    FileChannel fileChannel = randomAccessFile.getChannel();
                    ByteBuffer buffer = ByteBuffer.allocate(1024); // nio buffer
                    int bytesRead = fileChannel.read(buffer); // count byte read to buffer
                    while (bytesRead > -1) {
                        buffer.flip(); //
                        while (buffer.hasRemaining()) {

                            socketChannel.write(buffer);
                        }
                        buffer.clear();
                        bytesRead = fileChannel.read(buffer);
                    }
                    randomAccessFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("send file complete");



        } catch (Exception e) {
            e.printStackTrace();
        }
    }




}
