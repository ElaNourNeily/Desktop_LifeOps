package test;

import model.User;
import service.Userservice;
import utils.MyDB;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class main {
    static void main() {
        Userservice userservice = new Userservice();

       /* User u = new User("naim","zarbabou","na3im@gmail.esprit",200 , "123456");
       Userservice userservice = new Userservice();

       try {
            List<User> users = userservice.findAll();
            for (User user : users) {
                System.out.println(user.getNom()+"  "+user.getPrenom()+"  "+user.getEmail());
            }
        }catch (SQLException e){
            System.out.println(e.getMessage());        } */
       /* try{
            System.out.println(userservice.findbyID(19).getPrenom());
        }catch(Exception e){
            System.out.println(e.getMessage());
        }*/

      /*  try {
            List<User> users = userservice.sortbyName();
            for (User user : users) {
                System.out.println(user.getNom()+"  "+user.getPrenom()+"  "+user.getEmail());
        }
    }catch (SQLException e){
            System.out.println(e.getMessage());}*/
    }


}
