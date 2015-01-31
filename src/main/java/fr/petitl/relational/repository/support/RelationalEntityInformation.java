package fr.petitl.relational.repository.support;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Function;

import fr.petitl.relational.repository.annotation.PK;
import fr.petitl.relational.repository.repository.FieldMappingData;
import fr.petitl.relational.repository.repository.FieldUtil;
import org.springframework.data.repository.core.support.AbstractEntityInformation;

/**
 *
 */
public class RelationalEntityInformation<T, ID extends Serializable> extends AbstractEntityInformation<T, ID> {

    private final boolean generatedPK;
    private final Function<Object, ID> idGetter;
    private final Class<?> idType;
    private final List<FieldMappingData> pkFields;

    public RelationalEntityInformation(Class<T> clazz) {
        super(clazz);

        // Get the sorted
        TreeMap<Integer, FieldMappingData> pks = new TreeMap<>();
        boolean generated = false;
        for (Field field : clazz.getDeclaredFields()) {
            PK declaredAnnotation = field.getDeclaredAnnotation(PK.class);
            if (declaredAnnotation != null) {
                generated = declaredAnnotation.generated();
                FieldMappingData old = pks.put(declaredAnnotation.order(), FieldUtil.getData(field));
                if (old != null) {
                    throw new IllegalStateException("Two primary keys with the same order");
                }
            }
        }
        if (pks.isEmpty()) {
            throw new IllegalStateException("No primary keys specified");
        }
        this.generatedPK = generated;

        pkFields = new ArrayList<>(pks.values());
        if (pkFields.size() == 1) {
            FieldMappingData field = pkFields.get(0);
            idType = field.readMethod.getReturnType();
            idGetter = instance -> {
                try {
                    //noinspection unchecked
                    return (ID) field.readMethod.invoke(instance);
                } catch (InvocationTargetException | IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            };
        } else {
            idType = Object[].class;
            idGetter = instance -> {
                try {
                    //noinspection unchecked
                    Object[] objects = new Object[pkFields.size()];
                    for (int i = 0; i < objects.length; i++) {
                        Method field = pkFields.get(i).readMethod;
                        objects[i] = field.invoke(instance);
                    }
                    //noinspection unchecked
                    return (ID) objects;
                } catch (InvocationTargetException | IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            };
        }
    }

    public List<FieldMappingData> getPkFields() {
        return pkFields;
    }

    @Override
    public ID getId(Object o) {
        return idGetter.apply(o);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<ID> getIdType() {
        return (Class<ID>) idType;
    }

    public boolean isGeneratedPK() {
        return generatedPK;
    }
}
