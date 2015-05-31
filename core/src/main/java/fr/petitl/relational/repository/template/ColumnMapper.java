package fr.petitl.relational.repository.template;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface ColumnMapper {
    public void prepareColumn(PreparedStatement ps, int parameterIndex) throws SQLException;
}