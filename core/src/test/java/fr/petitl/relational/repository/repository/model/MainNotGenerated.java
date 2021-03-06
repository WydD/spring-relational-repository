package fr.petitl.relational.repository.repository.model;

import java.util.Date;

import fr.petitl.relational.repository.annotation.PK;
import fr.petitl.relational.repository.annotation.Table;

/**
 *
 */
@Table("MainNotGenerated")
public class MainNotGenerated {
    public final static String CREATE = "create table MainNotGenerated(id bigint primary key, name varchar(255), created_date timestamp)";
    public final static String INSERT = "insert into MainNotGenerated VALUES " +
            "(1, 'Hey', '2014-05-14 20:02:32'), " +
            "(2, 'Ho', '2014-05-19 20:02:32'), " +
            "(3, 'Hey', '2014-02-14 20:02:32')";

    @PK
    private Integer id;

    private String name;

    private Date createdDate;

    public MainNotGenerated() {
    }

    public MainNotGenerated(Integer id, String name, Date createdDate) {
        this.id = id;
        this.name = name;
        this.createdDate = createdDate;
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

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MainNotGenerated that = (MainNotGenerated) o;

        return !(id != null ? !id.equals(that.id) : that.id != null);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
