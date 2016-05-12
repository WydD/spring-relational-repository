package fr.petitl.relational.repository.query.macro;

import fr.petitl.relational.repository.annotation.Column;
import fr.petitl.relational.repository.dialect.BeanDialect;
import fr.petitl.relational.repository.query.macro.CompositeInMacro.Executor;
import fr.petitl.relational.repository.query.parametered.ParameteredQueryPart;
import fr.petitl.relational.repository.query.parametered.SingleParameterQueryPart;
import fr.petitl.relational.repository.query.parametered.StringQueryPart;
import fr.petitl.relational.repository.template.ColumnMapper;
import fr.petitl.relational.repository.template.RelationalTemplate;
import fr.petitl.relational.repository.template.bean.BeanAttributeWriter;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class CompositeInMacroTest {
    public static class A {
        @Column(writer = Writer.class)
        public String id;
        @Column(writer = Writer.class)
        public int pos;

        public A(String id, int pos) {
            this.id = id;
            this.pos = pos;
        }
    }

    public static class Writer implements BeanAttributeWriter {
        @Override
        public void writeAttribute(PreparedStatement ps, int column, Object o, Field sourceField) throws SQLException {
            settedValues.put(column, o);
        }
    }

    public static Map<Integer, Object> settedValues = new HashMap<>();

    @Before
    public void before() {
        settedValues.clear();
    }

    CompositeInMacro in = new CompositeInMacro();
    RelationalTemplate template = new RelationalTemplate(new DelegatingDataSource(), new BeanDialect(null).addMacro(new SingleInMacro()).addMacro(new CompositeInMacro()));

    @Test
    public void testExecutor() throws SQLException {
        Executor executor = createExecutor(4, "id", "pos");
        assertArrayEquals(new int[]{4}, executor.getRequiredParameters());
        assertFalse(executor.isStatic());

        List<Object[]> ids = Arrays.asList(new Object[]{"hey", 1}, new Object[]{"ho", 2}, new Object[]{"sup", 7});
        Function<Object, ColumnMapper> setter = it -> (ps, offset) -> settedValues.put(offset, it);
        try {
            executor.setParameter(2, ids, setter);
            assert false;
        } catch (IllegalArgumentException e) {
            // bad position
        }

        // This time it is accepted
        executor.setParameter(4, ids, setter);

        assertEquals("((id = ? AND pos = ?) OR (id = ? AND pos = ?) OR (id = ? AND pos = ?))", executor.getFragment());

        // Prepare the request, check that we have offset + 6 (3*2)
        assertEquals(15, executor.prepare(null, 9));

        assertEquals("hey", settedValues.get(9));
        assertEquals(1, settedValues.get(10));
        assertEquals("ho", settedValues.get(11));
        assertEquals(2, settedValues.get(12));
        assertEquals("sup", settedValues.get(13));
        assertEquals(7, settedValues.get(14));
    }

    @Test
    public void testExecutorWithBeanMapping() throws SQLException {
        Executor executor = createExecutor(4, "pos", "id");
        List<A> ids = Arrays.asList(new A("hey", 1), new A("zen", -45));

        executor.setParameter(4, ids, null);

        assertEquals("((pos = ? AND id = ?) OR (pos = ? AND id = ?))", executor.getFragment());

        // Prepare the request, check that we have offset + 5
        assertEquals(9+4, executor.prepare(null, 9));

        assertEquals(1, settedValues.get(9));
        assertEquals("hey", settedValues.get(10));
        assertEquals(-45, settedValues.get(11));
        assertEquals("zen", settedValues.get(12));
    }

    @Test
    public void testExecutorWithBeanMappingAndAliases() throws SQLException {
        Executor executor = createExecutor(4, "position:pos", "ref:id");
        List<A> ids = Arrays.asList(new A("hey", 1), new A("zen", -45));

        executor.setParameter(4, ids, null);

        assertEquals("((position = ? AND ref = ?) OR (position = ? AND ref = ?))", executor.getFragment());

        // Prepare the request, check that we have offset + 5
        assertEquals(9+4, executor.prepare(null, 9));

        assertEquals(1, settedValues.get(9));
        assertEquals("hey", settedValues.get(10));
        assertEquals(-45, settedValues.get(11));
        assertEquals("zen", settedValues.get(12));
    }

/*
    @Test
    public void testExecutorWithTypes() throws SQLException {
        testExecutorWithTypes(Collection::stream);
        testExecutorWithTypes(List::toArray);
        testExecutorWithTypes(ids -> ids.toArray(new Integer[ids.size()]));
        testExecutorWithTypes(ids -> new int[]{1,2,3,4,5});
    }*/

    private Executor createExecutor(int parameterNumber, String... attribute) throws SQLSyntaxErrorException {
        Stream<List<ParameteredQueryPart>> paramAttributes = Arrays.stream(attribute).map(it -> Collections.singletonList(new StringQueryPart(it)));
        Stream<List<ParameteredQueryPart>> allParams = Stream.concat(paramAttributes, Stream.of(Collections.singletonList(new SingleParameterQueryPart(parameterNumber))));
        return (Executor) in.build(allParams.collect(Collectors.toList()), template);
    }
}