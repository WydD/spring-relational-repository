package fr.petitl.relational.repository.fk.domain;

import fr.petitl.relational.repository.annotation.Column;
import fr.petitl.relational.repository.annotation.PK;
import fr.petitl.relational.repository.annotation.Table;

@Table("Country")
public class Country {
    @Column(name = "country_id")
    @PK
    private String id;

    private String name;

    private Integer capitalId;

    public Country(String id, String name, Integer capitalId) {
        this.id = id;
        this.name = name;
        this.capitalId = capitalId;
    }

    public Country() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCapitalId() {
        return capitalId;
    }

    public void setCapitalId(Integer capitalId) {
        this.capitalId = capitalId;
    }
}
