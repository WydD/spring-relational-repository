package fr.petitl.relational.repository.simple;

import javax.sql.DataSource;

import java.util.Date;

import fr.petitl.relational.repository.EnableRelationalRepositories;
import fr.petitl.relational.repository.SpringTest;
import fr.petitl.relational.repository.template.RelationalTemplate;
import fr.petitl.relational.repository.simple.repository.PojoRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassRelativeResourceLoader;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.ContextConfiguration;

/**
 *
 */
@ContextConfiguration(classes = {SimpleRepositoryTest.BaseConfiguration.class})
public class SimpleRepositoryTest extends SpringTest {


    @Configuration
    @EnableRelationalRepositories(basePackages = "fr.petitl.relational.repository.simple.repository")
    public static class BaseConfiguration {
        @Bean
        public DataSource dataSource() {
            return new EmbeddedDatabaseBuilder(new ClassRelativeResourceLoader(SimpleRepositoryTest.class)).
                    setType(EmbeddedDatabaseType.H2).
                    addScript("db-schema.sql").
                    build();
        }

        @Bean
        public RelationalTemplate relationalTemplate(DataSource dataSource) {
            return new RelationalTemplate(dataSource);
        }
    }

    @Autowired
    private PojoRepository repository;

    @Before
    public void init() {
        repository.deleteAll();

        Pojo pojo = new Pojo("hey", "ho", new Date());
        repository.save(pojo);
    }

    @Test
    public void testFindOne() {
        Pojo hey = repository.findOne("hey");
        Assert.assertEquals(hey.getName(), "ho");
    }

    @Test
    public void testGeneratedMethod() {
        Pojo hey = repository.testGet("ho");
        Assert.assertEquals("hey", hey.getId());
        Pojo hey2 = repository.testGet("oh no!");
        Assert.assertNull(hey2);
    }

    @Test
    public void testGeneratedPositionalMethod() {
        Pojo hey = repository.testGetPositional("ho");
        Assert.assertEquals("hey", hey.getId());
        Pojo hey2 = repository.testGetPositional("oh no!");
        Assert.assertNull(hey2);
    }

    @Test
    public void testCustomImpl() {
        Assert.assertEquals(42, repository.dummy());
    }
}
