package fr.petitl.relational.repository.template;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 */
public interface StatementMapper<E> {
    public void prepare(PreparedStatement pse, E e) throws SQLException;
}
