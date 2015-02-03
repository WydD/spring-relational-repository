package fr.petitl.relational.repository.repository;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.support.JdbcUtils;

import static org.springframework.jdbc.core.StatementCreatorUtils.javaTypeToSqlParameterType;
import static org.springframework.jdbc.core.StatementCreatorUtils.setParameterValue;

public interface BeanAttributeReader {
    public default Object readAttribute(ResultSet rs, int column, Field sourceField) throws SQLException {
        return JdbcUtils.getResultSetValue(rs, column, sourceField.getType());
    }

    public static class Default implements BeanAttributeReader {
        public static final BeanAttributeReader INSTANCE = new Default();
        private Default() {}
    }
}