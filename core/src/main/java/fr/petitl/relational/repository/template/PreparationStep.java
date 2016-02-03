package fr.petitl.relational.repository.template;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 */
public interface PreparationStep {
    void prepare(PreparedStatement ps) throws SQLException;
}
