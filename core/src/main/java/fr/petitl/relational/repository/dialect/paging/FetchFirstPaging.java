package fr.petitl.relational.repository.dialect.paging;

import org.springframework.data.domain.Pageable;

/**
 *
 */
public class FetchFirstPaging extends AbstractOrderByClause {
    private boolean offsetAllowed;

    public FetchFirstPaging(boolean offsetAllowed) {
        this.offsetAllowed = offsetAllowed;
    }

    protected String fetchFirstClause(Pageable page) {
        String fetchFirst = "FETCH FIRST " + page.getPageSize() + " ROWS ONLY";
        if (page.getOffset() == 0) {
            return fetchFirst;
        }
        if (offsetAllowed) {
            return " OFFSET " + page.getOffset() + " ROWS " + fetchFirst;
        }
        throw new UnsupportedOperationException("The offset clause is not allowed on this DBMS.");
    }

    @Override
    public String paging(String sql, Pageable pageable) {
        return sort(sql, pageable.getSort()) + fetchFirstClause(pageable);
    }
}