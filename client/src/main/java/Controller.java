import com.geekbrains.cloud.common.AbstractMessage;
import com.geekbrains.cloud.common.CommandMessage;
import com.geekbrains.cloud.common.FileMessage;
import com.geekbrains.cloud.common.FileRequest;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    //private static final Logger logger = LoggerFactory.getLogger(Controller.class);

    @FXML
    public Button send;
    @FXML
    public ListView<String> listView;
    @FXML
    public ListView<String> listViewS;
    @FXML
    public TextField text;
    @FXML
    private List<File> clientFileList;

    private static Socket socket;
    private static ObjectEncoderOutputStream outStream;
    private static ObjectDecoderInputStream inStream;

    String clientPath = "./client/src/main/resources/";

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        try {
            socket = new Socket("localhost", 8189);
            outStream = new ObjectEncoderOutputStream(socket.getOutputStream());
            inStream = new ObjectDecoderInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        sendMessage(new CommandMessage("/list"));

        refreshListView();

        Thread readThread = new Thread(()->{
            try{
                while(true){
                    AbstractMessage incomeMessage = null;
                    try {
                        incomeMessage = readMessage();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    if(incomeMessage instanceof FileMessage){
                        FileMessage fileMessage = (FileMessage) incomeMessage;
                        //пишем в файл;
                            try {
                                FileOutputStream fileOutputStream = new FileOutputStream(clientPath+ fileMessage.getFilename());
                                fileOutputStream.write(fileMessage.getData());
                                fileOutputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        System.out.println("файл" +fileMessage.getFilename()+"записан");
                        //файл записан
                        //logger
                    }else if(incomeMessage instanceof CommandMessage){
                        CommandMessage commandMessage = (CommandMessage) incomeMessage;
                        //Обновляем список файлов

                        if(commandMessage.getCommand().startsWith("/list")){
                            System.out.println("income list");
                            String[] tokens = commandMessage.getCommand().split("\\s");



                            Platform.runLater(()->{
                                listViewS.getItems().clear();
                                for ( String tok:tokens){
                                    if(!tok.equals("/list"))
                                    {
                                        listViewS.getItems().add(tok);
                                    }

                                }

                            });

                        }
                    }
                    refreshListView();
                }
            } catch (IOException ex){
                ex.printStackTrace();
            }
            finally {
                close();
            }
        });
        readThread.setDaemon(true);
        readThread.start();
    }
    public void pressOnUpdate(ActionEvent actionEvent){
        System.out.println("BtnUpdate copy from");
        MultipleSelectionModel<String> listSelect = listViewS.getSelectionModel();
        String file = String.valueOf(listSelect.getSelectedItem());
        sendMessage(new FileRequest(file));

    }

    public void pressOnSend(ActionEvent actionEvent){
        System.out.println("BtnSend");
        MultipleSelectionModel<String> listSelect = listView.getSelectionModel();
        String file = String.valueOf(listSelect.getSelectedItem());
        Path path = Paths.get(clientPath+file);
        if (Files.exists(path)){
            try {
            FileMessage outFileMessage = new FileMessage(path);
            sendMessage(outFileMessage);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


    }
    public void pressOnDelete(ActionEvent actionEvent){
        System.out.println("BtnDelete");
        MultipleSelectionModel<String> listSelect = listViewS.getSelectionModel();
        String file = String.valueOf(listSelect.getSelectedItem());
        /*Path path = Paths.get(clientPath+file);
        if (Files.exists(path)){
        */
            /*try {
                Files.delete(path);
            } catch (IOException e) {
                e.printStackTrace();
            }*/
            sendMessage(new CommandMessage("/delete "+file));
        //}
        System.out.println("Файл"+file+"удален на сервере");

        //refreshListView();
    }

    public static int sendMessage(AbstractMessage message){
        try {
            outStream.writeObject(message);
            return 1;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static void close() {
        try {
            inStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static AbstractMessage readMessage() throws IOException, ClassNotFoundException {
        return (AbstractMessage) inStream.readObject();
    }
    // обновление списка файлов на сервере
    public void refreshListViewS(){
        Platform.runLater(()->{
            try {
                listViewS.getItems().clear();
                Files.list(Paths.get(clientPath))
                        .filter(path -> !Files.isDirectory(path))
                        .map(file->file.getFileName().toString())
                        .forEach(filename->listViewS.getItems().add(filename));
            } catch (IOException e) {
                e.printStackTrace();
            }

        });
    }
    // обновление списка файлов на клиенте
    public void refreshListView(){
        Platform.runLater(()->{
            try {
                listView.getItems().clear();
                Files.list(Paths.get(clientPath))
                        .filter(path -> !Files.isDirectory(path))
                        .map(file->file.getFileName().toString())
                        .forEach(filename->listView.getItems().add(filename));
            } catch (IOException e) {
                e.printStackTrace();
            }

        });
    }

    public void sendCommand(ActionEvent actionEvent) {


    }
}
/*
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
*/


