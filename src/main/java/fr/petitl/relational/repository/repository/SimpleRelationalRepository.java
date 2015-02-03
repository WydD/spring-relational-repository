package fr.petitl.relational.repository.repository;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import fr.petitl.relational.repository.annotation.Table;
import fr.petitl.relational.repository.support.RelationalEntityInformation;
import fr.petitl.relational.repository.template.RelationalQuery;
import fr.petitl.relational.repository.template.RelationalTemplate;
import fr.petitl.relational.repository.template.RowMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.support.JdbcUtils;

import static java.lang.String.format;

public class SimpleRelationalRepository<T, ID extends Serializable> implements RelationalRepository<T, ID> {

    private final Function<T, ID> idGetter;
    private final BiConsumer<T, Map<String, Object>> idSetter;
    private final String tableName;
    private final String whereIds;
    private final String columnIds;
    private final RelationalEntityInformation<T, ID> entityInformation;

    public static Object[] pk(Object... idValues) {
        return idValues;
    }

    private final BeanMapper<T> rowMapper;
    private final BeanUnmapper<T> rowUnmapper;
    private final boolean generatedPK;

    private RelationalTemplate template;

    public SimpleRelationalRepository(RelationalEntityInformation<T, ID> entityInformation, RelationalTemplate template) {
        this.template = template;
        rowMapper = new BeanMapper<>(entityInformation.getJavaType());
        rowUnmapper = new BeanUnmapper<T>(entityInformation.getJavaType());
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
                        rowMapper.getFromColumnName(entry.getKey()).writeMethod.invoke(instance, entry.getValue());
                    }
                } catch (InvocationTargetException | IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            };
        } else {
            idSetter = null;
        }

        tableName = tableAnnotation.value();
        this.entityInformation = entityInformation;
        whereIds = entityInformation.getPkFields().stream().map(it -> it.columnName + " = ?").collect(Collectors.joining(" AND "));
        columnIds = entityInformation.getPkFields().stream().map(it -> it.columnName).collect(Collectors.joining(", "));
    }

    public <E> RelationalQuery<E> query(String sql, RowMapper<E> mapper) {
        return new RelationalQuery<>(sql, template, mapper);
    }

    @Override
    public long count() {
        return query(format("SELECT count(*) FROM %s", tableName), it -> it.getLong(1)).findOne();
    }

    @Override
    public void delete(ID id) {
        setId(id, query(format("DELETE FROM %s WHERE %s", tableName, whereIds), null)).update();
    }

    @Override
    public void delete(T entity) {
        delete(idGetter.apply(entity));
    }

    @Override
    public void delete(Iterable<? extends T> entities) {
        for (T t : entities) {
            delete(t);
        }
    }

    @Override
    public void deleteAll() {
        query(format("DELETE FROM %s", tableName), null).update();
    }

    @Override
    public boolean exists(ID id) {
        return setId(id, query(format("SELECT count(*) FROM %s WHERE %s", tableName, whereIds), it -> it.getLong(1))).findOne() > 0;
    }

    private <E> RelationalQuery<E> setId(ID id, RelationalQuery<E> query) {
        if (id instanceof Object[]) {
            Object[] objects = (Object[]) id;
            for (int i = 0; i < objects.length; i++) {
                query.setParameter(i + 1, objects[i]);
            }
        } else {
            query.setParameter(1, id);
        }
        return query;
    }

    @Override
    public List<T> findAll() {
        return streamAll().collect(Collectors.toList());
    }

    @Override
    public T findOne(ID id) {
        return setId(id, query(format("SELECT * FROM %s WHERE %s", tableName, whereIds), rowMapper)).findOne();
    }

    @Override
    public <S extends T> S save(S entity) {
        return create(entity);
    }

    public <S extends T> S update(S entity) {
       /* final Map<String, Object> columns = preUpdate(entity, columnsCopy(entity));
        final List<Object> idValues = removeIdColumns(columns);
        final String updateQuery = sqlGenerator.update(table, columns);
        for (int i = 0; i < table.getIdColumns().size(); ++i) {
            columns.put(table.getIdColumns().get(i), idValues.get(i));
        }
        final Object[] queryParams = columns.values().toArray();
        jdbcOperations.update(updateQuery, queryParams);
        return postUpdate(entity);      */
        return null;
    }

    protected <S extends T> S create(S entity) {
        String query = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName,
                rowUnmapper.getColumns().stream().collect(Collectors.joining(", ")),
                rowUnmapper.getColumns().stream().map(it -> "?").collect(Collectors.joining(", ")));
        if (generatedPK) {
            return template.executeInsertGenerated(query, entity, (pse, it) -> rowUnmapper.prepare(pse, (S) it), rowMapper::instanceMapper);
        } else {
            template.executeUpdate(query, entity, (pse, it) -> rowUnmapper.prepare(pse, (S) it));
            return entity;
        }
    }


    @Override
    public <S extends T> Iterable<S> save(Iterable<S> entities) {
        String query = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName,
                rowUnmapper.getColumns().stream().collect(Collectors.joining(", ")),
                rowUnmapper.getColumns().stream().map(it -> "?").collect(Collectors.joining(", ")));
        Stream<S> stream = StreamSupport.stream(entities.spliterator(), false);
        if (generatedPK) {
            return template.mapInsert(query, stream, (pse, it) -> rowUnmapper.prepare(pse, (S) it), rowMapper::instanceMapper)
                    .collect(Collectors.toList());
        } else {
            template.executeBatch(query, stream, (pse, it) -> rowUnmapper.prepare(pse, (S) it));
            return entities;
        }
    }

    @Override
    public Iterable<T> findAll(Iterable<ID> ids) {
        return null;
//        final List<ID> idsList = toList(ids);
//        if (idsList.isEmpty()) {
//            return Collections.emptyList();
//        }
//        final Object[] idColumnValues = flatten(idsList);
//        return jdbcOperations.query(sqlGenerator.selectByIds(table, idsList.size()), rowMapper, idColumnValues);
    }

    @Override
    public List<T> findAll(Sort sort) {
        return null; //jdbcOperations.query(sqlGenerator.selectAll(table, sort), rowMapper);
    }

    @Override
    public Page<T> findAll(Pageable page) {
        return null;
//        String query = sqlGenerator.selectAll(table, page);
//        return new PageImpl<T>(jdbcOperations.query(query, rowMapper), page, count());
    }

    public Stream<T> streamAll() {
        return query(format("SELECT * FROM %s", tableName), rowMapper).stream();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Stream<ID> streamAllIds() {
        return query(format("SELECT * FROM %s", tableName), rs -> {
            if (entityInformation.getPkFields().size() == 1)
                return (ID) JdbcUtils.getResultSetValue(rs, 1, entityInformation.getIdType());
            else {
                Object[] result = new Object[entityInformation.getPkFields().size()];
                for (int i = 0; i < result.length; i++) {
                    result[i] = JdbcUtils.getResultSetValue(rs, i + 1);
                }
                return (ID) result;
            }
        }).stream();
    }
}