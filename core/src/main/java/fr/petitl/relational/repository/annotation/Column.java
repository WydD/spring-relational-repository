package fr.petitl.relational.repository.annotation;

import java.lang.annotation.*;

import fr.petitl.relational.repository.template.bean.BeanAttributeReader;
import fr.petitl.relational.repository.template.bean.BeanAttributeWriter;
import fr.petitl.relational.repository.template.bean.VoidBeanAttributeMapper;

/**
 *
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Column {
    String name() default "";

    Class<? extends BeanAttributeReader> reader() default VoidBeanAttributeMapper.class;

    Class<? extends BeanAttributeWriter> writer() default VoidBeanAttributeMapper.class;
}
