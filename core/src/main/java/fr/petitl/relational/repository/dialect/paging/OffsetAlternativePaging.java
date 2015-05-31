package fr.petitl.relational.repository.dialect.paging;

import fr.petitl.relational.repository.dialect.PagingGeneration;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 *
 */
public class OffsetAlternativePaging implements PagingGeneration {
    private final PagingGeneration main;
    private final PagingGeneration offset;

    public OffsetAlternativePaging(PagingGeneration main, PagingGeneration offset) {
        this.main = main;
        this.offset = offset;
    }

    @Override
    public String paging(String sql, Pageable pageable) {
        if (pageable.getOffset() > 0) {
            return offset.paging(sql, pageable);
        }
        return main.paging(sql, pageable);
    }

    @Override
    public String sort(String sql, Sort sort) {
        return main.sort(sql, sort);
    }
}
