package fr.petitl.relational.repository.template.bean;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
    private Pattern camelCasePattern = Pattern.compile("([a-z])([A-Z])");

    private Class<T> clazz;

    private Map<Field, FieldMappingData> fields = new HashMap<>();
    private Map<String, FieldMappingData> columns = new HashMap<>();
    private final Table tableAnnotation;

    public BeanMappingData(Class<T> clazz, BeanDialect dialect, RelationalTemplate template) {
        this.clazz = clazz;

        tableAnnotation = clazz.getDeclaredAnnotation(Table.class);

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
                    // Create mapper instances, dont do it if it's VoidBeanMapper which is only a place holder
                    Class<? extends BeanAttributeReader> readerClass = annotation.reader();
                    if (!readerClass.equals(VoidBeanAttributeMapper.class))
                        reader = readerClass.newInstance();

                    Class<? extends BeanAttributeWriter> writerClass = annotation.writer();
                    if (!writerClass.equals(VoidBeanAttributeMapper.class))
                        writer = writerClass.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
            final Class fkId;
            final Class fkType;
            if (FK.class.isAssignableFrom(field.getType())) {
                ParameterizedType genericType = (ParameterizedType) field.getGenericType();
                Type[] fkParam = genericType.getActualTypeArguments();
                //noinspection unchecked
                fkId = (Class<?>) fkParam[0];
                fkType = (Class<?>) fkParam[1];
            } else {
                fkId = null;
                fkType = null;
            }
            if (colName == null || colName.isEmpty()) {
                colName = generateDefaultColumnName(field, fkId != null);
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
                    //noinspection unchecked
                    return repository.fid((Serializable) finalReader.readAttribute(rs, column, sourceField));
                };
                final BeanAttributeWriter finalWriter = writer;
                writer = (ps, column, o, sourceField) -> {
                    FK fk = (FK) o;
                    finalWriter.writeAttribute(ps, column, fk.getId(), sourceField);
                };
            }

            PropertyDescriptor f = BeanUtils.getPropertyDescriptor(field.getDeclaringClass(), field.getName());
            if (f == null)
                throw new IllegalStateException("Can not manage property " + field.getName() + ": can't find property descriptor");
            Method writeMethod = f.getWriteMethod();
            if (writeMethod == null)
                throw new IllegalStateException("Can not manage property " + field.getName() + ": can't find write method");
            Method readMethod = f.getReadMethod();
            if (readMethod == null)
                throw new IllegalStateException("Can not manage property " + field.getName() + ": can't find read method");
            FieldMappingData data = new FieldMappingData(colName, field, writeMethod, readMethod, writer, reader);
            fields.put(field, data);
            columns.put(colName, data);
        }

        mapper = new BeanMapper<>(this);
        unmapper = new BeanUnmapper<>(getFieldData());
    }

    protected String generateDefaultColumnName(Field field, boolean hasFK) {
        String colName;
        colName = camelToSnakeCase(field.getName());
        if (hasFK && !colName.endsWith("_id")) {
            colName += "_id";
        }
        return colName;
    }

    public FieldMappingData fieldForColumn(String columnName) {
        FieldMappingData data = columns.get(columnName.toLowerCase());
        if (data == null)
            throw new IllegalStateException("Can not write column name " + columnName + ": can't find matching property");
        return data;
    }

    public List<FieldMappingData> getFieldData() {
        return new ArrayList<>(fields.values());
    }

    public String camelToSnakeCase(String camelCase) {
        return this.camelCasePattern.matcher(camelCase).replaceAll("$1_$2").toLowerCase();
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

    public Table getTableAnnotation() {
        return tableAnnotation;
    }
}
