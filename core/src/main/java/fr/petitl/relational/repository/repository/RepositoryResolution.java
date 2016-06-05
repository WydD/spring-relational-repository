package fr.petitl.relational.repository.repository;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Created by WydD on 05/06/2016.
 */
public interface RepositoryResolution<T, ID> {
    <E> E findAll(Stream<ID> idStream, Function<Stream<T>, E> apply);
}
