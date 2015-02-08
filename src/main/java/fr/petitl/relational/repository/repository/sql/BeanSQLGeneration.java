package fr.petitl.relational.repository.repository.sql;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import fr.petitl.relational.repository.support.RelationalEntityInformation;
import fr.petitl.relational.repository.template.bean.BeanMappingData;
import fr.petitl.relational.repository.template.bean.FieldMappingData;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static java.lang.String.format;

/**
 * Inspired by the SqlGenerator pattern of nurkiewicz
 * https://github.com/nurkiewicz/spring-data-jdbc-repository/blob/master/src/main/java/com/nurkiewicz/jdbcrepository/sql/SqlGenerator.java
 */
public class BeanSQLGeneration<T, ID extends Serializable> {
    protected final String update;
    protected final String insertInto;
    protected final String tableName;

    protected final BeanMappingData<T> mappingData;
    protected final Set<FieldMappingData> pkFields;
    protected final String whereId;
    protected final String fromTable;
    protected final String idColumns;

    public static final String SELECT = "SELECT ";
    public static final String FROM = " FROM ";
    public static final String WHERE = " WHERE ";
    public static final String DELETE = "DELETE ";
    public static final String SELECT_COUNT = SELECT + "count(*)";
    public static final String SELECT_STAR = SELECT + "*";

    protected final boolean compositeKey;

    protected final String idClause;

    protected final RelationalEntityInformation<T, ID> entityInformation;

    public BeanSQLGeneration(RelationalEntityInformation<T, ID> entityInformation) {
        this.entityInformation = entityInformation;

        mappingData = entityInformation.getMappingData();
        compositeKey = entityInformation.getPkFields().size() > 1;

        tableName = mappingData.getTableAnnotation().value();

        pkFields = new HashSet<>(entityInformation.getPkFields());

        idClause = entityInformation.getPkFields().stream().map(it -> it.columnName + " = ?").collect(Collectors.joining(" AND "));
        idColumns = entityInformation.getPkFields().stream().map(it -> it.columnName).collect(Collectors.joining(", "));

        Collection<FieldMappingData> fields = mappingData.getFieldData();
        String updateSet = fields.stream().filter(it -> !pkFields.contains(it)).map(it -> it.columnName + " = ?").collect(Collectors.joining(", "));
        String columnList = fields.stream().map(it -> it.columnName).collect(Collectors.joining(", "));

        whereId = WHERE + idClause;
        fromTable = FROM + tableName;

        insertInto = format("INSERT INTO %s (%s) VALUES (%s)", tableName, columnList, questionMarks(fields.size()));
        update = format("UPDATE %s SET %s %s", tableName, updateSet, whereId);
    }

    public String countStar() {
        return SELECT_COUNT + fromTable;
    }

    public String deleteById() {
        return DELETE + fromTable + whereId;
    }

    public String delete() {
        return DELETE + fromTable;
    }

    public String exists() {
        return SELECT_COUNT + fromTable + whereId;
    }

    public String selectById() {
        return SELECT_STAR + fromTable + whereId;
    }

    public String selectAll() {
        return SELECT_STAR + fromTable;
    }

    public String selectIds() {
        return SELECT + idColumns + fromTable;
    }

    public String insertInto() {
        return insertInto;
    }

    public String update() {
        return update;
    }

    protected String questionMarks(int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0)
                builder.append(", ");
            builder.append("?");
        }
        return builder.toString();
    }


    protected String simpleIdIn(int count) {
        return idColumns + " IN (" + questionMarks(count) + ")";
    }

    public String selectAll(int count) {
        String sql = SELECT_STAR + fromTable + WHERE;
        if (compositeKey) {
            return sql + simpleIdIn(count);
        } else {
            return sql + compositeIdIn(count);
        }
    }

    private String compositeIdIn(int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0)
                builder.append(" OR ");
            builder.append("(");
            for (Iterator<FieldMappingData> iterator = entityInformation.getPkFields().iterator(); iterator.hasNext(); ) {
                FieldMappingData fieldData = iterator.next();
                builder.append(fieldData.columnName);
                builder.append(" = ?");
                if (iterator.hasNext())
                    builder.append(" AND ");
            }
            builder.append(")");
        }
        return builder.toString();
    }

    public String selectAll(Pageable page) {
        return selectAll(page.getSort()) + limitClause(page);
    }

    public String selectAll(Sort sort) {
        return selectAll() + sortClause(sort);
    }

    protected String sortClause(Sort sort) {
        if (sort == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder(" ORDER BY ");
        for (Iterator<Sort.Order> iterator = sort.iterator(); iterator.hasNext(); ) {
            Sort.Order order = iterator.next();
            builder.append(order.getProperty()).append(" ").append(order.getDirection().name());
            if (iterator.hasNext()) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }

    protected String limitClause(Pageable page) {
        return " LIMIT " + page.getPageNumber() * page.getPageSize() + ", " + page.getPageSize();
    }

}