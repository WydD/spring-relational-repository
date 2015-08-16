package fr.petitl.relational.repository.template;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.petitl.relational.repository.template.bean.BeanAttributeWriter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionTemplate;

/**
 *
 */
public class RelationalQuery<E> {
    private final SqlQuery sql;
    private final RelationalTemplate template;
    private RowMapper<E> mapper;
    private boolean preparing = true;


    private Function<Object, ColumnMapper> defaultSetter;
    private Pageable pageable;

    public interface PrepareStep {
        void prepareStatement(PreparedStatement ps) throws SQLException;
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

    public RelationalQuery<E> setPageable(Pageable pageable) {
        if (!preparing) {
            clearParameters();
            clearPageable();
            preparing = true;
        }
        this.pageable = pageable;
        return this;
    }

    private RelationalQuery<E> setParameter(List<Integer> resolved, ColumnMapper prepare) {
        if (!preparing) {
            clearParameters();
            clearPageable();
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

    public void clearPageable() {
        this.pageable = null;
    }

    public RelationalQuery<E> addPrepareStep(PrepareStep prepareColumn) {
        toPrepare.add(prepareColumn);
        return this;
    }

    private Stream<E> stream() {
        return template.executeQuery(sql.getNativeSql(), null, getPrepareStatement(), mapper);
    }

    public <F> F fetch(Function<Stream<E>, F> collectorFunction) {
        try (Stream<E> out = stream()) {
            return collectorFunction.apply(out);
        }
    }

    public Page<E> page() {
        if (pageable == null) {
            throw new IllegalStateException("Querying a page without a Pageable");
        }
        TransactionTemplate transactionTemplate = template.getTransactionTemplate();
        return transactionTemplate.execute(status -> {
            List<E> result;
            Long count;

            // Get the list of the page
            String nativeSql = template.getDialect().paging().paging(sql.getNativeSql(), pageable);
            StatementMapper<Object> prepare = getPrepareStatement();
            try (Stream<E> tmp = template.executeQuery(nativeSql, null, prepare, mapper)) {
                result = tmp.collect(Collectors.toList());
            }
            // count the result
            String countQuery = "SELECT COUNT(*) FROM (" + this.sql.getNativeSql() + ") SUBQ";
            try (Stream<Long> tmp = template.executeQuery(countQuery, null, prepare, rs -> rs.getLong(1))) {
                count = tmp.findFirst().get();
            }
            return new PageImpl<>(result, pageable, count);
        });
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
        if (toPrepare.isEmpty() && pageable == null)
            return null;
        return (ps, ignored, i) -> {
            if (pageable != null) {
                ps.setFetchSize(pageable.getPageSize());
            }
            for (PrepareStep op : toPrepare) {
                op.prepareStatement(ps);
            }
        };
    }
}
