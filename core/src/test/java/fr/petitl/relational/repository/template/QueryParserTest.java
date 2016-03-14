package fr.petitl.relational.repository.template;

import java.sql.SQLSyntaxErrorException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import fr.petitl.relational.repository.query.macro.MacroFunction;
import fr.petitl.relational.repository.query.macro.SingleInMacro;
import fr.petitl.relational.repository.query.parametered.ParameteredQueryPart;
import fr.petitl.relational.repository.query.parametered.SingleParameterQueryPart;
import fr.petitl.relational.repository.query.parametered.StringQueryPart;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class QueryParserTest {

    @Test
    public void testSimpleWithout() throws SQLSyntaxErrorException {
        QueryParser query = new QueryParser("SELECT * FROM Test");
        assertEquals(QueryParser.ParameterType.NONE, query.getParameterType());
        List<ParameteredQueryPart> parts = query.getQueryParts();

        assertTrue(parts.get(0) instanceof StringQueryPart);
        assertEquals("SELECT * FROM Test", parts.get(0).getFragment());
    }

    @Test
    public void testSimpleWithNamedParameters() throws SQLSyntaxErrorException {
        QueryParser query = new QueryParser("SELECT * FROM Test WHERE id = :id AND a = '\\':''' AND enabled = :enabled OR ready = :enabled");
        assertEquals(QueryParser.ParameterType.NAMED_PARAMETER, query.getParameterType());
        List<ParameteredQueryPart> parts = query.getQueryParts();
        assertTrue(parts.get(0) instanceof StringQueryPart);
        assertEquals("SELECT * FROM Test WHERE id = ", parts.get(0).getFragment());

        assertTrue(parts.get(1) instanceof SingleParameterQueryPart);
        assertEquals("?", parts.get(1).getFragment());
        assertArrayEquals(new int[]{query.getNamedParameterIndex().get("id")}, parts.get(1).getRequiredParameters());

        assertTrue(parts.get(2) instanceof StringQueryPart);
        assertEquals(" AND a = '\\':''' AND enabled = ", parts.get(2).getFragment());

        assertTrue(parts.get(3) instanceof SingleParameterQueryPart);
        assertEquals("?", parts.get(3).getFragment());
        assertArrayEquals(new int[]{query.getNamedParameterIndex().get("enabled")}, parts.get(3).getRequiredParameters());

        assertTrue(parts.get(4) instanceof StringQueryPart);
        assertEquals(" OR ready = ", parts.get(4).getFragment());

        assertTrue(parts.get(5) instanceof SingleParameterQueryPart);
        assertEquals("?", parts.get(5).getFragment());
        assertArrayEquals(new int[]{query.getNamedParameterIndex().get("enabled")}, parts.get(5).getRequiredParameters());
        assertEquals(2, query.getNamedParameterIndex().size());
    }

    @Test
    public void testSimpleWithQuestionMarks() throws SQLSyntaxErrorException {
        QueryParser query = new QueryParser("SELECT * FROM Test WHERE id = ? AND a = '\\':' AND enabled = ?");
        assertEquals(QueryParser.ParameterType.QUESTION_MARKS, query.getParameterType());
        List<ParameteredQueryPart> parts = query.getQueryParts();
        assertTrue(parts.get(0) instanceof StringQueryPart);
        assertEquals("SELECT * FROM Test WHERE id = ", parts.get(0).getFragment());

        assertTrue(parts.get(1) instanceof SingleParameterQueryPart);
        assertEquals("?", parts.get(1).getFragment());
        assertArrayEquals(new int[]{0}, parts.get(1).getRequiredParameters());

        assertTrue(parts.get(2) instanceof StringQueryPart);
        assertEquals(" AND a = '\\':' AND enabled = ", parts.get(2).getFragment());

        assertTrue(parts.get(3) instanceof SingleParameterQueryPart);
        assertEquals("?", parts.get(3).getFragment());
        assertArrayEquals(new int[]{1}, parts.get(3).getRequiredParameters());
    }

    @Test
    public void testSimpleWithPosition() throws SQLSyntaxErrorException {
        QueryParser query = new QueryParser("SELECT * FROM Test WHERE id = ?0 AND a = '\\':''' AND enabled = ?1 OR ready = ?1");
        assertEquals(QueryParser.ParameterType.POSITIONAL, query.getParameterType());
        List<ParameteredQueryPart> parts = query.getQueryParts();
        assertTrue(parts.get(0) instanceof StringQueryPart);
        assertEquals("SELECT * FROM Test WHERE id = ", parts.get(0).getFragment());

        assertTrue(parts.get(1) instanceof SingleParameterQueryPart);
        assertEquals("?", parts.get(1).getFragment());
        assertArrayEquals(new int[]{0}, parts.get(1).getRequiredParameters());

        assertTrue(parts.get(2) instanceof StringQueryPart);
        assertEquals(" AND a = '\\':''' AND enabled = ", parts.get(2).getFragment());

        assertTrue(parts.get(3) instanceof SingleParameterQueryPart);
        assertEquals("?", parts.get(3).getFragment());
        assertArrayEquals(new int[]{1}, parts.get(3).getRequiredParameters());

        assertTrue(parts.get(4) instanceof StringQueryPart);
        assertEquals(" OR ready = ", parts.get(4).getFragment());

        assertTrue(parts.get(5) instanceof SingleParameterQueryPart);
        assertEquals("?", parts.get(5).getFragment());
        assertArrayEquals(new int[]{1}, parts.get(5).getRequiredParameters());
    }


    @Test
    public void testMacro() throws SQLSyntaxErrorException {
        QueryParser query = new QueryParser("SELECT * FROM Test WHERE #in{ id ; ? }", Collections.singletonList(new SingleInMacro()));
        assertEquals(QueryParser.ParameterType.QUESTION_MARKS, query.getParameterType());
        List<ParameteredQueryPart> parts = query.getQueryParts();
        assertTrue(parts.get(0) instanceof StringQueryPart);
        assertEquals("SELECT * FROM Test WHERE ", parts.get(0).getFragment());

        assertTrue(parts.get(1) instanceof SingleInMacro.Executor);
        assertArrayEquals(new int[]{0}, parts.get(1).getRequiredParameters());

        SingleInMacro.Executor macroPart = (SingleInMacro.Executor) parts.get(1);
        assertEquals("id", macroPart.getAttribute());

        assertEquals(2, parts.size());
    }

    @Test
    public void testBadMacro() throws SQLSyntaxErrorException {
        SingleInMacro inMacro = new SingleInMacro();
        testException("SELECT * FROM Test WHERE #in{ id ; ? }");
        testException("SELECT * FROM Test WHERE #in { id ; s? }", inMacro);
        testException("SELECT * FROM Test WHERE #in { id ; s?", inMacro);
        testException("SELECT * FROM Test WHERE #in { ? ; s}", inMacro);
        testException("SELECT * FROM Test WHERE #in { id , ? }", inMacro);
    }

    @Test
    public void testMixingTypes() throws SQLSyntaxErrorException {
        testException("SELECT * FROM Test WHERE id = ? AND a = '\\':' AND enabled = ?0");
        testException("SELECT * FROM Test WHERE id = ?0 AND a = '\\':' AND enabled = :enabled");
        testException("SELECT * FROM Test WHERE id = :id AND a = '\\':' AND enabled = ?");
        testException("SELECT * FROM Test WHERE id = :id AND a = '\\':' AND enabled = ?0");
        testException("SELECT * FROM Test WHERE id = ? AND a = ': AND enabled = ?");
        testException("SELECT * FROM Test WHERE id = ? AND a = '\\");
    }

    private void testException(String sql, MacroFunction... macros) {
        try {
            new QueryParser(sql, Arrays.asList(macros));
            assert false;
        } catch (SQLSyntaxErrorException ignored) {
            //
        }
    }
}