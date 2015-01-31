package fr.petitl.relational.repository.repository;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;

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

    @Override
    public T mapRow(ResultSet rs, int j) throws SQLException {
        try {
            T instance = clazz.newInstance();
            ResultSetMetaData meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                String name = meta.getColumnName(i);

                FieldMappingData fieldData = getFromColumnName(name);

                Object object = fieldData.dbMapping.fromDB(rs, i, fieldData.field);
                fieldData.writeMethod.invoke(instance, object);
            }

            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }
}
