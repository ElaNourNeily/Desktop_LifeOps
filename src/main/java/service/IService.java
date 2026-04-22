package service;

import java.sql.SQLException;
import java.util.List;

public interface IService <T>{

    void ajouter(T t) throws SQLException;
    void modifier(T t) throws SQLException;
    void supprimer(int id) throws SQLException;

    List<T> recuperer() throws SQLException;

    /** Trier les enregistrements selon un critère métier */
    List<T> trier() throws SQLException;

    /** Rechercher par mot-clé */
    List<T> rechercher(String motCle) throws SQLException;

}
