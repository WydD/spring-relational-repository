package fr.petitl.relational.repository;

import java.lang.annotation.*;

import fr.petitl.relational.repository.support.RelationalRepositoryBeanDefinitionRegistrarSupport;
import fr.petitl.relational.repository.support.RelationalRepositoryFactoryBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.repository.query.QueryLookupStrategy;

/**
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(RelationalRepositoryBeanDefinitionRegistrarSupport.class)
public @interface EnableRelationalRepositories {

    String[] value() default {};

    String[] basePackages() default {};

    Class<?>[] basePackageClasses() default {};

    ComponentScan.Filter[] includeFilters() default {};

    ComponentScan.Filter[] excludeFilters() default {};

    String repositoryImplementationPostfix() default "Impl";

    String namedQueriesLocation() default "";

    QueryLookupStrategy.Key queryLookupStrategy() default QueryLookupStrategy.Key.CREATE_IF_NOT_FOUND;

    Class<?> repositoryFactoryBeanClass() default RelationalRepositoryFactoryBean.class;

    String relationalTemplateRef() default "relationalTemplate";
}
