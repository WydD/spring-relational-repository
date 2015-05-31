package fr.petitl.relational.repository.dialect.paging;

import java.util.Iterator;

import fr.petitl.relational.repository.dialect.PagingGeneration;
import org.springframework.data.domain.Sort;

/**
 *
 */
public abstract class AbstractOrderByClause implements PagingGeneration {
    @Override
    public String sort(String sql, Sort sort) {
        if (sort != null) {
            sql += sortClause(sort);
        }
        return sql;
    }

    protected String sortClause(Sort sort) {
        if (sort == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder(" ORDER BY ");
        for (Iterator<Sort.Order> iterator = sort.iterator(); iterator.hasNext(); ) {
            Sort.Order order = iterator.next();
            builder.append(order.getProperty()).append(" ").append(order.getDirection().name());
            if (iterator.hasNext()) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }
}
