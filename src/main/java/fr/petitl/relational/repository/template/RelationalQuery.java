package fr.petitl.relational.repository.template;

import java.sql.Connection;
import java.util.stream.Stream;

/**
 *
 */
public class RelationalQuery {
    private final String sql;
    private final RelationalTemplate template;

    public RelationalQuery(String sql, RelationalTemplate template) {
        this.sql = sql;
        this.template = template;
    }
}
