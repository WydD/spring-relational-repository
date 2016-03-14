package fr.petitl.relational.repository.query.macro;

import fr.petitl.relational.repository.query.parametered.ParameteredQueryPart;
import fr.petitl.relational.repository.query.parametered.SingleParameterQueryPart;
import fr.petitl.relational.repository.query.parametered.StringQueryPart;

import java.sql.SQLSyntaxErrorException;
import java.util.List;

/**
 * Created by loic on 14/03/2016.
 * Copyright (C) by Data Publica, All Rights Reserved.
 */
public class MacroUtils {
    public static StringQueryPart extractString(List<List<ParameteredQueryPart>> arguments, int idx) throws SQLSyntaxErrorException {
        List<ParameteredQueryPart> argument = arguments.get(idx);
        if (argument.isEmpty()) {
            throw new SQLSyntaxErrorException("Excepted a simple statement argument for position "+(idx+1)+" got empty argument");
        }
        if (argument.size() != 1) {
            throw new SQLSyntaxErrorException("Excepted a simple statement argument for position "+(idx+1)+" got complex argument");
        }
        ParameteredQueryPart sql = argument.get(0);
        if (!(sql instanceof StringQueryPart)) {
            throw new SQLSyntaxErrorException("Excepted a simple statement argument for position "+(idx+1)+" got "+sql.getClass().getSimpleName());
        }
        return (StringQueryPart) sql;
    }

    public static SingleParameterQueryPart extractParameter(List<List<ParameteredQueryPart>> arguments, int idx) throws SQLSyntaxErrorException {
        List<ParameteredQueryPart> argument = arguments.get(idx);
        if (argument.isEmpty()) {
            throw new SQLSyntaxErrorException("Excepted a parameter argument for position "+(idx+1)+" got empty argument");
        }
        if (argument.size() != 1) {
            throw new SQLSyntaxErrorException("Excepted a parameter argument for position "+(idx+1)+" got complex argument");
        }
        ParameteredQueryPart sql = argument.get(0);
        if (!(sql instanceof SingleParameterQueryPart)) {
            throw new SQLSyntaxErrorException("Excepted a simple statement argument for position "+(idx+1)+" got "+sql.getClass().getSimpleName());
        }
        return (SingleParameterQueryPart) sql;
    }
}
