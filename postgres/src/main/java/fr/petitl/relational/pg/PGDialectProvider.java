package fr.petitl.relational.pg;

import fr.petitl.relational.repository.dialect.BeanDialect;
import fr.petitl.relational.repository.dialect.generic.CompositeAsTupleSQLGeneration;
import fr.petitl.relational.repository.dialect.paging.FetchFirstPaging;
import fr.petitl.relational.repository.query.macro.SingleInMacro;
import fr.petitl.relational.repository.template.bean.BeanAttributeMapper;

public class PGDialectProvider {
    /**
     * Provide the dialect to manipulate a postgresql database
     * @return The dialect
     */
    public static BeanDialect get() {
        final BeanAttributeMapper attributeMapper = new PGAttributeMapper();
        return new BeanDialect(CompositeAsTupleSQLGeneration::new, new FetchFirstPaging(true), attributeMapper, attributeMapper).addMacro(new SingleInMacro());
    }
}
