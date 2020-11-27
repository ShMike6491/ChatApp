package client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

// TODO close socket connection on x button press
public class Controller implements Initializable {
    @FXML
    public TextArea textArea;
    @FXML
    public TextField textField;
    @FXML
    public HBox authPanel;
    @FXML
    public HBox textPanel;
    @FXML
    public TextField loginField;
    @FXML
    public PasswordField passwordField;

    private static final int PORT = 8189;
    private final static String HOST = "localhost";

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private boolean isConnected;
    private boolean isAuthenticated;
    private String nickname;
    private final String TITLE = "GeekChat";

    // отрисовка полей
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setAuthentication(false);
    }

    // основной код работы с сервиром
    private void connection() {
        try {
            socket = new Socket(HOST, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            isConnected = true;

            new Thread(() -> {
                try {
                    // проверка аутентификации
                    while(true) {
                        String msg = in.readUTF();
                        if(msg.startsWith("/authok")) {
                            String [] res = msg.split("\\s");
                            nickname = res[1];
                            setAuthentication(true);
                            break;
                        }
                    }
                    // основной поток
                    while (true) {
                        String msg = in.readUTF();
                        if (msg.equals("/exit")) {
                            textArea.appendText("You have been disconnected" + "\n");
                            isConnected = false;
                            break;
                        }
                        textArea.appendText(msg + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    setAuthentication(false);
                    System.out.println("Disconnected");
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // установка видимости полей при авторизации
    public void setAuthentication (boolean isAuthenticated) {
        this.isAuthenticated = isAuthenticated;
        authPanel.setVisible(!isAuthenticated);
        authPanel.setManaged(!isAuthenticated);

        textPanel.setVisible(isAuthenticated);
        textPanel.setManaged(isAuthenticated);

        if(!isAuthenticated) {
            nickname = "";
        }

        setTitle(nickname);
    }

    // меняем заголовок при авторизации
    private void setTitle (String name) {
        Platform.runLater(() -> {
            ((Stage)textField.getScene().getWindow()).setTitle(TITLE + " " + name);
        });
    }

    public void sendMsg(ActionEvent actionEvent) throws IOException {
        if(isConnected) {
            out.writeUTF(textField.getText());
            textField.clear();
            textField.requestFocus();
        } else {
            textArea.appendText("You have been disconnected");
        }
    }

    // метод вызываемый из sample.fxml при попытке авторизации
    // (является точкой входа в программу и запускает код)
    public void login(ActionEvent actionEvent) throws IOException {
        if(socket == null || socket.isClosed()) {
            connection();
        }
        out.writeUTF(String.format("/auth %s %s", loginField.getText().trim().toLowerCase(),
                passwordField.getText().trim()));
        passwordField.clear();
    }
}
