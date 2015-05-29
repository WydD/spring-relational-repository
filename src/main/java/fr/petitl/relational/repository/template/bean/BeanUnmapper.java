package fr.petitl.relational.repository.template.bean;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import fr.petitl.relational.repository.template.StatementMapper;

/**
 *
 */
public class BeanUnmapper<T> implements StatementMapper<T> {

    private List<FieldMappingData> fieldData;

    public BeanUnmapper(List<FieldMappingData> fieldData) {
        this.fieldData = fieldData;
    }

    @Override
    public void prepare(PreparedStatement pse, T instance) throws SQLException {
        int c = 1;
        for (FieldMappingData data : fieldData) {
            Object object = data.readMethod.apply(instance);
            data.attributeWriter.writeAttribute(pse, c++, object, data.field);
        }
    }
}
