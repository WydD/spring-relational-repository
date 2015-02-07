package fr.petitl.relational.repository.repository;

import java.io.Serializable;
import java.util.stream.Stream;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 *
 */
@NoRepositoryBean
public interface RelationalRepository<T, ID extends Serializable> extends PagingAndSortingRepository<T, ID> {

    public Stream<T> streamAll();

    public Stream<ID> streamAllIds();

    public <S extends T> S update(S entity);

    public <S extends T> void update(Stream<S> entity);
}
