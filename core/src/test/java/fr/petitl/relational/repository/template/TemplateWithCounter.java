package fr.petitl.relational.repository.template;

import fr.petitl.relational.repository.dialect.BeanDialect;
import fr.petitl.relational.repository.template.bean.NamingConvention;
import org.springframework.dao.DataAccessException;

import javax.sql.DataSource;
import java.sql.Statement;

/**
 * Created by WydD on 16/08/2015.
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
    protected <E extends Statement, T> T executeDontClose(StatementCallback<E, T> action, StatementProvider<E> supplier) throws DataAccessException {
        queryCounter++;
        return super.executeDontClose(action, supplier);
    }

    public long getQueryCounter() {
        return queryCounter;
    }
}
