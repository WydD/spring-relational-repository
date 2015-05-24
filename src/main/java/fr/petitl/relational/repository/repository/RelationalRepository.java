package fr.petitl.relational.repository.repository;

import java.io.Serializable;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 *
 */
@NoRepositoryBean
public interface RelationalRepository<T, ID extends Serializable> extends PagingAndSortingRepository<T, ID> {
    public List<T> findAll(Iterable<ID> ids);

    public <F> F fetchAll(Function<Stream<T>, F> apply);

    public <F> F fetchAllIds(Function<Stream<ID>, F> apply);

    public <S extends T> S update(S entity);

    public <S extends T> void update(Stream<S> entity);

    public FK<ID, T> fid(ID id);

    public FK<ID, T> fk(T obj);
}
