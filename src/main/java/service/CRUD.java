package service;

import java.sql.SQLException;
import java.util.List;

public interface CRUD <T>{
    void create(T t) throws SQLException;
    void update(T t) throws SQLException;
    void delete(T t) throws SQLException;
    T findbyID(int i) throws SQLException;
    T findbyMail(String mail) throws SQLException;
    List<T> findAll() throws SQLException;
    List<T> sortbyName() throws SQLException;


}
