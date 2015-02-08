package fr.petitl.relational.repository.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.google.common.collect.Lists;
import fr.petitl.relational.repository.repository.model.MainNotGenerated;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.Assert.*;

public class MainNotGeneratedRepositoryTest extends AbstractRepositoryTest {

    private SimpleRelationalRepository<MainNotGenerated, Integer> repository;

    @Before
    public void init() {
        repository = simpleBuild();
    }

    @Test
    public void testSave() throws Exception {
        MainNotGenerated before = new MainNotGenerated(null, "Youpi", new Date());

        Date createdDate = before.getCreatedDate();
        String name = before.getName();

        try {
            repository.save(before);
            // This is a conflict raised by the database!
            assert false;
        } catch (DataIntegrityViolationException ignored) {
        }
        before.setId(4);

        MainNotGenerated after = repository.save(before);
        assertEquals(createdDate, after.getCreatedDate());
        assertEquals(name, after.getName());
        assertEquals(4, after.getId().intValue());

        verifyEqualsInDB(after);

        try {
            repository.save(after);
            // This is a conflict raised by the database!
            assert false;
        } catch (DuplicateKeyException ignored) {
        }
    }

    @Test
    public void testSave1() throws Exception {
        Date createdDate = new Date();
        MainNotGenerated beforeFirst = new MainNotGenerated(4, "First", createdDate);
        MainNotGenerated beforeSecond = new MainNotGenerated(5, "Second", createdDate);
        List<MainNotGenerated> all = Lists.newArrayList(repository.save(Arrays.asList(beforeFirst, beforeSecond)));

        assertEquals(createdDate, all.get(0).getCreatedDate());
        assertEquals("First", all.get(0).getName());
        assertEquals(4, all.get(0).getId().intValue());

        assertEquals(createdDate, all.get(1).getCreatedDate());
        assertEquals("Second", all.get(1).getName());
        assertEquals(5, all.get(1).getId().intValue());

        verifyEqualsInDB(beforeFirst);
        verifyEqualsInDB(beforeSecond);

        try {
            repository.save(Arrays.asList(new MainNotGenerated(6, "First", createdDate), new MainNotGenerated(3, "First", createdDate)));
            assert false;
        } catch (DuplicateKeyException ignored) {
        }
    }

    protected void verifyEqualsInDB(MainNotGenerated beforeFirst) throws SQLException {
        repository.getTemplate().execute((PreparedStatement statement) -> {
            ResultSet rs = statement.executeQuery();
            assertTrue(rs.next());
            assertEquals(beforeFirst.getId().intValue(), rs.getInt(1));
            assertEquals(beforeFirst.getName(), rs.getString(2));
            assertEquals(beforeFirst.getCreatedDate(), rs.getTimestamp(3));
            return 1;
        }, con -> con.prepareStatement("SELECT id, name, created_date FROM MainNotGenerated WHERE id = " + beforeFirst.getId()));
    }

    protected SimpleRelationalRepository<MainNotGenerated, Integer> simpleBuild() {
        return getRepository(MainNotGenerated.class, MainNotGenerated.CREATE, MainNotGenerated.INSERT);
    }
}