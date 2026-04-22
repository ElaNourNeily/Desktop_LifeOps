package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import model.User;
import service.Userservice;

public class SignIn {

    @FXML
    private TextField prenom;

    @FXML
    private TextField nom;

    @FXML
    private TextField age;
    @FXML
    private Label p;

    @FXML
    private TextField telephone;

    @FXML
    private TextField email;

    @FXML
    private PasswordField password;

    private Userservice userservice = new Userservice();

    @FXML
    void SignUp(ActionEvent event) {
        int x = 0;
        try {
            User newUser = new User();
            if (prenom.getText().matches("^[a-zA-Z].*")){
                newUser.setPrenom(prenom.getText());
                x+=1;
                newUser.setNom(nom.getText());

            }else {
                p.setVisible(true);
            }
            if (nom.getText().matches("^[a-zA-Z].*")){
                newUser.setNom(nom.getText());
                x+=1;

            }

            
            if (!age.getText().isEmpty()) {
                newUser.setAge(Integer.parseInt(age.getText()));
                x+=1;

            }
            if (email.getText().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                newUser.setEmail(email.getText());
                x+=1;

            } else {
                System.out.println("Invalid email format");
            }
            if(password.getText().length()>5){
                newUser.setMot_de_passe(password.getText());
                x+=1;


            }else System.out.println("Invalid password format");
            if (x==5){
                userservice.create(newUser);
                System.out.println("Sign in successful!");
            }

        } catch (Exception e) {
            System.out.println("Sign in Failed: " + e.getMessage());
        }
    }
}
