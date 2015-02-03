package fr.petitl.relational.repository.repository;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import fr.petitl.relational.repository.template.StatementMapper;

/**
 *
 */
public class BeanUnmapper<T> implements StatementMapper<T> {

    private List<FieldMappingData> fields = new LinkedList<>();
    private List<String> columns;

    public BeanUnmapper(Class<T> clazz) {
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field f : declaredFields) {
            FieldMappingData data = FieldUtil.getData(f);
            if (Modifier.isTransient(f.getModifiers()))
                continue;

            fields.add(data);
        }
        columns = fields.stream().map(it -> it.columnName).collect(Collectors.toList());
    }

    public List<String> getColumns() {
        return columns;
    }

    @Override
    public void prepare(PreparedStatement pse, T instance) throws SQLException {
        try {
            int c = 1;
            for (FieldMappingData data : fields) {
                Object object = data.readMethod.invoke(instance);
                data.attributeWriter.writeAttribute(pse, c++, object, data.field);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }
}
