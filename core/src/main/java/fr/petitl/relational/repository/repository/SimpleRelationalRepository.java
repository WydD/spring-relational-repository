package fr.petitl.relational.repository.repository;

import fr.petitl.relational.repository.dialect.BeanSQLGeneration;
import fr.petitl.relational.repository.dialect.PagingGeneration;
import fr.petitl.relational.repository.support.RelationalEntityInformation;
import fr.petitl.relational.repository.template.*;
import fr.petitl.relational.repository.template.bean.BeanMappingData;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class SimpleRelationalRepository<T, ID extends Serializable> implements RelationalRepository<T, ID> {
    private static final RowMapper<Long> COUNT_MAPPER = it -> it.getLong(1);

    private final Function<T, ID> idGetter;
    private final RelationalEntityInformation<T, ID> entityInformation;
    private final BeanSQLGeneration sql;
    private final BeanMappingData<T> mappingData;

    private final boolean generatedPK;
    private final PagingGeneration paging;

    private RelationalTemplate template;

    public SimpleRelationalRepository(RelationalEntityInformation<T, ID> entityInformation, RelationalTemplate template) {
        this.template = template;
        sql = template.getDialect().sql(entityInformation);
        paging = template.getDialect().paging();

        mappingData = entityInformation.getMappingData();

        idGetter = entityInformation::getId;
        generatedPK = entityInformation.isGeneratedPK();

        this.entityInformation = entityInformation;
    }

    public <E> SelectQuery<E> query(String sql, RowMapper<E> mapper) {
        return new SelectQuery<>(sql, template, mapper);
    }

    private <E> SelectQuery<E> queryById(String sql, ID id, RowMapper<E> mapper) {
        SelectQuery<E> query = query(sql, mapper);
        setId(id, query, 1);
        return query;
    }

    private void setId(ID id, AbstractQuery query, int base) {
        query.addPrepareStep(pse -> entityInformation.getIdUnmapper().prepare(pse, id, base));
    }

    @Override
    public long count() {
        return query(sql.countStar(), it -> it.getLong(1)).findOne();
    }

    @Override
    public void delete(ID id) {
        UpdateQuery query = template.createQuery(sql.deleteById());
        setId(id, query, 1);
        query.execute();
    }

    @Override
    public int deleteByIds(Stream<ID> idsStream) {
        Set<ID> ids = idsStream.collect(Collectors.toSet());
        UpdateQuery query = template.createQuery(sql.deleteAll(ids.size()));
        setIds(ids, query);
        return query.execute();
    }

    @Override
    public void deleteAll() {
        template.createQuery(sql.delete()).execute();
    }

    @Override
    public boolean exists(ID id) {
        return queryById(sql.exists(), id, COUNT_MAPPER).findOne() > 0;
    }


    @Override
    public List<T> findAll() {
        return fetchAll(stream -> stream.collect(Collectors.toList()));
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
            if (res != 1) {
                throw new IncorrectResultSizeDataAccessException(1, res);
            }
        }
        if (count[0] != result.length)
            throw new IncorrectResultSizeDataAccessException(count[0], result.length);
    }

    @Override
    public Function<T, ID> pkGetter() {
        return idGetter;
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
            try (Stream<S> output = template.executeStreamInsertGenerated(sql.insertInto(), stream, mappingData.getInsertUnmapper(), entityInformation::setId)) {
                return output.collect(Collectors.toList());
            }
        } else {
            template.executeBatch(sql.insertInto(), stream, mappingData.getInsertUnmapper());
            return entities;
        }
    }

    @Override
    public <F> F findAll(Stream<ID> ids, Function<Stream<T>, F> apply) {
        Set<ID> idList = ids.collect(Collectors.toSet());
        if (idList.isEmpty()) {
            return apply.apply(Stream.empty());
        }
        SelectQuery<T> query = query(sql.selectAll(idList.size()), mappingData.getMapper());
        setIds(idList, query);
        return query.fetch(apply);
    }

    protected void setIds(Set<ID> idList, AbstractQuery query) {
        int pkSize = entityInformation.getPkFields().size();
        int c = 1;
        for (ID id : idList) {
            setId(id, query, c);
            c += pkSize;
        }
    }

    @Override
    public List<T> findAll(Sort sort) {
        return query(paging.sort(sql.selectAll(), sort), mappingData.getMapper()).list();
    }

    @Override
    public Page<T> findAll(Pageable page) {
        String sql = paging.paging(this.sql.selectAll(), page);
        List<T> content = query(sql, mappingData.getMapper()).list();
        return new PageImpl<>(content, page, query(this.sql.countStar(), COUNT_MAPPER).findOne());
    }

    @Override
    public <F> F fetchAll(Function<Stream<T>, F> apply) {
        return query(sql.selectAll(), mappingData.getMapper()).fetch(apply);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <F> F fetchAllIds(Function<Stream<ID>, F> apply) {
        return query(sql.selectIds(), rs -> entityInformation.getIdMapper().mapRow(rs)).fetch(apply);
    }

    protected RelationalTemplate getTemplate() {
        return template;
    }
}