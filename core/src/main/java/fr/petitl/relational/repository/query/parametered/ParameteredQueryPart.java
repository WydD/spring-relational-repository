package fr.petitl.relational.repository.query.parametered;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Function;

import fr.petitl.relational.repository.template.ColumnMapper;

/**
 *
 */
public interface ParameteredQueryPart {

    // set nth position
    default void setParameter(int parameterNumber, Object parameter, Function<Object, ColumnMapper> defaultSetter) {
        setParameter(parameterNumber, defaultSetter.apply(parameter));
    }

    // set nth position
    default void setParameter(int parameterNumber, ColumnMapper mapper) {
    }

    default int prepare(PreparedStatement ps, int offset) throws SQLException {
        return offset;
    }

    default boolean isStatic() {
        return true;
    }

    default int[] getRequiredParameters() {
        return null;
    }

    String getFragment();
}
