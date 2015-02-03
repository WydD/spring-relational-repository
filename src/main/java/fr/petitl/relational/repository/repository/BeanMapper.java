package fr.petitl.relational.repository.repository;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import fr.petitl.relational.repository.template.RowMapper;

/**
 *
 */
public class BeanMapper<T> implements RowMapper<T> {

    private Class<T> clazz;

    private Map<String, FieldMappingData> fieldMap = new HashMap<>();

    public BeanMapper(Class<T> clazz) {
        this.clazz = clazz;
        for (Field field : clazz.getDeclaredFields()) {
            FieldMappingData data = FieldUtil.getData(field);
            fieldMap.put(data.columnName, data);
        }
    }

    public FieldMappingData getFromColumnName(String name) {
        FieldMappingData fieldData = fieldMap.get(name.toLowerCase());
        if (fieldData == null)
            throw new IllegalStateException("Can not write column name " + name + ": can't find matching property");
        return fieldData;
    }

    public <S extends T> RowMapper<S> instanceMapper(S instance) {
        return rs -> mapToInstance(rs, instance);
    }

    @Override
    public T mapRow(ResultSet rs) throws SQLException {
        try {
            T instance = clazz.newInstance();
            return mapToInstance(rs, instance);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private <S extends T> S mapToInstance(ResultSet rs, S instance) throws SQLException {
        try {
            ResultSetMetaData meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                String name = meta.getColumnName(i);

                FieldMappingData fieldData = getFromColumnName(name);

                Object object = fieldData.attributeReader.readAttribute(rs, i, fieldData.field);
                fieldData.writeMethod.invoke(instance, object);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }

        return instance;
    }
}
