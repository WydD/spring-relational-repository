package fr.petitl.relational.repository.dialect.generic;

import java.io.Serializable;

import fr.petitl.relational.repository.dialect.BeanDialect;
import fr.petitl.relational.repository.repository.SQLGeneration;
import fr.petitl.relational.repository.support.RelationalEntityInformation;
import fr.petitl.relational.repository.template.bean.BeanAttributeReader;
import fr.petitl.relational.repository.template.bean.BeanAttributeWriter;

/**
 *
 */
public class GenericBeanDialect implements BeanDialect {
    @Override
    public <T, ID extends Serializable> SQLGeneration sql(RelationalEntityInformation<T, ID> entityInformation) {
        return new LimitBasedSQLGeneration<>(entityInformation);
    }

    @Override
    public BeanAttributeReader defaultReader() {
        return GenericBeanAttributeManager.INSTANCE;
    }

    @Override
    public BeanAttributeWriter defaultWriter() {
        return GenericBeanAttributeManager.INSTANCE;
    }
}
