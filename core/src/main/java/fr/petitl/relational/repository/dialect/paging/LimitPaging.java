package fr.petitl.relational.repository.dialect.paging;

import org.springframework.data.domain.Pageable;

/**
 * Implements
 */
public class LimitPaging extends AbstractOrderByClause {
    @Override
    public String paging(String sql, Pageable pageable) {
        return sort(sql, pageable.getSort()) + limitClause(pageable);
    }

    protected String limitClause(Pageable page) {
        return " LIMIT " + page.getPageSize() + " OFFSET " + page.getOffset();
    }
}
