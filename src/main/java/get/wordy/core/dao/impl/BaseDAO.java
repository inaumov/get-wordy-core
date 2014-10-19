package get.wordy.core.dao.impl;

import get.wordy.core.api.db.IConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class BaseDao {

    private IConnectionFactory connectionFactory;

    BaseDao(IConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    Connection getConnection() {
        return connectionFactory.get();
    }

    PreparedStatement prepare(String sql) throws SQLException {
        return getConnection().prepareStatement(sql);
    }

    PreparedStatement prepareInsert(String sql) throws SQLException {
        return getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    }

}