package fr.petitl.relational.repository;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 */
public interface DBMapping {
    public default Object fromDB(ResultSet rs, int column, Field targetField) throws SQLException {
        return rs.getObject(column);
    }

    public default void toDB(PreparedStatement ps, int column, Object o, Field sourceField) throws SQLException {
        ps.setObject(column, o);
    }

    public static class Default implements DBMapping {
        public static final DBMapping INSTANCE = new Default();
        private Default() {}
    }
}
