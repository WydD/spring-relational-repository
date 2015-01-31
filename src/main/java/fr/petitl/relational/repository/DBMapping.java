package fr.petitl.relational.repository;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 */
public interface DBMapping {
    public default Object fromDB(ResultSet rs, int column, Field targetField) throws SQLException {
        return rs.getObject(column);
    }

    public default Object toDB(Object o, Field sourceField) {
        return o;
    }

    public static class Default implements DBMapping {
        public static final DBMapping INSTANCE = new Default();
        private Default() {}
    }
}
