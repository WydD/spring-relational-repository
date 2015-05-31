package fr.petitl.relational.repository.template;

import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 */
public interface StatementCallback<E extends Statement, T> {
    public T execute(E st) throws SQLException;
}
