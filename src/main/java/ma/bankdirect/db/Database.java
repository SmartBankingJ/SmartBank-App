package ma.bankdirect.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import ma.bankdirect.config.AppConfig;

public class Database {
    private Database(){}

    public static Connection getConnection() throws SQLException{
        return DriverManager.getConnection(
            AppConfig.get("db.url"),
            AppConfig.get("db.user"),
            AppConfig.get("db.password")
        );
    }
}
