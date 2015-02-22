package fr.petitl.relational.repository.template;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.petitl.relational.repository.template.bean.BeanAttributeWriter;

/**
 *
 */
public class RelationalQuery<E> {
    private final SqlQuery sql;
    private final RelationalTemplate template;
    private RowMapper<E> mapper;
    private boolean preparing = true;


    private Function<Object, ColumnMapper> defaultSetter;

    public static interface PrepareStep {
        public void prepareStatement(PreparedStatement ps) throws SQLException;
    }

    protected List<PrepareStep> toPrepare = new LinkedList<>();

    public RelationalQuery(String sql, RelationalTemplate template, RowMapper<E> mapper) {
        this.sql = template.translateExceptions("ParseQuery", sql, () -> SqlQuery.parse(sql));
        this.template = template;
        BeanAttributeWriter defaultWriter = template.getDialect().defaultWriter();
        defaultSetter = obj -> (ps, i) -> defaultWriter.writeAttribute(ps, i, obj, null);
        this.mapper = mapper;
    }

    public RelationalQuery<E> setParameter(String parameter, ColumnMapper prepare) {
        return setParameter(sql.resolve(parameter), prepare);
    }

    public RelationalQuery<E> setParameter(int position, ColumnMapper prepare) {
        return setParameter(sql.resolve(position), prepare);
    }

    public RelationalQuery<E> setParameter(String parameter, Object object) {
        return setParameter(parameter, defaultSetter.apply(object));
    }

    public RelationalQuery<E> setParameter(int position, Object object) {
        return setParameter(position, defaultSetter.apply(object));
    }

    private RelationalQuery<E> setParameter(List<Integer> resolved, ColumnMapper prepare) {
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

    public RelationalQuery<E> addPrepareStep(PrepareStep prepareColumn) {
        toPrepare.add(prepareColumn);
        return this;
    }

    private Stream<E> stream() {
        return template.executeQuery(sql.getNativeSql(), null, getPrepareStatement(), mapper);
    }

    public <F> F fetch(Function<Stream<E>, F> transformer) {
        try (Stream<E> out = stream()) {
            return transformer.apply(out);
        }
    }

    public List<E> list() {
        return fetch(stream -> stream.collect(Collectors.toList()));
    }

    public E findOne() {
        return fetch(stream -> stream.findFirst().orElse(null));
    }

    public int update() {
        return template.executeUpdate(sql.getNativeSql(), null, getPrepareStatement());
    }

    public SqlQuery getSql() {
        return sql;
    }

    private StatementMapper<Object> getPrepareStatement() {
        preparing = false;
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
