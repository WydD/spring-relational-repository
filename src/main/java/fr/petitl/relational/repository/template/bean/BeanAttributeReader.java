package fr.petitl.relational.repository.template.bean;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface BeanAttributeReader {
    public Object readAttribute(ResultSet rs, int column, Field sourceField) throws SQLException;

}