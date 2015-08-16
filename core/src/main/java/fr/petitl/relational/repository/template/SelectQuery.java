package fr.petitl.relational.repository.template;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 */
public class SelectQuery<E> extends AbstractQuery<SelectQuery<E>> {
    protected RowMapper<E> mapper;
    protected Pageable pageable;

    public SelectQuery(String sql, RelationalTemplate template, RowMapper<E> mapper) {
        super(sql, template);
        this.mapper = mapper;
    }

    public AbstractQuery setPageable(Pageable pageable) {
        if (!preparing) {
            clearParameters();
            preparing = true;
        }
        this.pageable = pageable;
        return this;
    }

    protected Stream<E> stream() {
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

    @Override
    public void clearParameters() {
        super.clearParameters();
        this.pageable = null;
    }

    protected StatementMapper<Object> getPrepareStatement() {
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
