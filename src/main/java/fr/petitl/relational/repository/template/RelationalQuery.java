package fr.petitl.relational.repository.template;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.jdbc.core.RowMapper;

/**
 *
 */
public class RelationalQuery<E> {
    private final SqlQuery sql;
    private final RelationalTemplate template;
    private RowMapper<E> mapper;

    public static interface PrepareStep {
        public void prepareStatement(PreparedStatement ps) throws SQLException;
    }

    protected List<PrepareStep> toPrepare = new LinkedList<>();

    protected RelationalQuery(String sql, RelationalTemplate template, RowMapper<E> mapper) {
        this.sql = template.translateExceptions("ParseQuery", sql, () -> SqlQuery.parse(sql));
        this.template = template;
        this.mapper = mapper;
    }

    public RelationalQuery<E> setParameter(String parameter, PrepareColumn prepare) {
        return setParameter(sql.resolve(parameter), prepare);
    }

    public RelationalQuery<E> setParameter(int position, PrepareColumn prepare) {
        return setParameter(sql.resolve(position), prepare);
    }

    public RelationalQuery<E> setParameter(String parameter, Object object) {
        return setParameter(parameter, (ps,i) -> ps.setObject(i, object));
    }

    public RelationalQuery<E> setParameter(int position, Object object) {
        return setParameter(position, (ps,i) -> ps.setObject(i, object));
    }

    private RelationalQuery<E> setParameter(List<Integer> resolved, PrepareColumn prepare) {
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

    public RelationalQuery<E> addPrepareStep(PrepareStep prepareColumn) {
        toPrepare.add(prepareColumn);
        return this;
    }

    public E findOne() {
        return template.executeOne(sql.getNativeSql(), null, getPrepareStatement(), mapper);
    }

    public Stream<E> stream() {
        return template.executeQuery(sql.getNativeSql(), null, getPrepareStatement(), mapper);
    }

    public int update() {
        return template.executeUpdate(sql.getNativeSql(), null, getPrepareStatement());
    }

    private PrepareStatement<Object> getPrepareStatement() {
        // No prepare is necessary if no operation has been made
        if (toPrepare.isEmpty())
            return null;
        return (ps, ignored) -> {
            for (PrepareStep op : toPrepare) {
                op.prepareStatement(ps);
            }
        };
    }
}
