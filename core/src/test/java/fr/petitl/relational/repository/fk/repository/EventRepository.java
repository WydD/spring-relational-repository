package fr.petitl.relational.repository.fk.repository;

import fr.petitl.relational.repository.fk.domain.Event;
import fr.petitl.relational.repository.query.Query;
import fr.petitl.relational.repository.repository.RelationalRepository;

import java.util.Collection;
import java.util.List;

/**
 *
 */
public interface EventRepository extends RelationalRepository<Event, Integer> {
    @Query("SELECT * FROM Event WHERE name = ?0")
    Event findByName(String name);

    @Query("SELECT * FROM Event WHERE name #IN (?0)")
    Event findByName(Collection<String> name);

    @Query("SELECT * FROM Event WHERE (country_id, location_id) #IN (?0)")
    Event findByLocation(Collection<Object[]> name);
}
