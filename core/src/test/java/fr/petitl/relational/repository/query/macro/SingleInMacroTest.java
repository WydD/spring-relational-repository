package fr.petitl.relational.repository.query.macro;

import fr.petitl.relational.repository.query.macro.SingleInMacro.Executor;
import fr.petitl.relational.repository.query.parametered.SingleParameterQueryPart;
import fr.petitl.relational.repository.query.parametered.StringQueryPart;
import fr.petitl.relational.repository.template.ColumnMapper;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.util.*;
import java.util.function.Function;

import static org.junit.Assert.*;

public class SingleInMacroTest {
    SingleInMacro in = new SingleInMacro();

    @Test
    public void testExecutor() throws SQLException {
        Executor executor = createExecutor("id", 4);
        assertArrayEquals(new int[]{4}, executor.getRequiredParameters());
        assertFalse(executor.isStatic());
        Map<Integer, Integer> settedValues = new HashMap<>();

        List<Integer> ids = Arrays.asList(1, 2, 3, 4, 5);
        Function<Object, ColumnMapper> setter = it -> (ps, offset) -> settedValues.put(offset, (Integer) it);
        try {
            executor.setParameter(2, ids, setter);
            assert false;
        } catch (IllegalArgumentException e) {
            //
        }
        // This time it is accepted
        executor.setParameter(4, ids, setter);

        assertEquals("id IN (?, ?, ?, ?, ?)", executor.getFragment());

        // Prepare the request, check that we have offset + 5
        assertEquals(14, executor.prepare(null, 9));

        assertEquals(Integer.valueOf(1), settedValues.get(9));
        assertEquals(Integer.valueOf(2), settedValues.get(10));
        assertEquals(Integer.valueOf(3), settedValues.get(11));
        assertEquals(Integer.valueOf(4), settedValues.get(12));
        assertEquals(Integer.valueOf(5), settedValues.get(13));
    }

    public void testExecutorWithTypes(Function<List<Integer>, Object> converter) throws SQLException {
        Executor executor = createExecutor("id", 4);
        Map<Integer, Integer> settedValues = new HashMap<>();
        List<Integer> ids = Arrays.asList(1, 2, 3, 4, 5);
        Function<Object, ColumnMapper> setter = it -> (ps, offset) -> settedValues.put(offset, (Integer) it);
        executor.setParameter(4, converter.apply(ids), setter);

        assertEquals("id IN (?, ?, ?, ?, ?)", executor.getFragment());

        // Prepare the request, check that we have offset + 5
        assertEquals(14, executor.prepare(null, 9));

        assertEquals(Integer.valueOf(1), settedValues.get(9));
        assertEquals(Integer.valueOf(2), settedValues.get(10));
        assertEquals(Integer.valueOf(3), settedValues.get(11));
        assertEquals(Integer.valueOf(4), settedValues.get(12));
        assertEquals(Integer.valueOf(5), settedValues.get(13));
    }


    @Test
    public void testExecutorWithTypes() throws SQLException {
        testExecutorWithTypes(Collection::stream);
        testExecutorWithTypes(List::toArray);
        testExecutorWithTypes(ids -> ids.toArray(new Integer[ids.size()]));
        testExecutorWithTypes(ids -> new int[]{1,2,3,4,5});
    }

    private Executor createExecutor(String attribute, int parameterNumber) throws SQLSyntaxErrorException {
        return (Executor) in.build(Arrays.asList(Collections.singletonList(new StringQueryPart(attribute)), Collections.singletonList(new SingleParameterQueryPart(parameterNumber))), null);
    }
}