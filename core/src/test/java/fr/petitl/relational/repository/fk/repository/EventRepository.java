package fr.petitl.relational.repository.fk.repository;

import java.util.Collection;

import fr.petitl.relational.repository.fk.domain.Event;
import fr.petitl.relational.repository.query.Query;
import fr.petitl.relational.repository.repository.RelationalRepository;

/**
 *
 */
public interface EventRepository extends RelationalRepository<Event, Integer> {
    @Query("SELECT * FROM Event WHERE name = ?0")
    Event findByName(String name);

    @Query("SELECT * FROM Event WHERE name #IN (?0)")
    Event findByName(Collection<String> name);

    @Query("SELECT * FROM Event WHERE (country_id, location_id) #IN (?0)")
//    @Query("SELECT * FROM Location WHERE #each{?0 ; countryId = $countryId AND locationId = $id ; OR}")
//    @Query("SELECT * FROM Location WHERE (countryId, locationId) IN (#each{?0;($countryId, $id);,})")
//    @Query("SELECT * FROM Location WHERE id IN (#each{?0;$;,})")
//    @Query("SELECT * FROM Location WHERE #in{?0;countryId = $countryId;locationId = $id}")
    Event findByLocation(Collection<Object[]> name);
}
