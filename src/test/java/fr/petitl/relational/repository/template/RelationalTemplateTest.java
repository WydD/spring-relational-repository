package fr.petitl.relational.repository.template;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import fr.petitl.relational.repository.SpringTest;
import fr.petitl.relational.repository.dialect.BeanDialectProvider;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;

import static org.junit.Assert.*;

public class RelationalTemplateTest {

    private RelationalTemplate template = new RelationalTemplate(SpringTest.createEmbbededDataSource(), BeanDialectProvider.h2());

    private StatementCallback<Statement, Statement> testStatement = (Statement st) -> {
        assertNotNull(st);
        assertFalse(st.isClosed());
        assertFalse(st.getConnection().isClosed());
        return st;
    };

    private ConnectionCallback<Connection> testConnection = co -> {
        assertNotNull(co);
        assertFalse(co.isClosed());
        return co;
    };

    @Test
    public void testExecute() throws Exception {
        // Test a standard connection
        Connection c = template.execute(testConnection);
        assertTrue(c.isClosed());

        // Test with an exception fired.
        Connection[] returnValue = new Connection[1];
        try {
            template.execute((Connection con) -> {
                assertNotNull(con);
                assertFalse(con.isClosed());
                returnValue[0] = con;
                throw new SQLException("Test");
            });
            assert false;
        } catch (DataAccessException ignored) {
        }
        assertTrue(returnValue[0].isClosed());
    }

    @Test
    public void testExecuteDontClose() throws Exception {
        // Test a standard connection
        Connection c = template.executeDontClose(testConnection);
        assertFalse(c.isClosed());
        template.release(c);

        // Test an sql exception fired.
        Connection[] returnValue = new Connection[1];
        try {
            template.executeDontClose((Connection con) -> {
                returnValue[0] = con;
                throw new SQLException("Test");
            });
            assert false;
        } catch (DataAccessException ignored) {
        }
        assertNotNull(returnValue[0]);
        assertTrue(returnValue[0].isClosed());

        // Test a runtime exception
        try {
            template.executeDontClose((Connection con) -> {
                returnValue[0] = con;
                throw new RuntimeException("Test");
            });
            assert false;
        } catch (RuntimeException ignored) {
        }
        assertNotNull(returnValue[0]);
        assertTrue(returnValue[0].isClosed());
    }

    @Test
    public void testExecuteStatement() throws Exception {
        // Test a standard connection
        Statement c = template.execute(testStatement, Connection::createStatement);
        assertTrue(c.isClosed());
        assertTrue(c.getConnection() == null || c.getConnection().isClosed());

        // Test with an exception fired.
        Statement[] returnValue = new Statement[1];
        try {
            template.execute(st -> {
                returnValue[0] = st;
                throw new SQLException("Test");
            }, Connection::createStatement);
            assert false;
        } catch (DataAccessException ignored) {
        }
        assertTrue(returnValue[0].isClosed());
        assertTrue(returnValue[0].getConnection() == null || returnValue[0].getConnection().isClosed());
    }

    @Test
    public void testExecuteDontCloseStatement() throws Exception {
        // Test a standard connection
        Statement c = template.executeDontClose(testStatement, Connection::createStatement);
        assertFalse(c.isClosed());
        assertFalse(c.getConnection() == null || c.getConnection().isClosed());

        // Test with an exception fired.
        Statement[] returnValue = new Statement[1];
        try {
            template.executeDontClose(st -> {
                returnValue[0] = st;
                throw new SQLException("Test");
            }, Connection::createStatement);
            assert false;
        } catch (DataAccessException ignored) {
        }
        assertTrue(returnValue[0].isClosed());
        assertTrue(returnValue[0].getConnection() == null || returnValue[0].getConnection().isClosed());
    }
}