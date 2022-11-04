package com.example.sudoku;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class RegisterController {
    public Button loginButton;
    public TextField usernameTf;
    public TextField passwordTf;
    public Button registerButton;
    public TextField cpasswordTf;
    public Scene scene;
    public Stage stage;
    public Label consoleLabel;

    public void onRegisterButtonClick(ActionEvent event) {

        try {
            if (!cpasswordTf.getText().equals(passwordTf.getText())) {
                consoleLabel.setText("Passwords don't match");
                return;
            }

            // register to database
            Connection conn = DriverManager.getConnection("jdbc:mysql://sql12.freesqldatabase.com:3306/sql12531423", "sql12531423", "LACEJ2SjGm");
            Statement statement = conn.createStatement();

            String query = "INSERT INTO USERS VALUES ( '" + usernameTf.getText() + "' , '" + passwordTf.getText() + "' )";
            statement.executeUpdate(query);
            consoleLabel.setText("Registered Successfully");
        }catch (SQLException e){
            consoleLabel.setText("Registration Failed");
        }
    }

    public void onLoginButtonClick(ActionEvent event) throws IOException {
        stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
        FXMLLoader fxmlLoader = new FXMLLoader(StartPageController.class.getResource("LoginPage.fxml"));
        scene = new Scene(fxmlLoader.load());
        stage.setScene(scene);
        stage.show();
    }
}