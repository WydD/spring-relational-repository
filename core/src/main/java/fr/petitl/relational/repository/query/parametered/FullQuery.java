package fr.petitl.relational.repository.query.parametered;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import fr.petitl.relational.repository.template.ColumnMapper;
import fr.petitl.relational.repository.template.PreparationStep;

/**
 *
 */
public class FullQuery implements PreparationStep {
    private List<ParameteredQueryPart> parts;
    private Map<Integer, Collection<ParameteredQueryPart>> parameters = new TreeMap<>();
    private List<ParameteredQueryPart> toSetQueryPart = new LinkedList<>();
    private boolean isStatic = true;
    private Set<Integer> parametersToSet;

    public FullQuery(List<ParameteredQueryPart> parts) {
        this.parts = parts;
        for (ParameteredQueryPart part : parts) {
            if (!part.isStatic()) {
                isStatic = false;
            }
            int[] requiredParameters = part.getRequiredParameters();
            if (requiredParameters == null) {
                continue;
            }
            toSetQueryPart.add(part);
            for (int parameterNumber : requiredParameters) {
                Collection<ParameteredQueryPart> queryPartsForParameterNumber = parameters.get(parameterNumber);
                if (queryPartsForParameterNumber == null) {
                    queryPartsForParameterNumber = new LinkedList<>();
                    parameters.put(parameterNumber, queryPartsForParameterNumber);
                }
                queryPartsForParameterNumber.add(part);
            }
        }
        clear();
    }

    public void setParameter(int parameterNumber, Object parameter, Function<Object, ColumnMapper> defaultSetter) {
        doSetParameter(parameterNumber, part -> part.setParameter(parameterNumber, parameter, defaultSetter));
    }

    public void setParameter(int parameterNumber, ColumnMapper mapper) {
        doSetParameter(parameterNumber, part -> part.setParameter(parameterNumber, mapper));
    }

    protected void doSetParameter(int parameterNumber, Consumer<ParameteredQueryPart> action) {
        Collection<ParameteredQueryPart> parts = parameters.get(parameterNumber);
        if (parts == null) {
            throw new IllegalArgumentException("Setting an argument that is not part of any query parameter [" + parameterNumber + "]");
        }
        if (!parametersToSet.contains(parameterNumber)) {
            throw new IllegalStateException("Parameter number " + parameterNumber + " has already been set");
        }
        parametersToSet.remove(parameterNumber);
        parts.forEach(action);
    }

    public void prepare(PreparedStatement ps) throws SQLException {
        if (!parametersToSet.isEmpty()) {
            throw new IllegalStateException("Preparing phase is unauthorized as some parameters still need to be set " + parametersToSet);
        }
        if (toSetQueryPart.isEmpty()) return;
        int offset = 1;
        for (ParameteredQueryPart part : toSetQueryPart) {
            offset = part.prepare(ps, offset);
        }
    }

    public String getQueryString() {
        if (!parametersToSet.isEmpty()) {
            throw new IllegalStateException("Preparing phase is unauthorized as some parameters still need to be set " + parametersToSet);
        }
        StringBuilder res = new StringBuilder();
        for (ParameteredQueryPart part : parts) {
            res.append(part.getFragment());
        }
        return res.toString();
    }

    public void clear() {
        parametersToSet = new TreeSet<>(parameters.keySet());
    }

    public int getNumberOfArguments() {
        return parameters.size();
    }

    public boolean isStatic() {
        return isStatic;
    }
}