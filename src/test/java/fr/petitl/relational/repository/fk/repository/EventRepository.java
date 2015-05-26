package fr.petitl.relational.repository.fk.repository;

import fr.petitl.relational.repository.fk.domain.Event;
import fr.petitl.relational.repository.query.Query;
import fr.petitl.relational.repository.repository.RelationalRepository;

/**
 *
 */
public interface EventRepository extends RelationalRepository<Event, Integer> {
    @Query("SELECT * FROM Event WHERE name = ?0")
    Event findByName(String name);
}
