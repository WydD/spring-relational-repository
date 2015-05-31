package fr.petitl.relational.repository.template.bean;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import fr.petitl.relational.repository.template.RowMapper;

/**
 *
 */
public class BeanMapper<T> implements RowMapper<T> {

    private BeanMappingData<T> mappingData;

    public BeanMapper(BeanMappingData<T> mappingData) {
        this.mappingData = mappingData;
    }

    public <S extends T> RowMapper<S> instanceMapper(S instance) {
        return rs -> mapToInstance(rs, instance);
    }

    @Override
    public T mapRow(ResultSet rs) throws SQLException {
        try {
            T instance = mappingData.getBeanClass().newInstance();
            return mapToInstance(rs, instance);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private <S extends T> S mapToInstance(ResultSet rs, S instance) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            String name = meta.getColumnName(i);

            // Ignore column names with double "_" prefix (internal)
            if (name.startsWith("__")) {
                continue;
            }

            FieldMappingData fieldData = mappingData.fieldForColumn(name);

            Object object = fieldData.attributeReader.readAttribute(rs, i, fieldData.field);
            fieldData.writeMethod.accept(instance, object);
        }

        return instance;
    }
}
