package fr.petitl.relational.repository.repository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import fr.petitl.relational.repository.repository.sql.BeanSQLGeneration;
import fr.petitl.relational.repository.support.RelationalEntityInformation;
import fr.petitl.relational.repository.template.RelationalQuery;
import fr.petitl.relational.repository.template.RelationalTemplate;
import fr.petitl.relational.repository.template.RowMapper;
import fr.petitl.relational.repository.template.bean.BeanMappingData;
import fr.petitl.relational.repository.template.bean.FieldMappingData;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class SimpleRelationalRepository<T, ID extends Serializable> implements RelationalRepository<T, ID> {
    private static final RowMapper<Long> COUNT_MAPPER = it -> it.getLong(1);

    private final Function<T, ID> idGetter;
    private final RelationalEntityInformation<T, ID> entityInformation;
    private final BeanSQLGeneration<T, ID> sql;
    private final BeanMappingData<T> mappingData;

    private final boolean generatedPK;

    private RelationalTemplate template;

    public SimpleRelationalRepository(RelationalEntityInformation<T, ID> entityInformation, RelationalTemplate template) {
        this.template = template;
        sql = new BeanSQLGeneration<>(entityInformation);

        mappingData = entityInformation.getMappingData();

        idGetter = entityInformation::getId;
        generatedPK = entityInformation.isGeneratedPK();

        this.entityInformation = entityInformation;
    }

    public <E> RelationalQuery<E> query(String sql, RowMapper<E> mapper) {
        return new RelationalQuery<>(sql, template, mapper);
    }

    private <E> RelationalQuery<E> queryById(String sql, ID id, RowMapper<E> mapper) {
        RelationalQuery<E> query = query(sql, mapper);
        setId(id, query, 1);
        return query;
    }

    private <E> void setId(ID id, RelationalQuery<E> query, int base) {
        if (id instanceof Object[]) {
            Object[] objects = (Object[]) id;
            for (int i = 0; i < objects.length; i++) {
                query.setParameter(i + base, objects[i]);
            }
        } else {
            query.setParameter(base, id);
        }
    }

    @Override
    public long count() {
        return query(sql.countStar(), it -> it.getLong(1)).findOne();
    }

    @Override
    public void delete(ID id) {
        queryById(sql.deleteById(), id, null).update();
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
        query(sql.delete(), null).update();
    }

    @Override
    public boolean exists(ID id) {
        return queryById(sql.exists(), id, COUNT_MAPPER).findOne() > 0;
    }


    @Override
    public List<T> findAll() {
        return streamAll().collect(Collectors.toList());
    }

    @Override
    public T findOne(ID id) {
        return queryById(sql.selectById(), id, mappingData.getMapper()).findOne();
    }

    @Override
    public <S extends T> S save(S entity) {
        return create(entity);
    }

    @Override
    public <S extends T> S update(S entity) {
        int number = template.executeUpdate(sql.update(), entity, entityInformation.getUpdateUnmapper());
        if (number != 1) {
            throw new IncorrectResultSizeDataAccessException(1, number);
        }
        return entity;
    }

    @Override
    public <S extends T> void update(Stream<S> entity) {
        int[] count = new int[]{0};
        int[] result = template.executeBatch(sql.update(), entity.peek(it -> count[0]++), entityInformation.getUpdateUnmapper());
        for (int res : result) {
            if(res != 1) {
                throw new IncorrectResultSizeDataAccessException(1, res);
            }
        }
        if (count[0] != result.length)
            throw new IncorrectResultSizeDataAccessException(count[0], result.length);
    }

    protected <S extends T> S create(S entity) {
        if (generatedPK) {
            return template.executeInsertGenerated(sql.insertInto(), entity, mappingData.getInsertUnmapper(), entityInformation::setId);
        } else {
            template.executeUpdate(sql.insertInto(), entity, mappingData.getInsertUnmapper());
            return entity;
        }
    }


    @Override
    public <S extends T> Iterable<S> save(Iterable<S> entities) {
        Stream<S> stream = StreamSupport.stream(entities.spliterator(), false);
        if (generatedPK) {
            try (Stream<S> output = template.mapInsert(sql.insertInto(), stream, mappingData.getInsertUnmapper(), entityInformation::setId)) {
                return output.collect(Collectors.toList());
            }
        } else {
            template.executeBatch(sql.insertInto(), stream, mappingData.getInsertUnmapper());
            return entities;
        }
    }

    @Override
    public List<T> findAll(Iterable<ID> ids) {
        List<ID> idList = asList(ids);
        int pkSize = entityInformation.getPkFields().size();
        RelationalQuery<T> query = query(sql.selectAll(idList.size()), mappingData.getMapper());
        for (int i = 0; i < idList.size(); i++) {
            int c = i * pkSize + 1;
            ID id = idList.get(i);
            setId(id, query, c);
        }
        return query.list();
    }

    private static <T> List<T> asList(Iterable<T> iterable) {
        if (iterable instanceof ArrayList)
            return (List<T>) iterable;

        final List<T> result = new ArrayList<>();
        for (T item : iterable) {
            result.add(item);
        }
        return result;
    }

    @Override
    public List<T> findAll(Sort sort) {
        return query(sql.selectAll(sort), mappingData.getMapper()).list();
    }

    @Override
    public Page<T> findAll(Pageable page) {
        List<T> content = query(sql.selectAll(page), mappingData.getMapper()).list();
        return new PageImpl<>(content, page, query(sql.countStar(), COUNT_MAPPER).findOne());
    }

    public Stream<T> streamAll() {
        return query(sql.selectAll(), mappingData.getMapper()).stream();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Stream<ID> streamAllIds() {
        return query(sql.selectIds(), rs -> {
            List<FieldMappingData> pkFields = entityInformation.getPkFields();
            if (pkFields.size() == 1) {
                FieldMappingData fieldData = pkFields.get(0);
                return (ID) fieldData.attributeReader.readAttribute(rs, 1, fieldData.field);
            } else {
                Object[] result = new Object[pkFields.size()];
                for (int i = 0; i < result.length; i++) {
                    FieldMappingData fieldData = pkFields.get(i);
                    result[i] = fieldData.attributeReader.readAttribute(rs, i + 1, fieldData.field);
                }
                return (ID) result;
            }
        }).stream();
    }

    protected RelationalTemplate getTemplate() {
        return template;
    }
}