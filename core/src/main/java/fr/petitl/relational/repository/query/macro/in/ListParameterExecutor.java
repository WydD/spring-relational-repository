package fr.petitl.relational.repository.query.macro.in;

import fr.petitl.relational.repository.query.parametered.ParameteredQueryPart;
import fr.petitl.relational.repository.template.ColumnMapper;

import java.lang.reflect.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by loic on 13/05/2016.
 * Copyright (C) by Data Publica, All Rights Reserved.
 */
public abstract class ListParameterExecutor implements ParameteredQueryPart {
    protected final int[] parameters;
    protected Set<Object> toSet;
    protected Function<Object, ColumnMapper> defaultSetter;

    public ListParameterExecutor(int[] parameters) {
        this.parameters = parameters;
    }

    @Override
    public void setParameter(int parameterNumber, Object parameter, Function<Object, ColumnMapper> defaultSetter) {
        if (parameterNumber != parameters[0]) {
            throw new IllegalArgumentException("Trying to set a parameter number that is configured to be this one");
        }
        this.defaultSetter = defaultSetter;
        if (parameter instanceof Set) {
            toSet = (Set) parameter;
        } else if (parameter instanceof Collection) {
            toSet = new LinkedHashSet<>((Collection) parameter);
        } else if (parameter.getClass().isArray()) {
            int length = Array.getLength(parameter);
            toSet = new HashSet<>(length);
            for (int i = 0; i < length; i++) {
                toSet.add(Array.get(parameter, i));
            }
        } else if (parameter instanceof Stream) {
            Stream<?> asStream = (Stream) parameter;
            toSet = asStream.collect(Collectors.toCollection(LinkedHashSet::new));
            asStream.close();
        } else {
            throw new IllegalArgumentException("Unknown parameter class as parameter of the 'IN' macro");
        }
    }

    @Override
    public void setParameter(int parameterNumber, ColumnMapper mapper) {
        throw new IllegalStateException("Impossible to call setParameter with a straight ColumnMapper");
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public int[] getRequiredParameters() {
        return parameters;
    }

}
