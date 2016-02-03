package fr.petitl.relational.repository.template;

import java.util.function.Function;

import fr.petitl.relational.repository.query.parametered.FullQuery;
import fr.petitl.relational.repository.template.bean.BeanAttributeWriter;

public abstract class AbstractQuery<B extends AbstractQuery> {

    protected final RelationalTemplate template;
    protected final FullQuery query;

    protected Function<Object, ColumnMapper> defaultSetter;
    private final QueryParser parsedQuery;

    public AbstractQuery(String sql, RelationalTemplate template) {
        parsedQuery = template.translateExceptions("ParseQuery", sql, () -> new QueryParser(sql));
        query = parsedQuery.getQuery();
        this.template = template;
        BeanAttributeWriter defaultWriter = template.getDialect().defaultWriter();
        defaultSetter = obj -> (ps, i) -> defaultWriter.writeAttribute(ps, i, obj, null);
    }

    public B setParameter(String parameter, ColumnMapper prepare) {
        return setParameter(parsedQuery.resolve(parameter), prepare);
    }

    public B setParameter(int position, ColumnMapper prepare) {
        query.setParameter(position, prepare);
        //noinspection unchecked
        return (B) this;
    }

    public B setParameter(String parameter, Object object) {
        return setParameter(parsedQuery.resolve(parameter), object);
    }

    public B setParameter(int position, Object object) {
        query.setParameter(position, object, defaultSetter);
        //noinspection unchecked
        return (B) this;
    }

    public void clearParameters() {
        query.clear();
    }

    public FullQuery getQuery() {
        return query;
    }

    public QueryParser getParsedQuery() {
        return parsedQuery;
    }

    protected StatementMapper<Object> getPrepareStatement() {
        // No prepare is necessary if no operation has been made
        if (query.getNumberOfArguments() == 0) return null;

        // else, send the prepare step of query
        return (ps, ignored, i) -> query.prepare(ps);
    }
}
