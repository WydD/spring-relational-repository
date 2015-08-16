package fr.petitl.relational.repository.template.bean;

import java.lang.reflect.Field;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class CamelToSnakeConventionTest {
    private final CamelToSnakeConvention camelToSnakeConvention = new CamelToSnakeConvention();

    public class TestCamelCase {
        public String createdDate;
        public String dataFCOther;
    }

    public String test(Field field) {
        return camelToSnakeConvention.generateDefaultColumnName(field);
    }

    @Test
    public void testCamelCase() throws NoSuchFieldException {
        assertEquals("created_date", test(TestCamelCase.class.getField("createdDate")));
        assertEquals("data_fc_other", test(TestCamelCase.class.getField("dataFCOther")));
    }

    @Test
    public void testCamel() {
        assertEquals("created_date", CamelToSnakeConvention.camelToSnake("createdDate"));
        assertEquals("other_stuff", CamelToSnakeConvention.camelToSnake("OTHERStuff"));
        assertEquals("camel_casing_2_test", CamelToSnakeConvention.camelToSnake("camel_casing_2_test"));
        assertEquals("address_line_2", CamelToSnakeConvention.camelToSnake("addressLine2"));
        assertEquals("bfg_9000", CamelToSnakeConvention.camelToSnake("BFG9000"));
        assertEquals("yeah_", CamelToSnakeConvention.camelToSnake("yeah_"));
        assertEquals("_yeah", CamelToSnakeConvention.camelToSnake("_yeah"));
        assertEquals("bit_of_both_2", CamelToSnakeConvention.camelToSnake("bitOfBoth_2"));
        assertEquals("bit_0_both_2", CamelToSnakeConvention.camelToSnake("bit_0Both_2"));
    }
}