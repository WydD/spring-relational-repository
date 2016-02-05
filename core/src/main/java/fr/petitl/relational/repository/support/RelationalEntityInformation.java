package fr.petitl.relational.repository.support;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.petitl.relational.repository.annotation.PK;
import fr.petitl.relational.repository.annotation.PKClass;
import fr.petitl.relational.repository.annotation.Table;
import fr.petitl.relational.repository.template.ColumnMapper;
import fr.petitl.relational.repository.template.RelationalTemplate;
import fr.petitl.relational.repository.template.RowMapper;
import fr.petitl.relational.repository.template.bean.BeanMappingData;
import fr.petitl.relational.repository.template.bean.FieldMappingData;
import org.springframework.data.repository.core.support.AbstractEntityInformation;

/**
 *
 */
public class RelationalEntityInformation<T, ID extends Serializable> extends AbstractEntityInformation<T, ID> {

    private final boolean generatedPK;
    private final Function<Object, ID> idGetter;
    private final Class<ID> idType;
    private final List<FieldMappingData> pkFields;
    private final HashSet<FieldMappingData> pkSet;
    private IdSetter<T> idSetter;
    private BeanMappingData<T> mappingData;
    private final Function<T, List<ColumnMapper>> updateUnmapper;
    private final RowMapper<ID> idMapper;
    private final Function<ID, List<ColumnMapper>> idUnmapper;
    private final String tableName;


    public RelationalEntityInformation(BeanMappingData<T> mappingData, RelationalTemplate template) {
        super(mappingData.getBeanClass());

        this.mappingData = mappingData;

        final Table tableAnnotation = mappingData.getBeanClass().getAnnotation(Table.class);
        if (tableAnnotation == null)
            throw new IllegalStateException("Bean class " + mappingData.getBeanClass().getCanonicalName() + " has no @Table annotation");
        tableName = tableAnnotation.value();

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
            //noinspection unchecked
            idType = (Class<ID>) field.field.getType();
            idGetter = instance -> {
                //noinspection unchecked
                return (ID) field.readMethod.apply(instance);
            };
            final FieldMappingData pkField = pkFields.get(0);
            //noinspection unchecked
            idMapper = rs -> (ID) pkField.attributeReader.readAttribute(rs, 1, pkField.field);
            idUnmapper = id -> Collections.singletonList((ps, i) -> pkField.attributeWriter.writeAttribute(ps, i, id, pkField.field));
        } else {
            final PKClass annotation = mappingData.getBeanClass().getAnnotation(PKClass.class);
            if (annotation != null) {
                //noinspection unchecked
                idType = (Class<ID>) annotation.value();
                BeanMappingData<ID> pkMapping = template.getMappingData(idType);
                idMapper = pkMapping.getMapper();
                idUnmapper = id -> pkFields.stream().map(pkField -> {
                    // Make sure type resolution is alright
                    return (ColumnMapper) (ps, i) -> {
                        FieldMappingData mapping = pkMapping.fieldForColumn(pkField.columnName);
                        mapping.attributeWriter.writeAttribute(ps, i, mapping.readMethod.apply(id), mapping.field);
                    };
                }).collect(Collectors.toList());
                idGetter = instance -> {
                    try {
                        //noinspection unchecked
                        final ID o = idType.getConstructor().newInstance();
                        for (FieldMappingData pkField : pkFields) {
                            final FieldMappingData field = pkMapping.fieldForColumn(pkField.columnName);
                            field.writeMethod.accept(o, pkField.readMethod.apply(instance));
                        }
                        return o;
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException(e);
                    }
                };
            } else {
                //noinspection unchecked
                idType = (Class<ID>) Object[].class;
                idGetter = instance -> {
                    //noinspection unchecked
                    Object[] objects = new Object[pkFields.size()];
                    for (int i = 0; i < objects.length; i++) {
                        objects[i] = pkFields.get(i).readMethod.apply(instance);
                    }
                    //noinspection unchecked
                    return (ID) objects;
                };
                idMapper = rs -> {
                    Object[] result = new Object[pkFields.size()];
                    for (int i = 0; i < result.length; i++) {
                        FieldMappingData fieldData = pkFields.get(i);
                        result[i] = fieldData.attributeReader.readAttribute(rs, i + 1, fieldData.field);
                    }
                    //noinspection unchecked
                    return (ID) result;
                };
                idUnmapper = getObjectArrayUnmapper(pkFields);
            }
        }
        if (generatedPK) {
            final FieldMappingData finalGenerated = generated;
            idSetter = (instance, rs) -> {
                Object id = finalGenerated.attributeReader.readAttribute(rs, 1, finalGenerated.field);
                finalGenerated.writeMethod.accept(instance, id);
            };
        }

        List<FieldMappingData> fieldsForUpdate = Stream.concat(
                mappingData.getFieldData().stream().filter(it -> !pkSet.contains(it)),
                pkFields.stream()
        ).collect(Collectors.toList());

        updateUnmapper = instance -> fieldsForUpdate.stream().map(field -> (ColumnMapper) (ps, i) -> {
            Object object = field.readMethod.apply(instance);
            field.attributeWriter.writeAttribute(ps, i, object, field.field);
        }).collect(Collectors.toList());
    }

    public static <ID> Function<ID, List<ColumnMapper>> getObjectArrayUnmapper(List<FieldMappingData> pkFields) {
        return id -> {
            Object[] objects = (Object[]) id;
            List<ColumnMapper> mappers = new LinkedList<>();
            for (int i = 0; i < pkFields.size(); i++) {
                FieldMappingData fieldData = pkFields.get(i);
                Object value = objects[i];
                mappers.add((ps, idx) -> fieldData.attributeWriter.writeAttribute(ps, idx, value, fieldData.field));
            }
            return mappers;
        };
    }

    public List<FieldMappingData> getPkFields() {
        return pkFields;
    }

    @Override
    public ID getId(Object o) {
        return idGetter.apply(o);
    }

    @Override
    public Class<ID> getIdType() {
        return idType;
    }

    public <S extends T> RowMapper<S> setId(S entity) {
        return rs -> {
            idSetter.setId(entity, rs);
            return entity;
        };
    }

    public boolean isGeneratedPK() {
        return generatedPK;
    }

    public BeanMappingData<T> getMappingData() {
        return mappingData;
    }

    public RowMapper<ID> getIdMapper() {
        return idMapper;
    }

    public Function<ID, List<ColumnMapper>> getIdUnmapper() {
        return idUnmapper;
    }

    public Function<T, List<ColumnMapper>> getUpdateUnmapper() {
        return updateUnmapper;
    }

    public String getTableName() {
        return tableName;
    }

    private interface IdSetter<T> {
        void setId(T instance, ResultSet rs) throws SQLException;
    }
}
