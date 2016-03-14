package fr.petitl.relational.repository.query.macro;

import fr.petitl.relational.repository.query.parametered.ParameteredQueryPart;

import java.sql.SQLSyntaxErrorException;
import java.util.List;

/**
 *
 */
public interface MacroFunction {
    String name();

    int numberOfParameters();

    ParameteredQueryPart build(List<List<ParameteredQueryPart>> arguments) throws SQLSyntaxErrorException;
}
