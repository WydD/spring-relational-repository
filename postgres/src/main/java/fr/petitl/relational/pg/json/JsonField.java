package fr.petitl.relational.pg.json;

import java.lang.annotation.*;

/**
 * Created by loic on 11/06/15.
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface JsonField {
    boolean binary() default true;
}
