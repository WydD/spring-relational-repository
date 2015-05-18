package fr.petitl.relational.repository.query;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface CollectorFunction {
    /**
     * Specify the row type
     *
     * @return The row type class
     */
    Class<?> value() default Object.class;
}
