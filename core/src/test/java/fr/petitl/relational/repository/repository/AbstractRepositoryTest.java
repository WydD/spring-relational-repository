package fr.petitl.relational.repository.repository;

import javax.sql.DataSource;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import fr.petitl.relational.repository.SpringTest;
import fr.petitl.relational.repository.dialect.BeanDialectProvider;
import fr.petitl.relational.repository.support.RelationalEntityInformation;
import fr.petitl.relational.repository.template.RelationalTemplate;

/**
 *
 */
public abstract class AbstractRepositoryTest {

    protected Date dateFrom(String text) {
        return Date.from(LocalDateTime.parse(text).atZone(ZoneId.systemDefault()).toInstant());
    }

    protected <T, ID extends Serializable> SimpleRelationalRepository<T, ID> getRepository(Class<T> clazz, String... sql) {
        DataSource ds = SpringTest.createEmbbededDataSource(sql);
        RelationalTemplate template = new RelationalTemplate(ds, BeanDialectProvider.h2());
        return new SimpleRelationalRepository<>(new RelationalEntityInformation<>(template.getMappingData(clazz), template), template);
    }
}
