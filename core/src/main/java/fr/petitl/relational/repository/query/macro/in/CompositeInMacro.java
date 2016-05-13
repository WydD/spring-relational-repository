package fr.petitl.relational.repository.query.macro.in;

import fr.petitl.relational.repository.query.macro.MacroFunction;
import fr.petitl.relational.repository.query.macro.MacroUtils;
import fr.petitl.relational.repository.query.parametered.ParameteredQueryPart;
import fr.petitl.relational.repository.query.parametered.SingleParameterQueryPart;
import fr.petitl.relational.repository.template.ColumnMapper;
import fr.petitl.relational.repository.template.RelationalTemplate;
import fr.petitl.relational.repository.template.bean.BeanMappingData;
import fr.petitl.relational.repository.template.bean.FieldMappingData;
import fr.petitl.relational.repository.util.SqlStringUtil;

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

/**
 *
 */
public class CompositeInMacro implements MacroFunction {

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

    public static class Executor extends ListParameterExecutor {
        private final List<String> attributes;
        private final List<String> targetAttributes;
        private final RelationalTemplate template;
        private int attributeSize;

        public Executor(List<String> attributes, int[] parameters, RelationalTemplate template) throws SQLSyntaxErrorException {
            super(parameters);
            this.template = template;
            this.attributeSize = attributes.size();
            this.targetAttributes = new ArrayList<>(attributeSize);
            this.attributes = new ArrayList<>(attributeSize);
            for (String attribute : attributes) {
                String[] split = attribute.split("\\s*=\\s*");
                String target;
                if (split.length == 1) {
                    target = attribute;
                } else if (split.length == 2) {
                    attribute = split[0];
                    target = split[1];
                } else {
                    throw new SQLSyntaxErrorException("Invalid format for attribute '"+attribute+"', requested {attribute}[={targetAttribute}]");
                }
                this.attributes.add(attribute);
                targetAttributes.add(target);
            }
        }

        @Override
        public int prepare(PreparedStatement ps, int offset) throws SQLException {
            if (toSet.isEmpty()) return offset;

            for (Object element : toSet) {
                Class<?> clazz = element.getClass();
                if (clazz.isArray()) {
                    // In case of List< Object[] > like structures
                    int length = Array.getLength(element);
                    if (length != attributeSize) {
                        throw new IllegalArgumentException("Given "+length+" parameters in the tuple for the composite IN macro where "+attributeSize+" was expected");
                    }
                    for (int i = 0; i < length; i++) {
                        defaultSetter.apply(Array.get(element, i)).prepareColumn(ps, offset++);
                    }
                } else {
                    // In case of List< Bean > like structures
                    BeanMappingData<?> mappingData = template.getMappingData(clazz);
                    for (String targetAttribute : targetAttributes) {
                        FieldMappingData field = mappingData.fieldForColumn(targetAttribute);
                        field.attributeWriter.writeAttribute(ps, offset++, field.readMethod.apply(element), field.field);
                    }
                }
            }

            return offset;
        }

        public List<String> getAttributes() {
            return attributes;
        }

        public List<String> getTargetAttributes() {
            return targetAttributes;
        }

        @Override
        public String getFragment() {
            if (toSet.isEmpty()) {
                return "1=1";
            }
            String attributes = this.attributes.stream().map(it -> it + " = ?").collect(Collectors.joining(" AND "));
            if (toSet.size() == 1) {
                return attributes;
            }
            return "("+SqlStringUtil.joinRepetitive(toSet.size(), "("+attributes+")", " OR ")+")";
        }
    }
}
