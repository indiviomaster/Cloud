import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    public static Socket socket;
    private static DataInputStream is;
    private static DataOutputStream os;

    public void sendCommand(ActionEvent actionEvent) {
        try {
            os.writeUTF(text.getText());
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Отправлено:"+text.getText());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // TODO: 7/21/2020 init connect to server
        try{
            socket = new Socket("localhost", 8189);
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
            Thread.sleep(1000);
            clientFileList = new ArrayList<>();
            String clientPath = "./client/src/main/resources/";
            File dir = new File(clientPath);
            if (!dir.exists()) {
                throw new RuntimeException("directory resource not exists on client");
            }
            for (File file : Objects.requireNonNull(dir.listFiles())) {
                clientFileList.add(file);
                listView.getItems().add(file.getName());
            }

            Thread clientThread = new Thread(() -> {

                try {
                    while (true) {
                        String command = is.readUTF();
                        if (command.equals("./download")) {
                            String fileNameD = is.readUTF();
                            System.out.println(fileNameD);
                            System.out.println("fileName: " + fileNameD);
                            long fileLength = is.readLong();
                            System.out.println("fileLength: " + fileLength);
                            File fileD = new File(clientPath + fileNameD);
                            if (!fileD.exists()) {
                                fileD.createNewFile();
                            }
                            byte [] buffer = new byte[1024];
                            try (FileOutputStream fos = new FileOutputStream(fileD)) {
                                for (long i = 0; i < (fileLength / 1024 == 0 ? 1 : fileLength / 1024); i++) {
                                    int bytesRead = is.read(buffer);
                                    fos.write(buffer, 0, bytesRead);
                                }
                            }

                            os.writeUTF("файл получен клиентом OK");
                            clientFileList.clear();
                            listView.getItems().clear();
                            File dir2 = new File(clientPath);
                            if (!dir2.exists()) {
                                throw new RuntimeException("directory resource not exists on client");
                            }
                            for (File fileInDir : Objects.requireNonNull(dir2.listFiles())) {
                                clientFileList.add(fileInDir);
                                listView.getItems().add(fileInDir.getName());
                            }
                        } else if (command.equals(".err")){
                            String msgErr = is.readUTF();
                            System.out.println(msgErr);
                        }
                    }
                }

                catch (IOException e) {
                    e.printStackTrace();
                }
            });
            clientThread.start();

            listView.setOnMouseClicked(a -> {
                if (a.getClickCount() == 2) {
                    String fileName = listView.getSelectionModel().getSelectedItem();
                    File currentFile = findFileByName(fileName);
                    if (currentFile != null) {
                        try {
                            os.writeUTF("./upload");
                            os.writeUTF(fileName);
                            os.writeLong(currentFile.length());
                            FileInputStream fis = new FileInputStream(currentFile);
                            byte [] buffer = new byte[1024];
                            while (fis.available() > 0) {
                                int bytesRead = fis.read(buffer);
                                os.write(buffer, 0, bytesRead);
                            }
                            os.flush();
                            String response = is.readUTF();
                            System.out.println(response);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });



        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File findFileByName(String fileName) {
        for (File file : clientFileList) {
            if (file.getName().equals(fileName)){
                return file;
            }
        }
        return null;
    }
}
