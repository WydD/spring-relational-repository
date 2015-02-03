package fr.petitl.relational.repository.repository;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.springframework.jdbc.core.StatementCreatorUtils.javaTypeToSqlParameterType;
import static org.springframework.jdbc.core.StatementCreatorUtils.setParameterValue;

public interface BeanAttributeWriter {
    public default void writeAttribute(PreparedStatement ps, int column, Object o, Field sourceField) throws SQLException {
        setParameterValue(ps, column, javaTypeToSqlParameterType(sourceField == null ? o.getClass() : sourceField.getType()), o);
    }

    public static class Default implements BeanAttributeWriter {
        public static final BeanAttributeWriter INSTANCE = new Default();
        private Default() {}
    }
}