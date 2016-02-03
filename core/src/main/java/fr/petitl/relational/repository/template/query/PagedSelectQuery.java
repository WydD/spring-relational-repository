package fr.petitl.relational.repository.template.query;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.petitl.relational.repository.template.PreparationStep;
import fr.petitl.relational.repository.template.RelationalTemplate;
import fr.petitl.relational.repository.template.RowMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionTemplate;

/**
 *
 */
public class PagedSelectQuery<E> extends AbstractQuery<PagedSelectQuery<E>> {
    private final RowMapper<E> mapper;
    protected Pageable pageable;

    public PagedSelectQuery(String sql, RelationalTemplate template, RowMapper<E> mapper) {
        super(sql, template);
        this.mapper = mapper;
    }

    public PagedSelectQuery<E> setPageable(Pageable pageable) {
        this.pageable = pageable;
        return this;
    }

    public <F> F fetch(Function<Stream<E>, F> collectorFunction) {
        // Get the list of the page
        String nativeSql = template.getDialect().paging().paging(query.getQueryString(), pageable);
        PreparationStep prepare = ps -> {
            ps.setFetchSize(pageable.getPageSize());
            query.prepare(ps);
        };
        return template.executeQuery(nativeSql, mapper, collectorFunction, prepare);
    }

    public Page<E> fetch() {
        if (pageable == null) {
            throw new IllegalStateException("Querying a page without a Pageable");
        }
        TransactionTemplate transactionTemplate = template.getTransactionTemplate();
        return transactionTemplate.execute(status -> {
            List<E> result = fetch(tmp -> tmp.collect(Collectors.toList()));

            // count the result
            String countQuery = "SELECT COUNT(*) FROM (" + query.getQueryString() + ") SUBQ";
            Long count = template.executeQuery(countQuery, rs -> rs.getLong(1), st -> st.findFirst().get(), query);

            return new PageImpl<>(result, pageable, count);
        });
    }

}
