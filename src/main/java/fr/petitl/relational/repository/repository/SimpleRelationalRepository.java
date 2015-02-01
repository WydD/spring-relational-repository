package fr.petitl.relational.repository.repository;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import fr.petitl.relational.repository.annotation.Table;
import fr.petitl.relational.repository.support.RelationalEntityInformation;
import com.nurkiewicz.jdbcrepository.RowUnmapper;
import com.nurkiewicz.jdbcrepository.TableDescription;
import com.nurkiewicz.jdbcrepository.sql.SqlGenerator;
import fr.petitl.relational.repository.template.RelationalTemplateBak;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;

public class SimpleRelationalRepository<T, ID extends Serializable> implements RelationalRepository<T, ID> {

    private final Function<T, ID> idGetter;
    private final BiConsumer<T, Map<String, Object>> idSetter;

    public static Object[] pk(Object... idValues) {
        return idValues;
    }

    private final TableDescription table;

    private final RowMapper<T> rowMapper;
    private final RowUnmapper<T> rowUnmapper;
    private final boolean generatedPK;

    private SqlGenerator sqlGenerator = new SqlGenerator();
    private RelationalTemplateBak jdbcOperations;

    public SimpleRelationalRepository(RelationalEntityInformation<T, ID> entityInformation, RelationalTemplateBak jdbcOperations) {
        this.jdbcOperations = jdbcOperations;
        BeanMapper<T> beanMapper = new BeanMapper<>(entityInformation.getJavaType());
        rowMapper = beanMapper;
        rowUnmapper = new BeanUnmapper<>(entityInformation.getJavaType());
        Table tableAnnotation = entityInformation.getJavaType().getDeclaredAnnotation(Table.class);
        if (tableAnnotation == null) {
            throw new IllegalArgumentException("Given class is not a @Table");
        }
        idGetter = entityInformation::getId;
        generatedPK = entityInformation.isGeneratedPK();
        if (generatedPK) {
            idSetter = (instance, keys) -> {
                try {
                    for (Map.Entry<String, Object> entry : keys.entrySet()) {
                        beanMapper.getFromColumnName(entry.getKey()).writeMethod.invoke(instance, entry.getValue());
                    }
                } catch (InvocationTargetException | IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            };
        } else {
            idSetter = null;
        }

        table = new TableDescription(tableAnnotation.value(), null, FieldUtil.getColumnNames(entityInformation.getPkFields()));
    }

    protected TableDescription getTable() {
        return table;
    }

    @Override
    public long count() {
        return jdbcOperations.queryForObject(sqlGenerator.count(table), Long.class);
    }

    @Override
    public void delete(ID id) {
        jdbcOperations.update(sqlGenerator.deleteById(table), idToObjectArray(id));
    }

    @Override
    public void delete(T entity) {
        jdbcOperations.update(sqlGenerator.deleteById(table), idGetter.apply(entity));
    }

    @Override
    public void delete(Iterable<? extends T> entities) {
        for (T t : entities) {
            delete(t);
        }
    }

    @Override
    public void deleteAll() {
        jdbcOperations.update(sqlGenerator.deleteAll(table));
    }

    @Override
    public boolean exists(ID id) {
        return jdbcOperations.queryForObject(sqlGenerator.countById(table), Integer.class, idToObjectArray(id)) > 0;
    }

    @Override
    public List<T> findAll() {
        return jdbcOperations.query(sqlGenerator.selectAll(table), rowMapper);
    }

    @Override
    public T findOne(ID id) {
        final Object[] idColumns = idToObjectArray(id);
        final List<T> entityOrEmpty = jdbcOperations.query(sqlGenerator.selectById(table), idColumns, rowMapper);
        return entityOrEmpty.isEmpty() ? null : entityOrEmpty.get(0);
    }

    private static <ID> Object[] idToObjectArray(ID id) {
        if (id instanceof Object[])
            return (Object[]) id;
        else
            return new Object[]{id};
    }

    private static <ID> List<Object> idToObjectList(ID id) {
        if (id instanceof Object[])
            return Arrays.asList((Object[]) id);
        else
            return Collections.<Object>singletonList(id);
    }

    @Override
    public <S extends T> S save(S entity) {
        return create(entity);
    }

    public <S extends T> S update(S entity) {
        final Map<String, Object> columns = preUpdate(entity, columnsCopy(entity));
        final List<Object> idValues = removeIdColumns(columns);
        final String updateQuery = sqlGenerator.update(table, columns);
        for (int i = 0; i < table.getIdColumns().size(); ++i) {
            columns.put(table.getIdColumns().get(i), idValues.get(i));
        }
        final Object[] queryParams = columns.values().toArray();
        jdbcOperations.update(updateQuery, queryParams);
        return postUpdate(entity);
    }

    protected Map<String, Object> preUpdate(T entity, Map<String, Object> columns) {
        return columns;
    }

    protected <S extends T> S create(S entity) {
        final Map<String, Object> columns = preCreate(columnsCopy(entity), entity);
        if (generatedPK) {
            return createWithAutoGeneratedKey(entity, columns);
        } else {
            return createWithManuallyAssignedKey(entity, columns);
        }
    }

    private <S extends T> S createWithManuallyAssignedKey(S entity, Map<String, Object> columns) {
        final String createQuery = sqlGenerator.create(table, columns);
        final Object[] queryParams = columns.values().toArray();
        jdbcOperations.update(createQuery, queryParams);
        return postCreate(entity, null);
    }

    private <S extends T> S createWithAutoGeneratedKey(S entity, Map<String, Object> columns) {
        removeIdColumns(columns);
        final String createQuery = sqlGenerator.create(table, columns);
        final Object[] queryParams = columns.values().toArray();
        final GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbcOperations.update(con -> {
            final String idColumnName = table.getIdColumns().get(0);
            final PreparedStatement ps = con.prepareStatement(createQuery, new String[]{idColumnName});
            for (int i = 0; i < queryParams.length; ++i) {
                ps.setObject(i + 1, queryParams[i]);
            }
            return ps;
        }, key);
        return postCreate(entity, key.getKeys());
    }

    private List<Object> removeIdColumns(Map<String, Object> columns) {
        List<Object> idColumnsValues = new ArrayList<Object>(columns.size());
        for (String idColumn : table.getIdColumns()) {
            idColumnsValues.add(columns.remove(idColumn));
        }
        return idColumnsValues;
    }

    protected Map<String, Object> preCreate(Map<String, Object> columns, T entity) {
        return columns;
    }

    private LinkedHashMap<String, Object> columnsCopy(T entity) {
        return new LinkedHashMap<>(rowUnmapper.mapColumns(entity));
    }

    protected <S extends T> S postUpdate(S entity) {
        return entity;
    }

    /**
     * General purpose hook method that is called every time {@link #create} is called with a new entity.
     * <p>
     * OVerride this method e.g. if you want to fetch auto-generated key from database
     *
     * @param entity      Entity that was passed to {@link #create}
     * @param generatedIds ID generated during INSERT or NULL if not available/not generated.
     *                    todo: Type should be ID, not Number
     * @return Either the same object as an argument or completely different one
     */
    protected <S extends T> S postCreate(S entity, Map<String, Object> generatedIds) {
        if(idSetter != null) {
            idSetter.accept(entity, generatedIds);
        }
        return entity;
    }

    @Override
    public <S extends T> Iterable<S> save(Iterable<S> entities) {
        List<S> ret = new ArrayList<S>();
        for (S s : entities) {
            ret.add(save(s));
        }
        return ret;
    }

    @Override
    public Iterable<T> findAll(Iterable<ID> ids) {
        final List<ID> idsList = toList(ids);
        if (idsList.isEmpty()) {
            return Collections.emptyList();
        }
        final Object[] idColumnValues = flatten(idsList);
        return jdbcOperations.query(sqlGenerator.selectByIds(table, idsList.size()), rowMapper, idColumnValues);
    }

    private static <T> List<T> toList(Iterable<T> iterable) {
        final List<T> result = new ArrayList<T>();
        for (T item : iterable) {
            result.add(item);
        }
        return result;
    }

    private static <ID> Object[] flatten(List<ID> ids) {
        final List<Object> result = new ArrayList<Object>();
        for (ID id : ids) {
            result.addAll(idToObjectList(id));
        }
        return result.toArray();
    }

    @Override
    public List<T> findAll(Sort sort) {
        return jdbcOperations.query(sqlGenerator.selectAll(table, sort), rowMapper);
    }

    @Override
    public Page<T> findAll(Pageable page) {
        String query = sqlGenerator.selectAll(table, page);
        return new PageImpl<T>(jdbcOperations.query(query, rowMapper), page, count());
    }

    public Stream<T> streamAll() {
        return jdbcOperations.stream(sqlGenerator.selectAll(table)).map(it -> {
            try {
                return rowMapper.mapRow(it, 0);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public Stream<ID> streamAllIds() {
        return jdbcOperations.stream("SELECT "+String.join(", ", table.getIdColumns()) + " FROM "+table.getName()).map(it -> {
            try {
                if (table.getIdColumns().size() == 1)
                    return (ID) it.getObject(1);
                else {
                    Object[] result = new Object[table.getIdColumns().size()];
                    for (int i = 0; i < result.length; i++) {
                        result[i] = it.getObject(i+1);
                    }
                    return (ID) result;
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }
}