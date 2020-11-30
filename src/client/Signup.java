package client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.IOException;

public class Signup {
    @FXML
    public TextField loginField;
    @FXML
    public PasswordField passwordField;
    @FXML
    public TextField nickField;
    @FXML
    public TextArea textArea;

    private Controller controller;

    public void signUp(ActionEvent actionEvent) throws IOException {
        controller.register(loginField.getText().trim().toLowerCase(), passwordField.getText(), nickField.getText().trim().toLowerCase());
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }

    public void addMessage(String msg) {
        textArea.appendText(msg + "\n");
    }
}
