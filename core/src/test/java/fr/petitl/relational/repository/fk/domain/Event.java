package fr.petitl.relational.repository.fk.domain;

import fr.petitl.relational.repository.annotation.PK;
import fr.petitl.relational.repository.annotation.Table;

/**
 *
 */
@Table("Event")
public class Event {
    @PK(generated = true)
    private Integer id;

    private String name;

    private String countryId;

    private Integer locationId;

    public Event() {
    }

    public Event(String name, Location location) {
        this.name = name;
        if (location != null) {
            this.countryId = location.getCountryId();
            this.locationId = location.getId();
        }
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

    public Integer getLocationId() {
        return locationId;
    }

    public void setLocationId(Integer locationId) {
        this.locationId = locationId;
    }

    public String getCountryId() {
        return countryId;
    }

    public void setCountryId(String countryId) {
        this.countryId = countryId;
    }
}
