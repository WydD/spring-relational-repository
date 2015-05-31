package fr.petitl.relational.repository.repository.model;

import java.util.Date;

import fr.petitl.relational.repository.annotation.PK;
import fr.petitl.relational.repository.annotation.Table;

/**
 *
 */
@Table("Multiple")
public class Multiple {
    public final static String CREATE = "create table Multiple(id bigint auto_increment, type VARCHAR(255) NOT NULL, name varchar(255), created_date timestamp, primary key (id, type))";
    public final static String INSERT = "insert into Multiple VALUES " +
            "(1, 'canard', 'Hey', '2014-05-14 20:02:32'), " +
            "(2, 'canard', 'Ho', '2014-05-19 20:02:32'), " +
            "(3, 'youpi', 'Hey', '2014-02-14 20:02:32')";

    @PK(generated = true, order = 1)
    private Integer id;

    @PK(order = 2)
    private String type;

    private String name;

    private Date createdDate;

    public Multiple(Integer id, String type, String name, Date createdDate) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.createdDate = createdDate;
    }

    public Multiple() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

        Multiple multiple = (Multiple) o;

        if (id != null ? !id.equals(multiple.id) : multiple.id != null) return false;
        return !(type != null ? !type.equals(multiple.type) : multiple.type != null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }
}
