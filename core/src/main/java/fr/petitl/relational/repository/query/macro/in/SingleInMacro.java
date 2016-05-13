package fr.petitl.relational.repository.query.macro.in;

import java.lang.reflect.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.petitl.relational.repository.query.macro.MacroFunction;
import fr.petitl.relational.repository.query.macro.MacroUtils;
import fr.petitl.relational.repository.query.parametered.ParameteredQueryPart;
import fr.petitl.relational.repository.query.parametered.SingleParameterQueryPart;
import fr.petitl.relational.repository.query.parametered.StringQueryPart;
import fr.petitl.relational.repository.template.ColumnMapper;
import fr.petitl.relational.repository.template.RelationalTemplate;
import fr.petitl.relational.repository.util.SqlStringUtil;

/**
 *
 */
public class SingleInMacro implements MacroFunction {
    @Override
    public String name() {
        return "in";
    }

    @Override
    public int numberOfParameters() {
        return 2;
    }

    @Override
    public ParameteredQueryPart build(List<List<ParameteredQueryPart>> arguments, RelationalTemplate template) throws SQLSyntaxErrorException {
        StringQueryPart attributeName = MacroUtils.extractString(arguments, 0);
        SingleParameterQueryPart parameter = MacroUtils.extractParameter(arguments, 1);
        return new Executor(attributeName.getFragment(), parameter.getRequiredParameters());
    }

    public static class Executor extends ListParameterExecutor {
        private final String attribute;

        public Executor(String attribute, int[] parameters) {
            super(parameters);
            this.attribute = attribute;
        }

        @Override
        public int prepare(PreparedStatement ps, int offset) throws SQLException {
            if (toSet.isEmpty()) return offset;

            for (Object element : toSet) {
                defaultSetter.apply(element).prepareColumn(ps, offset++);
            }

            return offset;
        }

        public String getAttribute() {
            return attribute;
        }

        @Override
        public String getFragment() {
            if (toSet.isEmpty()) {
                return "1=1";
            }

            return attribute + " IN (" + SqlStringUtil.questionMarks(toSet.size()) + ")";
        }
    }
}
