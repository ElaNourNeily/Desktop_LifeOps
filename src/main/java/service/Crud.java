package service;

import java.sql.SQLException;
import java.util.List;

public interface Crud<T> {
    void ajouter(T t) throws SQLException;
    void modifier(T t) throws SQLException;
    void supprimer(int id) throws SQLException;
    List<T> recuperer() throws SQLException;
    List<T> trier();
    List<T> rechercher(String critere);
    T findbyID(int i) throws SQLException;
    T findbyMail(String mail) throws SQLException;
    List<T> findAll() throws SQLException;
    List<T> sortbyName() throws SQLException;
}