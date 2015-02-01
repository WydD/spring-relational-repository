package fr.petitl.relational.repository.template;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 */
public interface StatementProvider<E extends Statement> {
    public E statement(Connection connection) throws SQLException;
}
