package fr.petitl.relational.repository.template.bean;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 *
 */
public class FieldMappingData {
    public final String columnName;
    public final Field field;
    public final Method writeMethod;
    public final Method readMethod;
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
