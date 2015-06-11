package fr.petitl.relational.repository.dialect;

import java.util.function.Function;

import fr.petitl.relational.repository.dialect.generic.SpringJDBCAttributeMapper;
import fr.petitl.relational.repository.dialect.generic.StandardSQLGeneration;
import fr.petitl.relational.repository.support.RelationalEntityInformation;
import fr.petitl.relational.repository.template.bean.BeanAttributeReader;
import fr.petitl.relational.repository.template.bean.BeanAttributeWriter;

/**
 *
 */
public class BeanDialect {

    private final Function<RelationalEntityInformation<?, ?>, BeanSQLGeneration> sqlProvider;
    private final PagingGeneration paging;
    private final BeanAttributeReader reader;
    private final BeanAttributeWriter writer;

    public BeanDialect(Function<RelationalEntityInformation<?, ?>, BeanSQLGeneration> sqlProvider, PagingGeneration paging, BeanAttributeReader reader, BeanAttributeWriter writer) {
        this.sqlProvider = sqlProvider;
        this.paging = paging;
        this.reader = reader;
        this.writer = writer;
    }

    public BeanDialect(Function<RelationalEntityInformation<?, ?>, BeanSQLGeneration> sqlProvider, PagingGeneration paging) {
        this.sqlProvider = sqlProvider;
        this.paging = paging;
        SpringJDBCAttributeMapper manager = new SpringJDBCAttributeMapper();
        this.reader = manager;
        this.writer = manager;
    }

    public BeanDialect(PagingGeneration paging) {
        this(StandardSQLGeneration::new, paging);
    }

    public BeanSQLGeneration sql(RelationalEntityInformation<?, ?> entityInformation) {
        return sqlProvider.apply(entityInformation);
    }

    public PagingGeneration paging() {
        return paging;
    }

    public BeanAttributeReader defaultReader() {
        return reader;
    }

    public BeanAttributeWriter defaultWriter() {
        return writer;
    }
}
