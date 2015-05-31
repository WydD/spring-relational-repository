package fr.petitl.relational.repository.dialect.generic;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import fr.petitl.relational.repository.support.RelationalEntityInformation;
import fr.petitl.relational.repository.template.bean.FieldMappingData;

/**
 * Extension of the standard to allow composite id matching with the clause (id1, id2, id3) IN ((?,?,?), (?,?,?), ...)
 */
public class CompositeAsTupleSQLGeneration<T, ID extends Serializable> extends StandardSQLGeneration<T, ID> {
    public CompositeAsTupleSQLGeneration(RelationalEntityInformation<T, ID> entityInformation) {
        super(entityInformation);
    }

    @Override
    protected String compositeIdIn(int count) {
        List<FieldMappingData> pkFields = entityInformation.getPkFields();
        StringBuilder builder = new StringBuilder("(");
        String questionMarks = "(";
        for (Iterator<FieldMappingData> iterator = pkFields.iterator(); iterator.hasNext(); ) {
            builder.append(iterator.next().columnName);
            questionMarks += "?";
            if (iterator.hasNext()) {
                builder.append(", ");
                questionMarks += ", ";
            } else {
                builder.append(")");
                questionMarks += ")";
            }
        }

        builder.append(" IN (");
        for (int i = 0; i < count; i++) {
            if (i == 0)
                builder.append(", ");
            builder.append(questionMarks);
        }
        builder.append(")");
        return builder.toString();
    }
}
