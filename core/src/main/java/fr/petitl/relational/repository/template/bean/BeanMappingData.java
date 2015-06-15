package fr.petitl.relational.repository.template.bean;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import fr.petitl.relational.repository.annotation.Column;
import fr.petitl.relational.repository.annotation.Table;
import fr.petitl.relational.repository.dialect.BeanDialect;
import fr.petitl.relational.repository.repository.FK;
import fr.petitl.relational.repository.repository.RelationalRepository;
import fr.petitl.relational.repository.template.RelationalTemplate;
import fr.petitl.relational.repository.template.RowMapper;
import org.springframework.beans.BeanUtils;

/**
 *
 */
public class BeanMappingData<T> {

    private final BeanMapper<T> mapper;
    private final BeanUnmapper<T> unmapper;
    private final List<FieldMappingData> fieldData;

    private Class<T> clazz;

    private Map<String, FieldMappingData> columns = new HashMap<>();

    public BeanMappingData(Class<T> clazz, BeanDialect dialect, RelationalTemplate template) {
        this.clazz = clazz;

        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isTransient(field.getModifiers()) ||
                    Modifier.isStatic(field.getModifiers()))
                continue;
            String colName = null;
            BeanAttributeReader reader = null;
            BeanAttributeWriter writer = null;
            Column annotation = field.getDeclaredAnnotation(Column.class);
            if (annotation != null) {
                colName = annotation.name();
                try {
                    Class<? extends BeanAttributeMapper> mapperClass = annotation.mapper();
                    if (!mapperClass.equals(VoidBeanAttributeMapper.class)) {
                        BeanAttributeMapper mapper = mapperClass.newInstance();
                        reader = mapper;
                        writer = mapper;
                    } else {
                        // Create mapper instances, dont do it if it's VoidBeanMapper which is only a place holder
                        Class<? extends BeanAttributeReader> readerClass = annotation.reader();
                        if (!readerClass.equals(VoidBeanAttributeMapper.class))
                            reader = readerClass.newInstance();

                        Class<? extends BeanAttributeWriter> writerClass = annotation.writer();
                        if (!writerClass.equals(VoidBeanAttributeMapper.class))
                            writer = writerClass.newInstance();
                    }
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
            final Class fkId;
            final Class fkType;
            // Foreign key resolution
            if (FK.class.isAssignableFrom(field.getType())) {
                ParameterizedType genericType = (ParameterizedType) field.getGenericType();
                Type[] fkParam = genericType.getActualTypeArguments();
                // Fetch types in generic declaration
                //noinspection unchecked
                fkId = (Class<?>) fkParam[0];
                fkType = (Class<?>) fkParam[1];
            } else {
                fkId = null;
                fkType = null;
            }
            if (colName == null || colName.isEmpty()) {
                colName = template.getNamingConvention().generateDefaultColumnName(field, fkId != null);
            }
            if (reader == null)
                reader = dialect.defaultReader();
            if (writer == null)
                writer = dialect.defaultWriter();

            // Wrap the foreign key resolution if any
            if (fkId != null) {
                final BeanAttributeReader finalReader = reader;
                reader = (rs, column, sourceField) -> {
                    RelationalRepository repository = template.getRepositoryForType(fkType, fkId);

                    Serializable id = (Serializable) finalReader.readAttribute(rs, column, sourceField);
                    //noinspection unchecked
                    return id != null ? repository.fid(id) : null;
                };
                final BeanAttributeWriter finalWriter = writer;
                writer = (ps, column, o, sourceField) -> {
                    FK fk = (FK) o;
                    finalWriter.writeAttribute(ps, column, o != null ? fk.getId() : null, sourceField);
                };
            }

            FieldMappingData data;
            PropertyDescriptor f = BeanUtils.getPropertyDescriptor(field.getDeclaringClass(), field.getName());
            if (f != null) {
                Method writeMethod = f.getWriteMethod();
                if (writeMethod == null)
                    throw new IllegalStateException("Can not manage property " + field.getName() + ": can't find write method");
                Method readMethod = f.getReadMethod();
                if (readMethod == null)
                    throw new IllegalStateException("Can not manage property " + field.getName() + ": can't find read method");

                data = new FieldMappingData(colName, field, (obj, value) -> {
                    try {
                        writeMethod.invoke(obj, value);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new IllegalStateException(e);
                    }
                }, obj -> {
                    try {
                        return readMethod.invoke(obj);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new IllegalStateException(e);
                    }
                }, writer, reader);
            } else {
                if (!Modifier.isPublic(field.getModifiers())) {
                    try {
                        field.setAccessible(true);
                    } catch (SecurityException se) {
                        throw new IllegalStateException("Can not manage property " + field.getName() + ": can't find property descriptor, field is not public and security refuses override", se);
                    }
                }
                BiConsumer<Object, Object> setter = (obj, value) -> {
                    try {
                        field.set(obj, value);
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                };
                Function<Object, Object> getter = obj -> {
                    try {
                        return field.get(obj);
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                };
                data = new FieldMappingData(colName, field, setter, getter, writer, reader);
            }
            columns.put(colName, data);
        }

        mapper = new BeanMapper<>(this);
        fieldData = new ArrayList<>(columns.values());
        unmapper = new BeanUnmapper<>(fieldData);
    }

    public FieldMappingData fieldForColumn(String columnName) {
        FieldMappingData data = columns.get(columnName.toLowerCase());
        if (data == null)
            throw new IllegalStateException("Can not write column name " + columnName + ": can't find matching property");
        return data;
    }

    public List<FieldMappingData> getFieldData() {
        return fieldData;
    }

    @SuppressWarnings("unchecked")
    public <S extends T> RowMapper<S> getMapper() {
        return (BeanMapper<S>) mapper;
    }

    @SuppressWarnings("unchecked")
    public <S> BeanUnmapper<S> getInsertUnmapper() {
        return (BeanUnmapper<S>) unmapper;
    }

    public Class<T> getBeanClass() {
        return clazz;
    }
}
