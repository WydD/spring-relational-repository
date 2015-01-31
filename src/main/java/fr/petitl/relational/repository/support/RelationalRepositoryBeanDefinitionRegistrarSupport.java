package fr.petitl.relational.repository.support;

import java.lang.annotation.Annotation;

import fr.petitl.relational.repository.EnableRelationalRepositories;
import org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupport;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

/**
 *
 */
public class RelationalRepositoryBeanDefinitionRegistrarSupport extends RepositoryBeanDefinitionRegistrarSupport {
    @Override
    protected Class<? extends Annotation> getAnnotation() {
        return EnableRelationalRepositories.class;
    }

    @Override
    protected RepositoryConfigurationExtension getExtension() {
        return new RelationalRepositoryConfigurationExtension();
    }
}
