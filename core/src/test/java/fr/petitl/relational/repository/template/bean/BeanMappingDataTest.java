package fr.petitl.relational.repository.template.bean;

import java.util.Date;

import fr.petitl.relational.repository.dialect.BeanDialect;
import fr.petitl.relational.repository.dialect.SimpleDialectProvider;
import fr.petitl.relational.repository.template.RelationalTemplate;
import org.junit.Test;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 *
 */
public class BeanMappingDataTest {

    public static final BeanDialect DIALECT = SimpleDialectProvider.h2();
    public static final RelationalTemplate DUMMY_TEMPLATE = new RelationalTemplate(new DelegatingDataSource(), DIALECT);

    public static class PublicAttributeMapping {
        private int id;
        public Date createdDate;
    }

    @Test
    public void testPublicAttributeMapping() {
        BeanMappingData<PublicAttributeMapping> mappingData = new BeanMappingData<>(PublicAttributeMapping.class, DIALECT, new RelationalTemplate(new DelegatingDataSource(), DIALECT));
        try {
            mappingData.fieldForColumn("createddate");
            fail("Found createddate which is not supposed to happen");
        } catch (Exception ignored) {
        }
        FieldMappingData createdDate;
        try {
            createdDate = mappingData.fieldForColumn("created_date");
        } catch (Exception e) {
            fail("Could not find created date");
            return;
        }
        PublicAttributeMapping instance = new PublicAttributeMapping();
        Date date = new Date(321);
        instance.createdDate = date;
        assertEquals(date, createdDate.readMethod.apply(instance));
        Date date2 = new Date(123);
        createdDate.writeMethod.accept(instance, date2);
        assertEquals(date2, instance.createdDate);
    }

    public static class MethodAttributeMapping {
        private int id;
        private Date createdDate;
        private transient long time = -1;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public Date getCreatedDate() {
            return new Date(createdDate.getTime() - 1);
        }

        public void setCreatedDate(Date createdDate) {
            this.time = createdDate.getTime();
            this.createdDate = createdDate;
        }
    }

    @Test
    public void testMethodAttributeMapping() {
        BeanMappingData<MethodAttributeMapping> mappingData = new BeanMappingData<>(MethodAttributeMapping.class, DIALECT, DUMMY_TEMPLATE);
        try {
            mappingData.fieldForColumn("createddate");
            fail("Found createddate which is not supposed to happen");
        } catch (Exception ignored) {
        }
        FieldMappingData createdDate;
        try {
            createdDate = mappingData.fieldForColumn("created_date");
        } catch (Exception e) {
            fail("Could not find created date");
            return;
        }
        MethodAttributeMapping instance = new MethodAttributeMapping();
        instance.createdDate = new Date(321);
        assertEquals(new Date(320), createdDate.readMethod.apply(instance));
        assertEquals(-1, instance.time);
        Date date2 = new Date(123);
        createdDate.writeMethod.accept(instance, date2);
        assertEquals(date2, instance.createdDate);
        assertEquals(date2.getTime(), instance.time);
    }
}