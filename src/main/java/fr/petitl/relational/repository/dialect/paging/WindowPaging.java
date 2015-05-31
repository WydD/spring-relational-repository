package fr.petitl.relational.repository.dialect.paging;

import org.springframework.data.domain.Pageable;

/**
 *
 */
public class WindowPaging extends AbstractOrderByClause {
    private String function;

    private WindowPaging(String function) {
        this.function = function;
    }

    @Override
    public String paging(String sql, Pageable pageable) {
        if (pageable.getSort() == null) {
            throw new IllegalArgumentException("Cannot sort via window paging without providing a sort clause");
        }
        function = "ROW_NUMBER";
        return "SELECT * FROM (SELECT " + function + "() OVER (" + sortClause(pageable.getSort()) + ") AS __ROW_NUMBER, ORIGQ.* FROM (" + sql + ") ORIGQ) AS SUBQ WHERE __ROW_NUMBER <= " + (pageable.getOffset() + pageable.getPageSize()) + " AND __ROW_NUMBER > " + pageable.getOffset();
    }

    public static class Rank extends WindowPaging {
        public Rank() {
            super("RANK");
        }
    }

    public static class RowNumber extends WindowPaging {
        public RowNumber() {
            super("ROW_NUMBER");
        }
    }
}
