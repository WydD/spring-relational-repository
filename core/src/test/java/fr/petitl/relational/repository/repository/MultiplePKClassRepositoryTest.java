package fr.petitl.relational.repository.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import fr.petitl.relational.repository.repository.model.MultiplePKClass;
import fr.petitl.relational.repository.repository.model.MultiplePKClass.MPK;
import fr.petitl.relational.repository.template.RelationalTemplate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static fr.petitl.relational.repository.TestUtils.set;
import static org.junit.Assert.*;

public class MultiplePKClassRepositoryTest extends AbstractRepositoryTest {

    private SimpleRelationalRepository<MultiplePKClass, MPK> repository;

    @Before
    public void init() {
        repository = simpleBuild();
    }

    @Test
    public void testCount() throws Exception {
        Assert.assertEquals(3, repository.count());
    }

    @Test
    public void testDelete() throws Exception {
        repository.delete(new MultiplePKClass(2, "canard", null, null)); // other attributes are irrelevant
        repository.delete(new MultiplePKClass(5, "coin", null, null)); // other attributes are irrelevant
        verifyDeleted(repository, 2, MPK.create(2, "canard"));
    }

    @Test
    public void testDelete1() throws Exception {
        repository.delete(MPK.create(2, "canard"));
        repository.delete(MPK.create(4, "canard"));
        verifyDeleted(repository, 2, MPK.create(2, "canard"));
    }

    @Test
    public void testDelete2() throws Exception {
        repository.delete(Arrays.asList(new MultiplePKClass(2, "canard", null, null), new MultiplePKClass(3, "youpi", null, null)));
        repository.delete(Collections.singletonList(new MultiplePKClass(4, "canard", null, null)));
        verifyDeleted(repository, 1, MPK.create(2, "canard"), MPK.create(3, "youpi"));
    }

    @Test
    public void testDeleteAll() throws Exception {
        repository.deleteAll();
        verifyDeleted(repository, 0, MPK.create(1, "canard"), MPK.create(2, "canard"), MPK.create(3, "youpi"));
    }

    @Test
    public void testExists() throws Exception {
        assertTrue(repository.exists(MPK.create(2, "canard")));
        assertFalse(repository.exists(MPK.create(4, "canard")));
    }

    @Test
    public void testFindAll() throws Exception {
        verifyFindAll(repository.findAll(), 1, 2, 3);
    }


    @Test
    public void testFindAll1() throws Exception {
        List<MultiplePKClass> all = repository.findAll(new Sort(Arrays.asList(
                new Sort.Order(Sort.Direction.DESC, "name"),
                new Sort.Order(Sort.Direction.ASC, "created_date")
        )));
        verifyFindAll(all, 1, 2, 3);
        assertEquals(Arrays.asList(2, 3, 1), all.stream().map(MultiplePKClass::getId).collect(Collectors.toList()));
    }

    @Test
    public void testFindAll2() throws Exception {
        List<MultiplePKClass> all = repository.findAll(Arrays.asList(MPK.create(1, "canard"), MPK.create(3, "youpi")));
        verifyFindAll(all, 1, 3);
    }

    @Test
    public void testFindAll3() throws Exception {
        Page<MultiplePKClass> first = repository.findAll(new PageRequest(0, 2));
        Page<MultiplePKClass> last = repository.findAll(new PageRequest(1, 2));
        assertEquals(2, first.getTotalPages());
        assertEquals(3, first.getTotalElements());
        assertEquals(2, first.getContent().size());
        assertEquals(2, last.getTotalPages());
        assertEquals(3, last.getTotalElements());
        assertEquals(1, last.getContent().size());

        HashSet<MultiplePKClass> set = new HashSet<>();
        first.getContent().stream().forEach(set::add);
        last.getContent().stream().forEach(set::add);

        verifyFindAll(set, 1, 2, 3);

        Page<MultiplePKClass> firstPage = repository.findAll(new PageRequest(0, 2, new Sort(Collections.singletonList(
                new Sort.Order(Sort.Direction.DESC, "created_date")
        ))));
        verifyFindAll(firstPage.getContent(), 2, 1);
        assertEquals(Arrays.asList(2, 1), firstPage.getContent().stream().map(MultiplePKClass::getId).collect(Collectors.toList()));
    }

    @Test
    public void testFindOne() throws Exception {
        verifyPojo2(repository.findOne(MPK.create(2, "canard")));
        assertNull(repository.findOne(MPK.create(2, "youpi")));
    }

    @Test
    public void testSave() throws Exception {
        MultiplePKClass before = new MultiplePKClass(null, "yeah", "Wooh", new Date());

        Date createdDate = before.getCreatedDate();
        String name = before.getName();

        MultiplePKClass after = repository.save(before);
        assertEquals(createdDate, after.getCreatedDate());
        assertEquals(name, after.getName());
        assertEquals("yeah", after.getType());
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
        MultiplePKClass beforeFirst = new MultiplePKClass(null, "woot", "First", createdDate);
        MultiplePKClass beforeSecond = new MultiplePKClass(null, "w00t", "Second", createdDate);
        List<MultiplePKClass> all = (List<MultiplePKClass>) repository.save(Arrays.asList(beforeFirst, beforeSecond));

        assertEquals(createdDate, all.get(0).getCreatedDate());
        assertEquals("woot", all.get(0).getType());
        assertEquals("First", all.get(0).getName());
        assertEquals(4, all.get(0).getId().intValue());

        assertEquals(createdDate, all.get(1).getCreatedDate());
        assertEquals("w00t", all.get(1).getType());
        assertEquals("Second", all.get(1).getName());
        assertEquals(5, all.get(1).getId().intValue());

        verifyEqualsInDB(beforeFirst);
        verifyEqualsInDB(beforeSecond);
    }

