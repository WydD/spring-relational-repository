package fr.petitl.relational.repository.template;

import javax.sql.DataSource;
import java.sql.Statement;

import fr.petitl.relational.repository.dialect.BeanDialect;
import fr.petitl.relational.repository.template.bean.NamingConvention;
import org.springframework.dao.DataAccessException;

/**
 * Template capable of counting all queries.
 */
public class TemplateWithCounter extends RelationalTemplate {

    private long queryCounter = 0;

    public TemplateWithCounter(DataSource ds, BeanDialect dialect) {
        super(ds, dialect);
    }

    public TemplateWithCounter(DataSource ds, BeanDialect dialect, NamingConvention namingConvention) {
        super(ds, dialect, namingConvention);
    }

    @Override
    protected <E extends Statement, T> T executeDontClose(StatementProvider<E> supplier, StatementCallback<E, T> action) throws DataAccessException {
        queryCounter++;
        return super.executeDontClose(supplier, action);
    }

    public long getQueryCounter() {
        return queryCounter;
    }
}
