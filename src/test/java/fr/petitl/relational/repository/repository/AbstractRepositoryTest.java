package fr.petitl.relational.repository.repository;

import javax.sql.DataSource;
import java.io.Serializable;
import java.util.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;

import fr.petitl.relational.repository.SpringTest;
import fr.petitl.relational.repository.repository.model.MainGenerated;
import fr.petitl.relational.repository.support.RelationalEntityInformation;
import fr.petitl.relational.repository.template.RelationalTemplate;
import fr.petitl.relational.repository.template.bean.MappingFactory;

import static org.junit.Assert.*;

/**
 *
 */
public abstract class AbstractRepositoryTest {

    protected Date dateFrom(String text) {
        return Date.from(LocalDateTime.parse(text).atZone(ZoneId.systemDefault()).toInstant());
    }

    protected <T, ID extends Serializable> SimpleRelationalRepository<T, ID> getRepository(Class<T> clazz, String... sql) {
        DataSource ds = SpringTest.createEmbbededDataSource(sql);
        RelationalTemplate template = new RelationalTemplate(ds);
        return new SimpleRelationalRepository<>(new RelationalEntityInformation<>(MappingFactory.beanMapping(clazz)), template);
    }

    protected void verifyDeleted(SimpleRelationalRepository<MainGenerated, Integer> repository, int count, Integer... missingIds) throws SQLException {
        RelationalTemplate template = repository.getTemplate();
        for (Integer id : missingIds) {
            template.execute((PreparedStatement statement) -> {
                ResultSet rs = statement.executeQuery();
                assertFalse(rs.next());
                return 1;
            }, con -> con.prepareStatement("SELECT * FROM MainGenerated WHERE id = "+id));
        }

        template.execute((PreparedStatement statement) -> {
            ResultSet rs = statement.executeQuery();
            assertTrue(rs.next());
            assertEquals(count, rs.getInt(1));
            return 1;
        }, con -> con.prepareStatement("SELECT count(*) FROM MainGenerated"));
    }
}
