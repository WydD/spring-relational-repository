package fr.petitl.relational.repository.fk.domain;

import fr.petitl.relational.repository.annotation.Column;
import fr.petitl.relational.repository.annotation.PK;
import fr.petitl.relational.repository.annotation.Table;

/**
 *
 */
@Table("Location")
public class Location {
    @PK
    private String countryId;

    @PK(generated = true, order = 2)
    @Column(name = "location_id")
    private Integer id;

    private String name;

    public Location() {
    }

    public Location(String countryId, String name) {
        this.countryId = countryId;
        this.name = name;
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

    public String getCountryId() {
        return countryId;
    }

    public void setCountryId(String countryId) {
        this.countryId = countryId;
    }
}
