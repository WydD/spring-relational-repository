package fr.petitl.relational.pg.macro;

import fr.petitl.relational.repository.query.macro.MacroFunction;
import fr.petitl.relational.repository.query.macro.MacroUtils;
import fr.petitl.relational.repository.query.macro.in.CompositeInMacro;
import fr.petitl.relational.repository.query.parametered.ParameteredQueryPart;
import fr.petitl.relational.repository.query.parametered.SingleParameterQueryPart;
import fr.petitl.relational.repository.template.RelationalTemplate;
import fr.petitl.relational.repository.util.SqlStringUtil;

import java.sql.SQLSyntaxErrorException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
public class CompositeTupleInMacro implements MacroFunction {

    @Override
    public String name() {
        return "in";
    }

    @Override
    public int numberOfParameters() {
        return -1;
    }

    @Override
    public ParameteredQueryPart build(List<List<ParameteredQueryPart>> arguments, RelationalTemplate template) throws SQLSyntaxErrorException {
        List<String> attributes = new ArrayList<>(arguments.size() - 1);
        for (int i = 0; i < arguments.size() - 1; i++) {
            attributes.add(MacroUtils.extractString(arguments, i).getFragment());
        }
        SingleParameterQueryPart parameter = MacroUtils.extractParameter(arguments, arguments.size() - 1);
        return new Executor(attributes, parameter.getRequiredParameters(), template);
    }

    public static class Executor extends CompositeInMacro.Executor {
        public Executor(List<String> attributes, int[] parameters, RelationalTemplate template) throws SQLSyntaxErrorException {
            super(attributes, parameters, template);
        }

        @Override
        public String getFragment() {
            if (toSet.isEmpty()) {
                return "1=1";
            }
            return "(" + attributes.stream().collect(Collectors.joining(", ")) + ")" + " IN (" +
                    SqlStringUtil.joinRepetitive(toSet.size(), "(" + SqlStringUtil.questionMarks(attributeSize) + ")", ", ") + ")";
        }
    }
}
