package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import model.User;
import service.Userservice;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class login {

    @FXML
    private TextField email;

    @FXML
    private PasswordField password;
    Userservice userservice = new Userservice();

    @FXML
    void LogIn(ActionEvent event) {
        try {
            User testingUser = new User();
            testingUser.setEmail(email.getText());
            testingUser.setMot_de_passe(password.getText());

            if(userservice.recherche(testingUser)){
                System.out.println("login successful");
            }else{
                System.out.println("login unsuccessful");
            }
        }catch (SQLException e){
            System.out.println(e.getMessage());
        }
    }
}
