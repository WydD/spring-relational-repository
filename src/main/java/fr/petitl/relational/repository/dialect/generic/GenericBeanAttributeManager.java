package fr.petitl.relational.repository.dialect.generic;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import fr.petitl.relational.repository.template.bean.BeanAttributeReader;
import fr.petitl.relational.repository.template.bean.BeanAttributeWriter;
import org.springframework.jdbc.support.JdbcUtils;

import static org.springframework.jdbc.core.StatementCreatorUtils.javaTypeToSqlParameterType;
import static org.springframework.jdbc.core.StatementCreatorUtils.setParameterValue;

public class GenericBeanAttributeManager implements BeanAttributeReader, BeanAttributeWriter {
    public static final GenericBeanAttributeManager INSTANCE = new GenericBeanAttributeManager();

    public void writeAttribute(PreparedStatement ps, int column, Object o, Field sourceField) throws SQLException {
        setParameterValue(ps, column, javaTypeToSqlParameterType(sourceField == null ? o.getClass() : sourceField.getType()), o);
    }

    public Object readAttribute(ResultSet rs, int column, Field sourceField) throws SQLException {
        return JdbcUtils.getResultSetValue(rs, column, sourceField.getType());
    }

    private GenericBeanAttributeManager() {
    }
}