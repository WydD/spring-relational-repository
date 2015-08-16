package fr.petitl.relational.repository.repository;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 *
 */
@NoRepositoryBean
public interface RelationalRepository<T, ID extends Serializable> extends PagingAndSortingRepository<T, ID> {
    <F> F findAll(Stream<ID> ids, Function<Stream<T>, F> apply);

    <F> F fetchAll(Function<Stream<T>, F> apply);

    <F> F fetchAllIds(Function<Stream<ID>, F> apply);

    int deleteByIds(Stream<ID> ids);

    <S extends T> S update(S entity);

    <S extends T> void update(Stream<S> entity);

    Function<T, ID> pkGetter();

    default <F, G> G resolveFK(Stream<F> stream, Function<F, ID> fkGetter, Function<Stream<T>, G> apply) {
        return findAll(stream.map(fkGetter).filter(it -> it != null), apply);
    }

    default <F> Map<ID, T> resolveFK(Stream<F> stream, Function<F, ID> fkGetter) {
        return resolveFK(stream, fkGetter, asIndex());
    }

    default void delete(T entity) {
        delete(pkGetter().apply(entity));
    }

    default int delete(Stream<? extends T> entities) {
        return deleteByIds(entities.map(pkGetter()));
    }

    default int deleteByIds(Iterable<ID> ids) {
        return deleteByIds(StreamSupport.stream(ids.spliterator(), false));
    }

    default void delete(Iterable<? extends T> entities) {
        delete(StreamSupport.stream(entities.spliterator(), false));
    }

    default Function<Stream<T>, Map<ID, T>> asIndex() {
        return stream -> stream.collect(Collectors.toMap(pkGetter(), it -> it));
    }

    default List<T> findAll(Iterable<ID> ids) {
        return findAll(StreamSupport.stream(ids.spliterator(), false), it -> it.collect(Collectors.toList()));
    }
}
