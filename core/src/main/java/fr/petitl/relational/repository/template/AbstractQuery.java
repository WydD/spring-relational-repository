package fr.petitl.relational.repository.template;

import fr.petitl.relational.repository.template.bean.BeanAttributeWriter;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

public abstract class AbstractQuery<B extends AbstractQuery> {

    protected final SqlStatement sql;
    protected final RelationalTemplate template;
    protected boolean preparing = true;

    protected Function<Object, ColumnMapper> defaultSetter;

    public interface PrepareStep {
        void prepareStatement(PreparedStatement ps) throws SQLException;
    }

    protected List<PrepareStep> toPrepare = new LinkedList<>();

    public AbstractQuery(String sql, RelationalTemplate template) {
        this.sql = template.translateExceptions("ParseQuery", sql, () -> SqlStatement.parse(sql));
        this.template = template;
        BeanAttributeWriter defaultWriter = template.getDialect().defaultWriter();
        defaultSetter = obj -> (ps, i) -> defaultWriter.writeAttribute(ps, i, obj, null);
    }

    public B setParameter(String parameter, ColumnMapper prepare) {
        return setParameter(sql.resolve(parameter), prepare);
    }

    public B setParameter(int position, ColumnMapper prepare) {
        return setParameter(sql.resolve(position), prepare);
    }

    public B setParameter(String parameter, Object object) {
        return setParameter(parameter, defaultSetter.apply(object));
    }

    public B setParameter(int position, Object object) {
        return setParameter(position, defaultSetter.apply(object));
    }

    protected B setParameter(List<Integer> resolved, ColumnMapper prepare) {
        if (!preparing) {
            clearParameters();
            preparing = true;
        }
        if (resolved.size() == 1) {
            Integer idx = resolved.get(0);
            return addPrepareStep(ps -> prepare.prepareColumn(ps, idx));
        }
        return addPrepareStep(ps -> {
            for (Integer idx : resolved) {
                prepare.prepareColumn(ps, idx);
            }
        });
    }

    public void clearParameters() {
        toPrepare.clear();
    }

    public B addPrepareStep(PrepareStep prepareColumn) {
        toPrepare.add(prepareColumn);
        //noinspection unchecked
        return (B) this;
    }

    public SqlStatement getSql() {
        return sql;
    }

    protected StatementMapper<Object> getPrepareStatement() {
        preparing = false;
        // No prepare is necessary if no operation has been made
        if (toPrepare.isEmpty())
            return null;
        return (ps, ignored, i) -> {
            prepare(ps);
        };
    }

    private void prepare(PreparedStatement ps) throws SQLException {
        for (PrepareStep op : toPrepare) {
            op.prepareStatement(ps);
        }
    }
}
