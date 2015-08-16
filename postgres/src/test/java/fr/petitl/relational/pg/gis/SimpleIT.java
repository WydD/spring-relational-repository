package fr.petitl.relational.pg.gis;

import fr.petitl.relational.pg.PGDialectProvider;
import fr.petitl.relational.pg.gis.model.Location;
import fr.petitl.relational.pg.gis.repository.LocationRepository;
import fr.petitl.relational.repository.EnableRelationalRepositories;
import fr.petitl.relational.repository.dialect.SimpleDialectProvider;
import fr.petitl.relational.repository.template.RelationalQuery;
import fr.petitl.relational.repository.template.RelationalTemplate;
import org.junit.Test;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.postgis.MultiPolygon;
import org.postgis.Point;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.sql.DataSource;
import java.sql.SQLException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SimpleIT.BaseConfiguration.class)
public class SimpleIT {
    @Configuration
    @EnableRelationalRepositories(basePackages = "fr.petitl.relational.pg.gis.repository")
    public static class BaseConfiguration {
        @Bean
        public DataSource dataSource() {
            final PGSimpleDataSource pg = new PGSimpleDataSource();
            pg.setServerName("localhost");
            pg.setDatabaseName("test");
            pg.setUser("test");
            pg.setPassword("test");
            return pg;
        }

        @Bean
        public RelationalTemplate relationalTemplate(DataSource dataSource) {
            final RelationalTemplate template = new RelationalTemplate(dataSource, PGDialectProvider.get());
            template.executeUpdate(Location.DROP);
            template.executeUpdate(Location.CREATE);
            template.executeUpdate(Location.INSERT);
            return template;
        }
    }

    @Autowired
    private LocationRepository repository;

    @Test
    public void testFindOne() {
        final Location paris = repository.findOne("75056");
        Assert.assertNotNull(paris);
        Assert.assertEquals("Paris", paris.getLabel());
        Assert.assertEquals(1, paris.getGeometry().numPolygons());
        Assert.assertEquals(2.3, paris.getReference().getX(), 0.1);
        Assert.assertEquals(48.8, paris.getReference().getY(), 0.1);
    }

    @Test
    public void testPut() throws SQLException {
        final Location location = new Location();
        location.setId("26362");
        location.setLabel("Valence");
        location.setGeometry(new MultiPolygon("SRID=4326;MULTIPOLYGON(((4.91045129073418 44.8897182634282,4.88212735355442 44.8875811359426,4.87800104017468 44.8937480545813,4.85432388430886 44.896523451539,4.8575755845501 44.9028903759041,4.86863373983227 44.9096035203311,4.87108518317352 44.9162267340334,4.88658929984345 44.9366516305336,4.90027909819044 44.9395165549132,4.92598965171413 44.9548620604216,4.94002998709083 44.9563021472557,4.94729589105312 44.9529508520132,4.96453725113903 44.9588619650027,4.97197430680502 44.9593247433277,4.97601303869962 44.9533448102038,4.97585558791352 44.9384419742966,4.95281675471961 44.9395786421841,4.94728216309918 44.9342989655278,4.94716180377916 44.9275463461437,4.93771037174086 44.9203894571615,4.93367059110535 44.9138852840961,4.93718238818786 44.8999635511301,4.92823757720566 44.9007406131483,4.91621480706223 44.8956260450858,4.91045129073418 44.8897182634282)))"));
        location.setReference(new Point("SRID=4326;POINT(4.91444013135654 44.9229811667211)"));
        repository.save(location);

        final Location valence = repository.findAt(4.961016, 44.947777);
        Assert.assertNull(valence);
    }

    @Test
    public void testFindAt() {
        final Location paris = repository.findAt(2.377421, 48.861208);
        Assert.assertNotNull(paris);
        final Location notParis = repository.findAt(2.377421, 46.861208);
        Assert.assertNull(notParis);
    }
}
