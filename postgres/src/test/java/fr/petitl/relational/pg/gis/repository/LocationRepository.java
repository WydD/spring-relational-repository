package fr.petitl.relational.pg.gis.repository;

import fr.petitl.relational.pg.gis.model.Location;
import fr.petitl.relational.repository.query.Query;
import fr.petitl.relational.repository.repository.RelationalRepository;

public interface LocationRepository extends RelationalRepository<Location, String> {
    @Query("SELECT * FROM location WHERE ST_Within(ST_SetSRID(ST_MakePoint(?0,?1),4326), geometry)")
    Location findAt(double lon, double lat);
}
