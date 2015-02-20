package fr.petitl.relational.repository.template.bean;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Pattern;

import fr.petitl.relational.repository.annotation.Column;
import fr.petitl.relational.repository.annotation.Table;
import fr.petitl.relational.repository.template.RowMapper;
import org.springframework.beans.BeanUtils;

/**
 *
 */
public class BeanMappingData<T> {

    private final BeanMapper<T> mapper;
    private final BeanUnmapper<T> unmapper;
    private Pattern camelCasePattern = Pattern.compile("([a-z])([A-Z])");

    private Class<T> clazz;

    private Map<Field, FieldMappingData> fields = new HashMap<>();
    private Map<String, FieldMappingData> columns = new HashMap<>();
    private final Table tableAnnotation;

    public BeanMappingData(Class<T> clazz) {
        this.clazz = clazz;

        tableAnnotation = clazz.getDeclaredAnnotation(Table.class);

        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isTransient(field.getModifiers()) ||
                    Modifier.isStatic(field.getModifiers()))
                continue;
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
            FieldMappingData data = new FieldMappingData(colName, field, writeMethod, readMethod, writer, reader);
            fields.put(field, data);
            columns.put(colName, data);
        }

        mapper = new BeanMapper<>(this);
        unmapper = new BeanUnmapper<>(getFieldData());
    }

    public FieldMappingData fieldForColumn(String columnName) {
        FieldMappingData data = columns.get(columnName.toLowerCase());
        if (data == null)
            throw new IllegalStateException("Can not write column name " + columnName + ": can't find matching property");
        return data;
    }

    public List<FieldMappingData> getFieldData() {
        return new ArrayList<>(fields.values());
    }

    public String camelToSnakeCase(String camelCase) {
        return this.camelCasePattern.matcher(camelCase).replaceAll("$1_$2").toLowerCase();
    }

    @SuppressWarnings("unchecked")
    public <S extends T> RowMapper<S> getMapper() {
        return (BeanMapper<S>) mapper;
    }

    @SuppressWarnings("unchecked")
    public <S> BeanUnmapper<S> getInsertUnmapper() {
        return (BeanUnmapper<S>) unmapper;
    }

    public Class<T> getBeanClass() {
        return clazz;
    }

    public Table getTableAnnotation() {
        return tableAnnotation;
    }
}
