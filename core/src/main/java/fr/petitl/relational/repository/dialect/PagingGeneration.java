package fr.petitl.relational.repository.dialect;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 *
 */
public interface PagingGeneration {
    /**
     * Apply paging constraints to a unpaged sql query.
     * <p>
     * An unpaged sql query is a query without any mention of paging
     * (e.g. an ORDER BY clause or a LIMIT/OFFSET clause)
     *
     * @param sql      An unpaged sql
     * @param pageable Paging constraints
     * @return A paged sql
     * @throws IllegalArgumentException if a paged sql is detected (not guaranteed)
     */
    String paging(String sql, Pageable pageable);

    /**
     * Apply a sort constraint without paging limitation
     *
     * @param sql  An unordered sql query
     * @param sort T
     * @return
     */
    String sort(String sql, Sort sort);
}
