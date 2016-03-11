package fr.petitl.relational.repository.query.parametered;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Function;

import fr.petitl.relational.repository.template.ColumnMapper;

/**
 *
 */
public class MacroQueryPart implements ParameteredQueryPart {
    @Override
    public void setParameter(int parameterNumber, Object parameter, Function<Object, ColumnMapper> defaultSetter) {

    }

    @Override
    public void setParameter(int parameterNumber, ColumnMapper mapper) {
        throw new IllegalStateException("Cannot call a straight column mapper when using macros");
    }

    @Override
    public int prepare(PreparedStatement ps, int offset) throws SQLException {
        return 0;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public int[] getRequiredParameters() {
        return new int[0];
    }

    @Override
    public String getFragment() {
        return null;
    }
}
