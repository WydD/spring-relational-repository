package fr.petitl.relational.repository.support;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;

import fr.petitl.relational.repository.annotation.PK;
import fr.petitl.relational.repository.template.bean.BeanMappingData;
import fr.petitl.relational.repository.template.bean.BeanUnmapper;
import fr.petitl.relational.repository.template.bean.FieldMappingData;
import org.springframework.data.repository.core.support.AbstractEntityInformation;

/**
 *
 */
public class RelationalEntityInformation<T, ID extends Serializable> extends AbstractEntityInformation<T, ID> {

    private final boolean generatedPK;
    private final Function<Object, ID> idGetter;
    private final Class<?> idType;
    private final List<FieldMappingData> pkFields;
    private final HashSet<FieldMappingData> pkSet;
    private BeanMappingData<T> mappingData;
    private final BeanUnmapper<T> updateUnmapper;


    public RelationalEntityInformation(BeanMappingData<T> mappingData) {
        super(mappingData.getBeanClass());

        this.mappingData = mappingData;

        if (mappingData.getTableAnnotation() == null)
            throw new IllegalStateException("Bean class "+ mappingData.getBeanClass().getCanonicalName()+" has no @Table annotation");

        // Get the sorted
        TreeMap<Integer, FieldMappingData> pks = new TreeMap<>();
        boolean generated = false;
        for (FieldMappingData fieldData : mappingData.getFieldData()) {
            PK declaredAnnotation = fieldData.field.getDeclaredAnnotation(PK.class);
            if (declaredAnnotation != null) {
                generated = declaredAnnotation.generated();
                FieldMappingData old = pks.put(declaredAnnotation.order(), fieldData);
                if (old != null) {
                    throw new IllegalStateException("Two primary keys with the same order");
                }
            }
        }
        if (pks.isEmpty()) {
            throw new IllegalStateException("No primary keys specified");
        }
        this.generatedPK = generated;

        pkSet = new HashSet<>(pks.values());
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

        LinkedList<FieldMappingData> list = new LinkedList<>();
        mappingData.getFieldData().stream().filter(it -> !pkSet.contains(it)).forEach(list::add);
        list.addAll(pkFields);

        updateUnmapper = new BeanUnmapper<>(list);
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

    public BeanMappingData<T> getMappingData() {
        return mappingData;
    }

    @SuppressWarnings("unchecked")
    public <S extends T> BeanUnmapper<S> getUpdateUnmapper() {
        return (BeanUnmapper<S>) updateUnmapper;
    }
}
