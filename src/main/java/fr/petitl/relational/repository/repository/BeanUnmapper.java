package fr.petitl.relational.repository.repository;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

import com.nurkiewicz.jdbcrepository.RowUnmapper;

/**
 *
 */
public class BeanUnmapper<T> implements RowUnmapper<T> {

    private Class<T> clazz;

    public BeanUnmapper(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Map<String, Object> mapColumns(T instance) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        try {

            for (Field f : clazz.getDeclaredFields()) {
                FieldMappingData data = FieldUtil.getData(f);
                if (Modifier.isTransient(f.getModifiers()))
                    continue;

                Object object = data.readMethod.invoke(instance);
                object = data.dbMapping.toDB(object, f);
                result.put(data.columnName, object);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
        return result;
    }
}
