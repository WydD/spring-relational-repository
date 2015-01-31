package fr.petitl.relational.repository.annotation;

import java.lang.annotation.*;

import fr.petitl.relational.repository.DBMapping;

/**
 *
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Column {
    String name() default "";
    Class<? extends DBMapping> mapping() default DBMapping.Default.class;
}
