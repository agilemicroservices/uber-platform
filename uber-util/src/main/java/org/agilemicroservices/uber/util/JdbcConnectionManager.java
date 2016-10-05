package org.agilemicroservices.uber.util;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;


public class JdbcConnectionManager {
    private DataSource dataSource;
    private Connection connection;


    public JdbcConnectionManager(DataSource dataSource) {
        this.dataSource = dataSource;
        invalidate();
    }


    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            // TODO log warning
        }
    }


    public void invalidate() {
        if (null != connection) {
            try {
                connection.close();
            } catch (Exception e) {
                // ignore
            }
        }

        try {
            connection = dataSource.getConnection();
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
