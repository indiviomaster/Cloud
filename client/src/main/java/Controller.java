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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    private static final Logger LOGGER = LogManager.getLogger(Controller.class);

    @FXML
    public Button btnUpdateListOnClient;
    @FXML
    public Button btnUpdateListOnServer;
    @FXML
    public Button btnDeleteFromClient;
    @FXML
    public Button btnSendToServer;
    @FXML
    public Button btnCopyFromServer;
    @FXML
    public Button btnDeleteFromServer;
    @FXML
    public ListView<String> listViewClient;
    @FXML
    public ListView<String> listViewServer;

    private List<File> clientFileList;
    private static Socket socket;
    private static ObjectEncoderOutputStream outStream;
    private static ObjectDecoderInputStream inStream;

    String clientPath = "./ClientFiles/";

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        try {
            socket = new Socket("localhost", 8189);
            outStream = new ObjectEncoderOutputStream(socket.getOutputStream());
            inStream = new ObjectDecoderInputStream(socket.getInputStream());
            LOGGER.info("Соединение установлено");
        } catch (IOException e) {
            LOGGER.error("Ошибка соединения ", e);
        }

        sendMessage(new CommandMessage("/list"));

        refreshListViewClient();

        Thread readThread = new Thread(()->{
            try{
                while(true){
                    AbstractMessage incomeMessage = null;
                    try {
                        incomeMessage = readMessage();
                        LOGGER.debug("Пришло входящее сообщение");
                    } catch (ClassNotFoundException e) {
                        LOGGER.error("Прислан неправильный тип данных", e);
                    }
                    if(incomeMessage instanceof FileMessage){
                        FileMessage fileMessage = (FileMessage) incomeMessage;
                        //пишем в файл;
                            try {
                                FileOutputStream fileOutputStream = new FileOutputStream(clientPath+ fileMessage.getFilename());
                                fileOutputStream.write(fileMessage.getData());
                                fileOutputStream.close();
                            } catch (IOException e) {
                                LOGGER.error("Ошибка ввода вывода", e);
                            }
                        LOGGER.info("Файл " +fileMessage.getFilename()+" записан на клиенте");



                    }else if(incomeMessage instanceof CommandMessage){
                        CommandMessage commandMessage = (CommandMessage) incomeMessage;
                        //Обновляем список файлов

                        if(commandMessage.getCommand().startsWith("/list")){
                            LOGGER.info("Пришел запрос списка файлов");
                            String[] tokens = commandMessage.getCommand().split(";");
                            Platform.runLater(()->{
                                listViewServer.getItems().clear();
                                for ( String tok:tokens){
                                    if(!tok.equals("/list"))
                                    {
                                        listViewServer.getItems().add(tok);
                                    }
                                }
                            });
                        }
                    }
                    refreshListViewClient();
                }
            } catch (IOException e){
                LOGGER.error("Ошибка ввода вывода", e);
            }
            finally {
                close();
            }
        });
        readThread.setDaemon(true);
        readThread.start();
    }

    public void pressOnCopyFrom(ActionEvent actionEvent){

        MultipleSelectionModel<String> listSelect = listViewServer.getSelectionModel();
        String file = String.valueOf(listSelect.getSelectedItem());
        if(!file.equals("null")){
            sendMessage(new FileRequest(file));
            LOGGER.info("Нажата кнопка CopyFromServer, скопирован файл = "+file);
        }
    }

    public void pressOnSend(ActionEvent actionEvent){

        MultipleSelectionModel<String> listSelect = listViewClient.getSelectionModel();
        String file = String.valueOf(listSelect.getSelectedItem());
        if(!file.equals("null")){


            Path path = Paths.get(clientPath+file);
            if (Files.exists(path)){
                try {
                    FileMessage outFileMessage = new FileMessage(path);
                    sendMessage(outFileMessage);
                    LOGGER.info("Нажата кнопка BtnSend, отправлен файл = "+file);
                } catch (FileNotFoundException e) {
                    LOGGER.error("Файл не найден", e);
                } catch (IOException e) {
                    LOGGER.error("Ошибка ввода вывода", e);
                }

            }
        }
    }
    public void pressDeleteOnServer(ActionEvent actionEvent){

        MultipleSelectionModel<String> listSelect = listViewServer.getSelectionModel();
        String file = String.valueOf(listSelect.getSelectedItem());
        if(!file.equals("null")) {
            sendMessage(new CommandMessage("/delete;" + file));
            LOGGER.info("Нажата кнопка BtnDelete, на сервере удален файл = "+file);
        }else {
            LOGGER.info("В окне сервера не выбран файл");
        }

    }

    public void pressDeleteOnClient(ActionEvent actionEvent){


        MultipleSelectionModel<String> listSelect = listViewClient.getSelectionModel();
        String file = String.valueOf(listSelect.getSelectedItem());
        if(!file.equals("null")) {

            Path path = Paths.get(clientPath + file);

            if (Files.exists(path)) {

                try {
                    Files.delete(path);
                    LOGGER.info("Нажата кнопка Delete From Client, на клиенте удален файл = "+file);
                } catch (IOException e) {
                    LOGGER.error("Ошибка ввода вывода", e);
                }
            }
        }else {
            LOGGER.info("В окне клиента не выбран файл");
        }

        refreshListViewClient();
    }

    public static int sendMessage(AbstractMessage message){
        try {
            outStream.writeObject(message);
            LOGGER.info("Сообщение отправлено");
            return 1;

        } catch (IOException e) {
            LOGGER.error("Ошибка ввода вывода", e);
        }
        return 0;
    }

    public static void close() {
        try {
            inStream.close();
            LOGGER.info("Закрыт поток in");
        } catch (IOException e) {
            LOGGER.error("Ошибка закрытия потока in", e);
        }

        try {
            outStream.close();
            LOGGER.info("Закрыт поток out");
        } catch (IOException e) {
            LOGGER.error("Ошибка закрытия потока out", e);
        }

        try {
            socket.close();
            LOGGER.info("Закрыто соединение");
        } catch (IOException e) {
            LOGGER.error("Ошибка закрытия соединения", e);
        }
    }

    public static AbstractMessage readMessage() throws IOException, ClassNotFoundException {
        return (AbstractMessage) inStream.readObject();
    }

    // обновление списка файлов на сервере
    public void refreshListViewServer(){
        sendMessage(new CommandMessage("/list"));
        LOGGER.info("Запрос обновления списка файлов на сервере");

    }

    // обновление списка файлов на клиенте
    public void refreshListViewClient(){
        Platform.runLater(()->{
            try {
                listViewClient.getItems().clear();
                Files.list(Paths.get(clientPath))
                        .filter(path -> !Files.isDirectory(path))
                        .map(file->file.getFileName().toString())
                        .forEach(filename->listViewClient.getItems().add(filename));
                LOGGER.info("Запрос обновления списка файлов на клиенте");
            } catch (IOException e) {
                LOGGER.error("Ошибка ввода вывода", e);
            }

        });
    }
}
