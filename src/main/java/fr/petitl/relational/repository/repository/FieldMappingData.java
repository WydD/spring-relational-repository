package fr.petitl.relational.repository.repository;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import fr.petitl.relational.repository.DBMapping;

/**
*
*/
public class FieldMappingData {
    public String columnName;
    public Field field;
    public Method writeMethod;
    public Method readMethod;
    public DBMapping dbMapping;

    public FieldMappingData(String columnName, Field field, Method writeMethod, Method readMethod, DBMapping dbMapping) {
        this.columnName = columnName;
        this.field = field;
        this.writeMethod = writeMethod;
        this.readMethod = readMethod;
        this.dbMapping = dbMapping;
    }
}
