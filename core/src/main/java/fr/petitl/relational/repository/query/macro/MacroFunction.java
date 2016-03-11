package fr.petitl.relational.repository.query.macro;

import java.sql.SQLSyntaxErrorException;
import java.util.List;

import fr.petitl.relational.repository.query.parametered.ParameteredQueryPart;

/**
 *
 */
public interface MacroFunction {
    String name();

    int numberOfParameters();

    ParameteredQueryPart build(List<List<ParameteredQueryPart>> arguments) throws SQLSyntaxErrorException;
}
