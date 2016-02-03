package fr.petitl.relational.repository.query.parametered;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import fr.petitl.relational.repository.template.ColumnMapper;

/**
 *
 */
public class SingleParameterQueryPart implements ParameteredQueryPart {
    private ColumnMapper mapper;

    private int[] parameters;

    public SingleParameterQueryPart(int parameterNumber) {
        parameters = new int[]{parameterNumber};
    }

    @Override
    public void setParameter(int parameterNumber, ColumnMapper mapper) {
        if (parameterNumber != this.parameters[0]) return;
        this.mapper = mapper;
    }

    @Override
    public int prepare(PreparedStatement ps, int offset) throws SQLException {
        mapper.prepareColumn(ps, offset);
        return offset + 1;
    }

    @Override
    public int[] getRequiredParameters() {
        return parameters;
    }

    @Override
    public String getFragment() {
        return "?";
    }
}
