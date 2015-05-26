package fr.petitl.relational.repository.fk.domain;

import fr.petitl.relational.repository.annotation.PK;
import fr.petitl.relational.repository.annotation.Table;
import fr.petitl.relational.repository.repository.FK;

/**
 *
 */
@Table("Event")
public class Event {
    @PK(generated = true)
    private Integer id;

    private String name;

    /**
     * This foreign key references a Location instance using the column "location_id"
     * (having `_id` is implicit by default). (see db-schema.sql in the fk package)
     *
     * The resolution is made outside the query using FK::resolve
     */
    private FK<Integer, Location> location;

    public Event() {
    }

    public Event(String name, FK<Integer, Location> location) {
        this.name = name;
        this.location = location;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FK<Integer, Location> getLocation() {
        return location;
    }

    public void setLocation(FK<Integer, Location> location) {
        this.location = location;
    }
}
