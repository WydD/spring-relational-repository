package fr.petitl.relational.repository.template.bean;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 */
public class VoidBeanAttributeMapper implements BeanAttributeReader, BeanAttributeWriter {
    private VoidBeanAttributeMapper() {

    }

    @Override
    public void writeAttribute(PreparedStatement ps, int column, Object o, Field sourceField) throws SQLException {
        throw new IllegalStateException("Nothing to see here... it should be replaced by the dialect");
    }

    @Override
    public Object readAttribute(ResultSet rs, int column, Field sourceField) throws SQLException {
        throw new IllegalStateException("Nothing to see here... it should be replaced by the dialect");
    }
}
