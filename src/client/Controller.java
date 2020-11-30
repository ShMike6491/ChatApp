package client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
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
    @FXML
    public ListView clientList;

    private static final int PORT = 8189;
    private final static String HOST = "localhost";

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    // поле для закрытия программы (обрабатывается в графическом потоке)
    private Stage stage;
    private Stage regStage;
    private Signup signup;

    private boolean isConnected;
    private boolean isAuthenticated;
    private String nickname;
    private final String TITLE = "GeekChat";

    // отрисовка полей
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setAuthentication(false);
        try {
            createRegWindow();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Platform.runLater(() -> {
            endConnection();
        });
    }

    //метод для отключения от сервера после закрытия программы
    private void endConnection() {
        stage = (Stage)textField.getScene().getWindow();
        stage.setOnCloseRequest((WindowEvent event) -> {
            try {
                if(isAuthenticated)
                    out.writeUTF("/exit");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
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
                        if(msg.startsWith("/200")) {
                            signup.addMessage("Successful registration");
                        }
                        if(msg.startsWith("/400")) {
                            signup.addMessage("Failed registration");
                        }
                    }
                    // основной поток
                    while (true) {
                        String msg = in.readUTF();
                        if (msg.startsWith("/")) {

                            if (msg.equals("/exit")) {
                                textArea.appendText("You have been disconnected" + "\n");
                                isConnected = false;
                                break;
                            }

                            // добавляем активных пользователей в поле юзеров
                            if (msg.startsWith("/client")) {
                                String[] token = msg.split("\\s+");
                                Platform.runLater(() -> {
                                    clientList.getItems().clear();
                                    for (int i = 1; i < token.length; i++) {
                                        clientList.getItems().add(token[i]);
                                    }
                                });
                            }

                        } else {
                            textArea.appendText(msg + "\n");
                        }

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
        clientList.setVisible(isAuthenticated);
        clientList.setManaged(isAuthenticated);

        if(!isAuthenticated) {
            nickname = "";
        }
        textArea.clear();
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

    public void personalMsg(MouseEvent mouseEvent) {
        String name = (String) clientList.getSelectionModel().getSelectedItem();
        textField.appendText("/w " + name + " ");
    }

    // метод создает окно для регистрации
    private void createRegWindow() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("signup.fxml"));
        Parent root = fxmlLoader.load();
        regStage = new Stage();
        regStage.setTitle("Sign Up");
        regStage.setScene(new Scene(root, 400, 250));

        signup = fxmlLoader.getController();
        signup.setController(this);

        regStage.initModality(Modality.APPLICATION_MODAL);
    }

    public void register(String login, String password, String nickname) throws IOException {
        String msg = String.format("/reg %s %s %s", login, password, nickname);

        if(socket == null || socket.isClosed()) {
            connection();
        }

        out.writeUTF(msg);
    }

    public void register(ActionEvent actionEvent) {
        regStage.show();
    }
}
