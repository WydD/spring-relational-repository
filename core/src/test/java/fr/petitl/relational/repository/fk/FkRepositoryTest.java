package fr.petitl.relational.repository.fk;

import javax.sql.DataSource;

import fr.petitl.relational.repository.EnableRelationalRepositories;
import fr.petitl.relational.repository.SpringTest;
import fr.petitl.relational.repository.dialect.BeanDialectProvider;
import fr.petitl.relational.repository.fk.domain.Event;
import fr.petitl.relational.repository.fk.domain.Location;
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
import org.springframework.test.context.ContextConfiguration;

/**
 * Provides a simple case of foreign key integration in spring relational repository.
 *
 * See the domain definition as well as the database creation script (db-schema.sql in test resources) and of course
 * how the framework behave in the real world in this class.
 */
@ContextConfiguration(classes = {FkRepositoryTest.BaseConfiguration.class})
public class FkRepositoryTest extends SpringTest {

    private Location rennes;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Before
    public void before() {
        rennes = locationRepository.save(new Location("Rennes"));
        Assert.assertNotNull(rennes.getId());
        // Rennes is now a valid location

        // Create the "stunfest" event which contains a foreign declared using the location repository
        // (which knows the ID type and resolution strategies)
        final Event stunfest = new Event("Stunfest", locationRepository.fk(rennes));
        // locationRepository.fid(rennes.getId()) works as well to create the fk to rennes
        // ... but this one has a resolved entity because it was given by the user
        Assert.assertTrue(stunfest.getLocation().isResolved());

        // and now we save
        eventRepository.save(stunfest);
    }

    @After
    public void after() {
        rennes = null;
        // Order is important here as event references location
        eventRepository.deleteAll();
        locationRepository.deleteAll();
    }

    @Test
    public void testSimpleCase() {
        Event stunfest = eventRepository.findByName("Stunfest");
        Assert.assertNotNull(stunfest.getId());
        Assert.assertEquals("Stunfest", stunfest.getName());

        // Event.Location is a reference a Location instance
        Assert.assertNotNull(stunfest.getLocation());
        // Which has this specific ID
        Assert.assertEquals(rennes.getId(), stunfest.getLocation().getId());
        // But we don't know the content
        Assert.assertFalse(stunfest.getLocation().isResolved());

        // Perform the explicit DB resolve
        final Location resolved = stunfest.getLocation().resolve();

        // Now we know
        Assert.assertEquals("Rennes", resolved.getName());
        Assert.assertTrue(stunfest.getLocation().isResolved());

        // A second resolved is a cache
        Assert.assertTrue(resolved == stunfest.getLocation().resolve());
        // Which is not the case for a forced one
        Assert.assertTrue(resolved != stunfest.getLocation().forceResolve());
    }

    @Configuration
    @EnableRelationalRepositories(basePackages = "fr.petitl.relational.repository.fk.repository")
    public static class BaseConfiguration {
        @Bean
        public DataSource dataSource() {
            return createEmbbededDataSource(FkRepositoryTest.class, "db-schema.sql");
        }

        @Bean
        public RelationalTemplate relationalTemplate(DataSource dataSource) {
            return new RelationalTemplate(dataSource, BeanDialectProvider.h2());
        }
    }

}
