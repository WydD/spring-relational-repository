package fr.petitl.relational.repository.repository;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import fr.petitl.relational.repository.DBMapping;
import fr.petitl.relational.repository.annotation.Column;
import org.springframework.beans.BeanUtils;

/**
 *
 */
public class FieldUtil {
    private static Map<Field, FieldMappingData> cache = new HashMap<>();
    public synchronized static FieldMappingData getData(Field field) {
        FieldMappingData data = cache.get(field);
        if(data != null) {
            return data;
        }

        String colName = null;
        BeanAttributeReader reader;
        BeanAttributeWriter writer;
        Column annotation = field.getDeclaredAnnotation(Column.class);
        if (annotation == null) {
            reader = BeanAttributeReader.Default.INSTANCE;
            writer = BeanAttributeWriter.Default.INSTANCE;
        } else {
            colName = annotation.name();
            try {
                reader = annotation.reader().newInstance();
                writer = annotation.writer().newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        if (colName == null || colName.isEmpty())
            colName = camelToSnakeCase(field.getName());

        PropertyDescriptor f = BeanUtils.getPropertyDescriptor(field.getDeclaringClass(), field.getName());
        if (f == null)
            throw new IllegalStateException("Can not manage property " + field.getName() + ": can't find property descriptor");
        Method writeMethod = f.getWriteMethod();
        if (writeMethod == null)
            throw new IllegalStateException("Can not manage property " + field.getName() + ": can't find write method");
        Method readMethod = f.getReadMethod();
        if (readMethod == null)
            throw new IllegalStateException("Can not manage property " + field.getName() + ": can't find read method");
        data = new FieldMappingData(colName, field, writeMethod, readMethod, writer, reader);
        cache.put(field, data);
        return data;
    }

    public static String camelToSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }


}
