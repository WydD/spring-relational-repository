package fr.petitl.relational.repository.annotation;

import java.lang.annotation.*;

/**
 * Created by loic on 03/06/15.
 */

@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PKClass {
    Class<?> value() default Object.class;
}
