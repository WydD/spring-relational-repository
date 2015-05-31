package fr.petitl.relational.repository.support;

import java.io.Serializable;

import fr.petitl.relational.repository.template.RelationalTemplate;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.util.Assert;

/**
 *
 */
public class RelationalRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable> extends
        RepositoryFactoryBeanSupport<T, S, ID> {

    private RelationalTemplate operations;

    @Override
    protected RepositoryFactorySupport createRepositoryFactory() {
        return new RelationalRepositoryFactorySupport(operations);
    }

    public void setOperations(RelationalTemplate operations) {
        this.operations = operations;
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        Assert.notNull(operations, "RelationalTemplate must not be null!");
    }
}
