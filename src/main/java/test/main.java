package test;

import service.user.UserService;
import model.user.User;
import java.sql.SQLException;
import java.util.List;

public class main {
    public static void main(String[] args) {
        UserService userservice = new UserService();
        mainfx.main(args);

       /* User u = new User("naim","zarbabou","na3im@gmail.esprit",200 , "123456");
       UserService userservice = new UserService();

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
