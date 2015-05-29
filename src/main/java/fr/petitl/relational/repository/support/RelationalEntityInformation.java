package fr.petitl.relational.repository.support;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.*;
import java.util.function.Function;

import fr.petitl.relational.repository.annotation.PK;
import fr.petitl.relational.repository.template.RowMapper;
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
    private Function<T, RowMapper<T>> idSetter;
    private BeanMappingData<T> mappingData;
    private final BeanUnmapper<T> updateUnmapper;


    public RelationalEntityInformation(BeanMappingData<T> mappingData) {
        super(mappingData.getBeanClass());

        this.mappingData = mappingData;

        if (mappingData.getTableAnnotation() == null)
            throw new IllegalStateException("Bean class " + mappingData.getBeanClass().getCanonicalName() + " has no @Table annotation");

        // Get the sorted
        TreeMap<Integer, FieldMappingData> pks = new TreeMap<>();
        FieldMappingData generated = null;
        for (FieldMappingData fieldData : mappingData.getFieldData()) {
            PK declaredAnnotation = fieldData.field.getDeclaredAnnotation(PK.class);
            if (declaredAnnotation != null) {
                if (declaredAnnotation.generated()) {
                    if (generated != null) {
                        throw new UnsupportedOperationException("Two generated columns are not supported yet in class " + getJavaType().getName());
                    }
                    generated = fieldData;
                }

                FieldMappingData old = pks.put(declaredAnnotation.order(), fieldData);
                if (old != null) {
                    throw new IllegalStateException("Two primary keys with the same order in class " + getJavaType().getName());
                }
            }
        }
        if (pks.isEmpty()) {
            throw new IllegalStateException("No primary keys specified in class " + getJavaType().getName());
        }
        this.generatedPK = generated != null;

        pkSet = new HashSet<>(pks.values());
        pkFields = new ArrayList<>(pks.values());
        if (pkFields.size() == 1) {
            FieldMappingData field = pkFields.get(0);
            idType = field.field.getType();
            idGetter = instance -> {
                //noinspection unchecked
                return (ID) field.readMethod.apply(instance);
            };
        } else {
            idType = Object[].class;
            idGetter = instance -> {
                //noinspection unchecked
                Object[] objects = new Object[pkFields.size()];
                for (int i = 0; i < objects.length; i++) {
                    objects[i] = pkFields.get(i).readMethod.apply(instance);
                }
                //noinspection unchecked
                return (ID) objects;
            };
        }
        if (generatedPK) {
            final FieldMappingData finalGenerated = generated;
            idSetter = (T instance) -> (ResultSet rs) -> {
                Object id = finalGenerated.attributeReader.readAttribute(rs, 1, finalGenerated.field);
                finalGenerated.writeMethod.accept(instance, id);
                return instance;
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

    @SuppressWarnings("unchecked")
    public <S extends T> RowMapper<S> setId(S entity) {
        return (RowMapper<S>) idSetter.apply(entity);
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