    protected void verifyEqualsInDB(MultiplePKClass beforeFirst) throws SQLException {
        repository.getTemplate().execute(con -> con.prepareStatement("SELECT id, type, name, created_date FROM Multiple WHERE id = " + beforeFirst.getId() + " AND type = '" + beforeFirst.getType() + "'"), (PreparedStatement statement) -> {
            ResultSet rs = statement.executeQuery();
            assertTrue(rs.next());
            assertEquals(beforeFirst.getId().intValue(), rs.getInt(1));
            assertEquals(beforeFirst.getType(), rs.getString(2));
            assertEquals(beforeFirst.getName(), rs.getString(3));
            assertEquals(beforeFirst.getCreatedDate(), rs.getTimestamp(4));
            return 1;
        });
    }

    @Test
    public void testUpdate() throws Exception {
        MultiplePKClass before = new MultiplePKClass(3, "youpi", "Youpi", new Date());

        try {
            repository.save(before);
            // This is a conflict raised by the database!
            assert false;
        } catch (DuplicateKeyException ignored) {
        }

        repository.update(before);
        verifyEqualsInDB(before);

        try {
            repository.update(new MultiplePKClass(4, "youpi again", "Youpi", new Date()));
            assert false;
        } catch (IncorrectResultSizeDataAccessException ignored) {
        }
    }

    @Test
    public void testUpdate1() throws Exception {
        MultiplePKClass pojo2 = new MultiplePKClass(2, "canard", "Youpi", new Date());
        MultiplePKClass pojo3 = new MultiplePKClass(3, "youpi", "Yeah", new Date());
        repository.update(Arrays.asList(pojo2, pojo3).stream());

        verifyEqualsInDB(pojo2);
        verifyEqualsInDB(pojo3);

        try {
            repository.update(Arrays.asList(pojo2, new MultiplePKClass(4, "youpi again", "Yeah", new Date())).stream());
            assert false;
        } catch (IncorrectResultSizeDataAccessException ignored) {
        }
    }

    @Test
    public void testStreamAll() throws Exception {
        assertEquals(Integer.valueOf(1), repository.fetchAll(out -> {
            Map<Integer, MultiplePKClass> result = out.collect(Collectors.toMap(MultiplePKClass::getId, it -> it));
            verifyPojo1(result.get(1));
            verifyPojo2(result.get(2));
            verifyPojo3(result.get(3));
            return 1;
        }));
    }

    @Test
    public void testStreamAllIds() throws Exception {
        assertTrue(repository.fetchAllIds(out -> {
            assertEquals(set(1, 2, 3), out.map(it -> it.id).collect(Collectors.toSet()));
            return true;
        }));
    }

    protected void verifyFindAll(Collection<MultiplePKClass> all, Integer... ids) {
        Set<Integer> idSet = set(ids);
        if (idSet.contains(1)) {
            verifyPojo1(all.stream().filter(it -> it.getId() == 1).findFirst().get());
        }

        if (idSet.contains(2)) {
            verifyPojo2(all.stream().filter(it -> it.getId() == 2).findFirst().get());
        }

        if (idSet.contains(3)) {
            verifyPojo3(all.stream().filter(it -> it.getId() == 3).findFirst().get());
        }
    }

    private void verifyPojo3(MultiplePKClass pojo3) {
        assertEquals(3, pojo3.getId().intValue());
        assertEquals("youpi", pojo3.getType());
        assertEquals("Hey", pojo3.getName());
        assertEquals(dateFrom("2014-02-14T20:02:32"), pojo3.getCreatedDate());
    }

    private void verifyPojo2(MultiplePKClass pojo2) {
        assertEquals(2, pojo2.getId().intValue());
        assertEquals("canard", pojo2.getType());
        assertEquals("Ho", pojo2.getName());
        assertEquals(dateFrom("2014-05-19T20:02:32"), pojo2.getCreatedDate());
    }

    private void verifyPojo1(MultiplePKClass pojo1) {
        assertEquals(1, pojo1.getId().intValue());
        assertEquals("canard", pojo1.getType());
        assertEquals("Hey", pojo1.getName());
        assertEquals(dateFrom("2014-05-14T20:02:32"), pojo1.getCreatedDate());
    }

    protected SimpleRelationalRepository<MultiplePKClass, MPK> simpleBuild() {
        return getRepository(MultiplePKClass.class, MultiplePKClass.CREATE, MultiplePKClass.INSERT);
    }

    protected void verifyDeleted(SimpleRelationalRepository<MultiplePKClass, MPK> repository, int count, MPK... missingIds) throws SQLException {
        RelationalTemplate template = repository.getTemplate();
        for (MPK id : missingIds) {
            template.execute(con -> con.prepareStatement("SELECT * FROM Multiple WHERE id = " + id.id + " AND type='" + id.type + "'"), (PreparedStatement statement) -> {
                ResultSet rs = statement.executeQuery();
                assertFalse(rs.next());
                return 1;
            });
        }

        template.execute(con -> con.prepareStatement("SELECT count(*) FROM Multiple"), (PreparedStatement statement) -> {
            ResultSet rs = statement.executeQuery();
            assertTrue(rs.next());
            assertEquals(count, rs.getInt(1));
            return 1;
        });
    }
}