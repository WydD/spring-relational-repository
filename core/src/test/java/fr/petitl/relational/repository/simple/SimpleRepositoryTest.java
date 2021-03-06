package fr.petitl.relational.repository.simple;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import fr.petitl.relational.repository.EnableRelationalRepositories;
import fr.petitl.relational.repository.SpringTest;
import fr.petitl.relational.repository.dialect.SimpleDialectProvider;
import fr.petitl.relational.repository.simple.repository.PojoRepository;
import fr.petitl.relational.repository.template.RelationalTemplate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
            return createEmbbededDataSource(SimpleRepositoryTest.class, "db-schema.sql");
        }

        @Bean
        public RelationalTemplate relationalTemplate(DataSource dataSource) {
            return new RelationalTemplate(dataSource, SimpleDialectProvider.h2());
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
    public void testGeneratedPositionalCollectionMethod() {
        List<Pojo> hey = repository.testGetPositionalList("ho");
        Assert.assertEquals("hey", hey.get(0).getId());
        Assert.assertEquals(1, hey.size());
        List<Pojo> hey2 = repository.testGetPositionalList("oh no!");
        Assert.assertEquals(0, hey2.size());
    }

    @Test
    public void testApplyMethod() {
        List<PojoDTO> pojos = repository.testApplyStream("ho", it -> it.collect(Collectors.toList()));
        Assert.assertEquals(1, pojos.size());
        Assert.assertEquals("hey", pojos.get(0).getId());
    }

    @Test
    public void testPagedMethod() {
        repository.save(new Pojo("hey1", "ho1", new Date()));
        repository.save(new Pojo("hey2", "ho2", new Date()));

        Page<Pojo> pojos = repository.testPaged("%", new PageRequest(0, 2, new Sort(Sort.Direction.DESC, "id")));
        Assert.assertEquals(2, pojos.getTotalPages());
        Assert.assertEquals(3, pojos.getTotalElements());
        Assert.assertEquals(Arrays.asList("hey2", "hey1"), pojos.getContent().stream().map(Pojo::getId).collect(Collectors.toList()));
        Assert.assertEquals(0, pojos.getNumber());

        Page<Pojo> nextPojos = repository.testPaged("%", pojos.nextPageable());
        Assert.assertEquals(2, nextPojos.getTotalPages());
        Assert.assertEquals(3, nextPojos.getTotalElements());
        Assert.assertEquals(Collections.singletonList("hey"), nextPojos.getContent().stream().map(Pojo::getId).collect(Collectors.toList()));
        Assert.assertEquals(1, nextPojos.getNumber());
    }

    @Test
    public void testCustomMapper() {
        PojoDTO dto = repository.testCustomMapper();
        Assert.assertEquals("hey", dto.getId());
    }

    @Test
    public void testCustomImpl() {
        Assert.assertEquals(42, repository.dummy());
    }
}
