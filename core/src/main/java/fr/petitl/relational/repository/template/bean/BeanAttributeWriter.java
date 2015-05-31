package fr.petitl.relational.repository.template.bean;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface BeanAttributeWriter {
    void writeAttribute(PreparedStatement ps, int column, Object o, Field sourceField) throws SQLException;
}