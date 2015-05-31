package fr.petitl.relational.repository.dialect.generic;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import fr.petitl.relational.repository.template.bean.BeanAttributeMapper;
import org.springframework.jdbc.core.StatementCreatorUtils;
import org.springframework.jdbc.support.JdbcUtils;

public class SpringJDBCAttributeWriter implements BeanAttributeMapper {
    public void writeAttribute(PreparedStatement ps, int column, Object o, Field sourceField) throws SQLException {
        int sqlType = StatementCreatorUtils.javaTypeToSqlParameterType(sourceField == null ? o.getClass() : sourceField.getType());
        StatementCreatorUtils.setParameterValue(ps, column, sqlType, o);
    }

    public Object readAttribute(ResultSet rs, int column, Field sourceField) throws SQLException {
        return JdbcUtils.getResultSetValue(rs, column, sourceField.getType());
    }
}