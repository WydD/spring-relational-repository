package fr.petitl.relational.repository.fk;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.petitl.relational.repository.EnableRelationalRepositories;
import fr.petitl.relational.repository.SpringTest;
import fr.petitl.relational.repository.dialect.SimpleDialectProvider;
import fr.petitl.relational.repository.fk.domain.Country;
import fr.petitl.relational.repository.fk.domain.Event;
import fr.petitl.relational.repository.fk.domain.Location;
import fr.petitl.relational.repository.fk.repository.CountryRepository;
import fr.petitl.relational.repository.fk.repository.EventRepository;
import fr.petitl.relational.repository.fk.repository.LocationRepository;
import fr.petitl.relational.repository.repository.FKResolver;
import fr.petitl.relational.repository.template.TemplateWithCounter;
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

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private TemplateWithCounter template;

    private FKResolver<Event, EventDTO> eventResolver;
    private Location paris;

    @PostConstruct
    private void init() {
        eventResolver = FKResolver.of(EventDTO::new)
                .add(it -> it.getLocationId() != null ? new Object[]{it.getCountryId(), it.getLocationId()} : null,
                        locationRepository, (dto, location) -> dto.location = new LocationDTO(location))
                .add(Event::getCountryId, countryRepository, (dto, country) -> dto.location.country = country)
                .build();
    }

    @Before
    public void before() {
        template.getTransactionTemplate().execute(tx -> {
            template.executeUpdate("SET REFERENTIAL_INTEGRITY FALSE"); // No deferred check on H2
            rennes = locationRepository.save(new Location("FR", "Rennes"));
            paris = locationRepository.save(new Location("FR", "Paris"));
            Assert.assertNotNull(rennes.getId());
            Assert.assertNotNull(paris.getId());
            // Rennes is now a valid location
            countryRepository.save(new Country("FR", "France", paris.getId()));
            return null;
        });

        final Event stunfest = new Event("Stunfest", rennes);
        // and now we save
        eventRepository.save(stunfest);
    }

    @After
    public void after() {
        rennes = null;
        paris = null;
        // Order is important here as event references location
        eventRepository.deleteAll();
        countryRepository.deleteAll();
        locationRepository.deleteAll();
    }

    @Test
    public void testSimpleResolve() {
        long counter = template.getQueryCounter();
        Map<String, Country> countries = countryRepository.resolveFK(Stream.of(rennes, paris), Location::getCountryId);
        Assert.assertEquals(counter+1, template.getQueryCounter());
        Assert.assertEquals(1, countries.size());
        Assert.assertEquals("France", countries.get("FR").getName());

        counter = template.getQueryCounter();
        countries = countryRepository.resolveFK(Stream.empty(), Location::getCountryId);
        // No query must be made if nothing has to be found
        Assert.assertEquals(counter, template.getQueryCounter());
        Assert.assertEquals(0, countries.size());

        counter = template.getQueryCounter();
        countries = countryRepository.resolveFK(Stream.of(new Location(null, "Unknown")), Location::getCountryId);
        Assert.assertEquals(counter, template.getQueryCounter());
        Assert.assertEquals(0, countries.size());
    }

    @Test
    public void testComplexResolve() {
        Event stunfest = eventRepository.findByName("Stunfest");
        Assert.assertNotNull(stunfest.getId());
        Assert.assertEquals("Stunfest", stunfest.getName());

        // Event.Location is a reference a Location instance
        Assert.assertNotNull(stunfest.getLocationId());
        // Which has this specific ID
        Assert.assertEquals(rennes.getId(), stunfest.getLocationId());

        long queryCounter = template.getQueryCounter();
        EventDTO resolved = eventResolver.resolve(Stream.of(stunfest)).findAny().get();
        // Two queries must have been issued
        Assert.assertEquals(queryCounter+2, template.getQueryCounter());

        // Entities MUST has been resolved
        Assert.assertNotNull(resolved.location);
        Assert.assertNotNull(resolved.location.country);

        // Now we know
        Assert.assertEquals("Rennes", resolved.location.name);
        Assert.assertEquals("France", resolved.location.country.getName());
        Assert.assertEquals(paris.getId(), resolved.location.country.getCapitalId());

        Event other = new Event("Unknown stuff", null);
        other.setId(123);
        // we don't need to store the data to resolve it, that's the beauty :)

        queryCounter = template.getQueryCounter();
        resolved = eventResolver.resolve(Stream.of(other)).findAny().get();
        // No queries must have been issued (only null entries)
        Assert.assertEquals(queryCounter, template.getQueryCounter());
        // Must be null
        Assert.assertNull(resolved.location);

        queryCounter = template.getQueryCounter();
        List<EventDTO> all = eventResolver.resolve(Stream.of(stunfest, other, stunfest), 2).collect(Collectors.toList());
        // Two batches of 2 (and each one contains something useful)
        Assert.assertEquals(queryCounter + 4, template.getQueryCounter());
        Assert.assertEquals(3, all.size());
        Assert.assertEquals(stunfest.getId(), all.get(0).id);
        Assert.assertEquals(stunfest.getLocationId(), all.get(0).location.id);
        Assert.assertEquals(other.getId(), all.get(1).id);
        Assert.assertNull(all.get(1).location);
        Assert.assertEquals(stunfest.getId(), all.get(2).id);
        Assert.assertEquals(stunfest.getLocationId(), all.get(2).location.id);
    }

    @Configuration
    @EnableRelationalRepositories(basePackages = "fr.petitl.relational.repository.fk.repository")
    public static class BaseConfiguration {
        @Bean
        public DataSource dataSource() {
            return createEmbbededDataSource(FkRepositoryTest.class, "db-schema.sql");
        }

        @Bean
        public TemplateWithCounter relationalTemplate(DataSource dataSource) {
            TemplateWithCounter template = new TemplateWithCounter(dataSource, SimpleDialectProvider.h2());
            return template;
        }
    }

    private class EventDTO {
        public Integer id;

        public String name;

        public LocationDTO location;

        public EventDTO(Event event) {
            id = event.getId();
            name = event.getName();
        }
    }

    private class LocationDTO {
        public Country country;

        public Integer id;

        public String name;

        public LocationDTO(Location location) {
            id = location.getId();
            name = location.getName();
        }
    }
}
