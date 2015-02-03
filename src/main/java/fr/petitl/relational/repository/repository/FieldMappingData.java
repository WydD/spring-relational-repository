package fr.petitl.relational.repository.repository;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
*
*/
public class FieldMappingData {
    public String columnName;
    public Field field;
    public Method writeMethod;
    public Method readMethod;
    public BeanAttributeWriter attributeWriter;
    public BeanAttributeReader attributeReader;

    public FieldMappingData(String columnName, Field field, Method writeMethod, Method readMethod,
                            BeanAttributeWriter attributeWriter, BeanAttributeReader attributeReader) {
        this.columnName = columnName;
        this.field = field;
        this.writeMethod = writeMethod;
        this.readMethod = readMethod;
        this.attributeWriter = attributeWriter;
        this.attributeReader = attributeReader;
    }
}
