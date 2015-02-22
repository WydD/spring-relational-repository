package fr.petitl.relational.repository.dialect;

import java.io.Serializable;

import fr.petitl.relational.repository.repository.SQLGeneration;
import fr.petitl.relational.repository.support.RelationalEntityInformation;
import fr.petitl.relational.repository.template.bean.BeanAttributeReader;
import fr.petitl.relational.repository.template.bean.BeanAttributeWriter;

/**
 *
 */
public interface BeanDialect {
    <T, ID extends Serializable> SQLGeneration sql(RelationalEntityInformation<T, ID> entityInformation);

    BeanAttributeReader defaultReader();

    BeanAttributeWriter defaultWriter();
}
