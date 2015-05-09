package fr.petitl.relational.repository.simple.repository;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import fr.petitl.relational.repository.query.Query;
import fr.petitl.relational.repository.repository.RelationalRepository;
import fr.petitl.relational.repository.simple.Pojo;
import org.springframework.data.repository.query.Param;

/**
 *
 */
public interface PojoRepository extends RelationalRepository<Pojo, String>, PojoRepositoryCustom {
    @Query("SELECT * FROM Pojo WHERE name = :name")
    Pojo testGet(@Param("name") String name);

    @Query("SELECT * FROM Pojo WHERE name = ?0")
    Pojo testGetPositional(String name);

    @Query("SELECT * FROM Pojo WHERE name = ?0")
    List<Pojo> testGetPositionalList(String name);

    @Query("SELECT * FROM Pojo WHERE name = ?0")
    <E> E testApplyStream(String name, Function<Stream<Pojo>, E> apply);
}
