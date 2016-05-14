package fr.petitl.relational.repository.dialect.generic;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import fr.petitl.relational.repository.dialect.BeanSQLGeneration;
import fr.petitl.relational.repository.support.RelationalEntityInformation;
import fr.petitl.relational.repository.template.bean.BeanMappingData;
import fr.petitl.relational.repository.template.bean.FieldMappingData;

import static fr.petitl.relational.repository.util.SqlStringUtil.questionMarks;
import static java.lang.String.format;

/**
 * Inspired by the SqlGenerator pattern of nurkiewicz (but is way different now)
 * https://github.com/nurkiewicz/spring-data-jdbc-repository/blob/master/src/main/java/com/nurkiewicz/jdbcrepository/sql/SqlGenerator.java
 */
public class StandardSQLGeneration<T, ID extends Serializable> implements BeanSQLGeneration {
    protected final String update;
    protected final String insertInto;
    protected final String tableName;

    protected final BeanMappingData<T> mappingData;
    protected final Set<FieldMappingData> pkFields;
    protected final String whereId;
    protected final String fromTable;
    protected final String idColumns;
    protected final String whereIdsIn;

    public static final String SELECT = "SELECT ";
    public static final String FROM = " FROM ";
    public static final String WHERE = " WHERE ";
    public static final String DELETE = "DELETE";
    public static final String SELECT_COUNT = SELECT + "count(*)";
    public static final String SELECT_STAR = SELECT + "*";

    protected final boolean compositeKey;

    protected final String idClause;

    protected final RelationalEntityInformation<T, ID> entityInformation;

    public StandardSQLGeneration(RelationalEntityInformation<T, ID> entityInformation) {
        this.entityInformation = entityInformation;

        mappingData = entityInformation.getMappingData();
        compositeKey = entityInformation.getPkFields().size() > 1;

        tableName = entityInformation.getTableName();

        pkFields = new HashSet<>(entityInformation.getPkFields());

        idClause = entityInformation.getPkFields().stream().map(it -> it.columnName + " = ?").collect(Collectors.joining(" AND "));
        idColumns = entityInformation.getPkFields().stream().map(it -> it.columnName).collect(Collectors.joining(", "));
        whereIdsIn = WHERE + "#in{"+idColumns.replace(',',';')+"; ?}";

        Collection<FieldMappingData> fields = mappingData.getFieldData();
        String updateSet = fields.stream().filter(it -> !pkFields.contains(it)).map(it -> it.columnName + " = ?").collect(Collectors.joining(", "));
        String columnList = fields.stream().map(it -> it.columnName).collect(Collectors.joining(", "));

        whereId = WHERE + idClause;
        fromTable = FROM + tableName;

        insertInto = format("INSERT INTO %s (%s) VALUES (%s)", tableName, columnList, questionMarks(fields.size()));
        update = format("UPDATE %s SET %s %s", tableName, updateSet, whereId);
    }

    @Override
    public String countStar() {
        return SELECT_COUNT + fromTable;
    }

    @Override
    public String deleteById() {
        return DELETE + fromTable + whereId;
    }

    @Override
    public String delete() {
        return DELETE + fromTable;
    }

    @Override
    public String exists() {
        return SELECT_COUNT + fromTable + whereId;
    }

    @Override
    public String selectById() {
        return SELECT_STAR + fromTable + whereId;
    }

    @Override
    public String selectAll() {
        return SELECT_STAR + fromTable;
    }

    @Override
    public String selectIds() {
        return SELECT + idColumns + fromTable;
    }

    @Override
    public String insertInto() {
        return insertInto;
    }

    @Override
    public String update() {
        return update;
    }

    @Override
    public String selectByIds() {
        return SELECT_STAR + fromTable + whereIdsIn;
    }

    @Override
    public String deleteByIds() {
        return DELETE + fromTable + whereIdsIn;
    }
}
