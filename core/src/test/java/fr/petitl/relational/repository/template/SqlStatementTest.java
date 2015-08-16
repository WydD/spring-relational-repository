package fr.petitl.relational.repository.template;

import java.sql.SQLSyntaxErrorException;
import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SqlStatementTest {

    @Test
    public void testSimpleWithout() throws SQLSyntaxErrorException {
        SqlStatement query = SqlStatement.parse("SELECT * FROM Test");
        assertEquals(SqlStatement.ParameterType.NONE, query.getType());
        assertEquals(query.getNativeSql(), query.getOriginal());
        query = SqlStatement.parse("SELECT * FROM Test WHERE id = '2\'3'");
        assertEquals(SqlStatement.ParameterType.NONE, query.getType());
        assertEquals(query.getNativeSql(), query.getOriginal());
    }

    @Test
    public void testSimpleWithNamedParameters() throws SQLSyntaxErrorException {
        SqlStatement query = SqlStatement.parse("SELECT * FROM Test WHERE id = :id AND a = '\\':' AND enabled = :enabled OR ready = :enabled");
        assertEquals(SqlStatement.ParameterType.NAMED_PARAMETER, query.getType());
        assertEquals(Arrays.asList(1), query.resolve("id"));
        assertEquals(Arrays.asList(2, 3), query.resolve("enabled"));
        try {
            query.resolve(1);
            assert false;
        } catch (IllegalStateException ignored) {

        }
        try {
            query.resolve("qwe");
            assert false;
        } catch (IllegalArgumentException ignored) {

        }
        assertEquals("SELECT * FROM Test WHERE id = ? AND a = '\\':' AND enabled = ? OR ready = ?", query.getNativeSql());
    }

    @Test
    public void testSimpleWithQuestionMarks() throws SQLSyntaxErrorException {
        SqlStatement query = SqlStatement.parse("SELECT * FROM Test WHERE id = ? AND a = '\\':' AND enabled = ?");
        assertEquals(SqlStatement.ParameterType.QUESTION_MARKS, query.getType());
        assertEquals(Arrays.asList(1), query.resolve(1));
        assertEquals(Arrays.asList(2), query.resolve(2));
        try {
            query.resolve(3);
            assert false;
        } catch (IllegalArgumentException ignored) {

        }
        assertEquals(query.getNativeSql(), query.getOriginal());
    }

    @Test
    public void testSimpleWithPosition() throws SQLSyntaxErrorException {
        SqlStatement query = SqlStatement.parse("SELECT * FROM Test WHERE id = ?0 AND a = '\\':' AND enabled = ?1 OR ready = ?1");
        assertEquals(SqlStatement.ParameterType.POSITIONAL, query.getType());
        assertEquals(Arrays.asList(1), query.resolve(0));
        assertEquals(Arrays.asList(2,3), query.resolve(1));
        try {
            query.resolve(3);
            assert false;
        } catch (IllegalArgumentException ignored) {

        }
        assertEquals("SELECT * FROM Test WHERE id = ? AND a = '\\':' AND enabled = ? OR ready = ?", query.getNativeSql());
    }

    @Test
    public void testMixingTypes() throws SQLSyntaxErrorException {
        testException("SELECT * FROM Test WHERE id = ? AND a = '\\':' AND enabled = ?0");
        testException("SELECT * FROM Test WHERE id = ?0 AND a = '\\':' AND enabled = :enabled");
        testException("SELECT * FROM Test WHERE id = :id AND a = '\\':' AND enabled = ?");
        testException("SELECT * FROM Test WHERE id = :id AND a = '\\':' AND enabled = ?0");
    }

    private void testException(String sql) {
        try {
            SqlStatement.parse(sql);
            assert false;
        } catch (SQLSyntaxErrorException ignored) {
            //
        }
    }


    @Test
    public void testExtractSeparator() {
        String sql = "WHERE id = :id AND a = 1";
        assertEquals("id", SqlStatement.extract(sql.indexOf(':') + 1, sql.length(), sql));
        sql = "WHERE id = :id";
        assertEquals("id", SqlStatement.extract(sql.indexOf(':') + 1, sql.length(), sql));
    }
}