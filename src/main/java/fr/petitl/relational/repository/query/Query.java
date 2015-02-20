package fr.petitl.relational.repository.query;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import fr.petitl.relational.repository.template.RowMapper;

/**
 *
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Query {
    public String value();

    public Class<? extends RowMapper> mapper() default RowMapper.class;
}
