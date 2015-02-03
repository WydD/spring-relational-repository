package fr.petitl.relational.repository.annotation;

import java.lang.annotation.*;

import fr.petitl.relational.repository.DBMapping;
import fr.petitl.relational.repository.repository.BeanAttributeReader;
import fr.petitl.relational.repository.repository.BeanAttributeWriter;

/**
 *
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Column {
    String name() default "";
    Class<? extends BeanAttributeReader> reader() default BeanAttributeReader.Default.class;
    Class<? extends BeanAttributeWriter> writer() default BeanAttributeWriter.Default.class;
}
