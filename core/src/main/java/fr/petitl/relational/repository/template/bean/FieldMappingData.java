package fr.petitl.relational.repository.template.bean;

import java.lang.reflect.Field;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 *
 */
public class FieldMappingData {
    public final String columnName;
    public final Field field;
    public final BiConsumer<Object, Object> writeMethod;
    public final Function<Object, Object> readMethod;
    public final BeanAttributeWriter attributeWriter;
    public final BeanAttributeReader attributeReader;

    public FieldMappingData(String columnName, Field field, BiConsumer<Object, Object> writeMethod, Function<Object, Object> readMethod,
                            BeanAttributeWriter attributeWriter, BeanAttributeReader attributeReader) {
        this.columnName = columnName;
        this.field = field;
        this.writeMethod = writeMethod;
        this.readMethod = readMethod;
        this.attributeWriter = attributeWriter;
        this.attributeReader = attributeReader;
    }
}
