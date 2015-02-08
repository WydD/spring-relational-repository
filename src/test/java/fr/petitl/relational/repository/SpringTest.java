package fr.petitl.relational.repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.runner.RunWith;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class SpringTest {
    private static EmbeddedDatabase current;

    public static EmbeddedDatabase createEmbbededDataSource() {
        if(current != null)
            current.shutdown();
        current = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();
        return current;
    }

    public static DataSource createEmbbededDataSource(String... sql) {
        EmbeddedDatabase ds = createEmbbededDataSource();
        try (Connection connection = ds.getConnection()) {
            Statement statement = connection.createStatement();
            for (String s : sql) {
                statement.execute(s);
            }
            statement.close();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return ds;
    }
}
