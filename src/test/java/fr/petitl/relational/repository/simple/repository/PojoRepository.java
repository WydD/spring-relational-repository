package fr.petitl.relational.repository.simple.repository;

import fr.petitl.relational.repository.query.Query;
import fr.petitl.relational.repository.repository.RelationalRepository;
import fr.petitl.relational.repository.simple.Pojo;
import org.springframework.data.repository.query.Param;

/**
 *
 */
public interface PojoRepository extends RelationalRepository<Pojo, String>, PojoRepositoryCustom {
    @Query("SELECT * FROM Pojo WHERE name = :name")
    public Pojo testGet(@Param("name") String name);

    @Query("SELECT * FROM Pojo WHERE name = ?0")
    public Pojo testGetPositional(String name);
}
