package fr.petitl.relational.repository.fk;

import javax.sql.DataSource;

import fr.petitl.relational.repository.EnableRelationalRepositories;
import fr.petitl.relational.repository.SpringTest;
import fr.petitl.relational.repository.dialect.generic.GenericBeanDialect;
import fr.petitl.relational.repository.fk.repository.EventRepository;
import fr.petitl.relational.repository.fk.repository.LocationRepository;
import fr.petitl.relational.repository.template.RelationalTemplate;
import org.junit.After;
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
@ContextConfiguration(classes = {FkRepositoryTest.BaseConfiguration.class})
public class FkRepositoryTest extends SpringTest {


    private Location rennes;

    @Configuration
    @EnableRelationalRepositories(basePackages = "fr.petitl.relational.repository.fk.repository")
    public static class BaseConfiguration {
        @Bean
        public DataSource dataSource() {
            return new EmbeddedDatabaseBuilder(new ClassRelativeResourceLoader(FkRepositoryTest.class)).
                    setType(EmbeddedDatabaseType.H2).
                    addScript("db-schema.sql").
                    build();
        }

        @Bean
        public RelationalTemplate relationalTemplate(DataSource dataSource) {
            return new RelationalTemplate(dataSource, new GenericBeanDialect());
        }
    }

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Before
    public void testInsert() {
        rennes = locationRepository.save(new Location("Rennes"));
        Assert.assertNotNull(rennes.getId());
        eventRepository.save(new Event("Stunfest", locationRepository.fk(rennes)));
    }

    @After
    public void after() {
        rennes = null;
        eventRepository.deleteAll();
        locationRepository.deleteAll();
    }

    @Test
    public void testSimpleCase() {
        Event stunfest = eventRepository.findByName("Stunfest");
        Assert.assertNotNull(stunfest.getId());
        Assert.assertEquals("Stunfest", stunfest.getName());
        Assert.assertNotNull(stunfest.getLocation());
        Assert.assertEquals(rennes.getId(), stunfest.getLocation().getId());
        Assert.assertEquals("Rennes", stunfest.getLocation().resolve().getName());
    }
}
