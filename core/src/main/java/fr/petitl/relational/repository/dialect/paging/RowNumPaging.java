package fr.petitl.relational.repository.dialect.paging;

import org.springframework.data.domain.Pageable;

/**
 *
 */
public class RowNumPaging extends AbstractOrderByClause {

    @Override
    public String paging(String sql, Pageable pageable) {
        String sortedSQL = sort(sql, pageable.getSort());
        return "SELECT * FROM (SELECT ROWNUM __ROWNUM, SUBQ.* FROM (" + sortedSQL + ") SUBQ WHERE __ROWNUM <= " + (pageable.getOffset() + pageable.getPageSize()) + ") WHERE __ROWNUM > " + pageable.getOffset();
    }
}
