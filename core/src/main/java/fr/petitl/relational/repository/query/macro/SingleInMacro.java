package fr.petitl.relational.repository.query.macro;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.petitl.relational.repository.query.parametered.ParameteredQueryPart;
import fr.petitl.relational.repository.query.parametered.SingleParameterQueryPart;
import fr.petitl.relational.repository.query.parametered.StringQueryPart;
import fr.petitl.relational.repository.template.ColumnMapper;
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
    public ParameteredQueryPart build(List<List<ParameteredQueryPart>> arguments) throws SQLSyntaxErrorException {
        assert arguments.size() == 2;
        ParameteredQueryPart attributeName = arguments.get(0).get(0);
        ParameteredQueryPart parameter = arguments.get(1).get(0);
        if (!(parameter instanceof SingleParameterQueryPart) || !(attributeName instanceof StringQueryPart)) {
            throw new SQLSyntaxErrorException("Invalid context while executing macro 'IN' which needs {attributeName}{parameter}");
        }
        return new Executor(attributeName.getFragment(), parameter.getRequiredParameters());
    }

    public static class Executor implements ParameteredQueryPart {
        private final String attribute;
        private final int[] parameters;
        private Collection<?> toSet;
        private Function<Object, ColumnMapper> defaultSetter;

        public Executor(String attribute, int[] parameters) {
            this.attribute = attribute;
            this.parameters = parameters;
        }

        @Override
        public void setParameter(int parameterNumber, Object parameter, Function<Object, ColumnMapper> defaultSetter) {
            this.defaultSetter = defaultSetter;
            if (parameter instanceof Collection) {
                toSet = (Collection) parameter;
            } else if (parameter.getClass().isArray()) {
                // TODO
                throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
            } else if (parameter instanceof Stream) {
                Stream<?> asStream = (Stream) parameter;
                toSet = asStream.collect(Collectors.toList());
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
        public int prepare(PreparedStatement ps, int offset) throws SQLException {
            if (toSet.isEmpty()) return offset;

            for (Object element : toSet) {
                defaultSetter.apply(element).prepareColumn(ps, offset++);
            }

            return offset;
        }

        @Override
        public boolean isStatic() {
            return false;
        }

        @Override
        public int[] getRequiredParameters() {
            return parameters;
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
