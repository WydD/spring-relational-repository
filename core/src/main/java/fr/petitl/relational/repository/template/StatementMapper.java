package fr.petitl.relational.repository.template;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 */
public interface StatementMapper<E> {
    public void prepare(PreparedStatement pse, E e, int offset) throws SQLException;

    public default void prepare(PreparedStatement pse, E instance) throws SQLException {
        prepare(pse, instance, 1);
    }
}
