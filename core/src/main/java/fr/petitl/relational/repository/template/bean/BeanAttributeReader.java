package fr.petitl.relational.repository.template.bean;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface BeanAttributeReader {
    Object readAttribute(ResultSet rs, int column, Field sourceField, Object instance) throws SQLException;
}