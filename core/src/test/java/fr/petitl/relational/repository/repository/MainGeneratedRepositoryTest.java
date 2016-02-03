package fr.petitl.relational.repository.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.petitl.relational.repository.repository.model.MainGenerated;
import fr.petitl.relational.repository.template.RelationalTemplate;
import fr.petitl.relational.repository.template.TemplateWithCounter;
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

public class MainGeneratedRepositoryTest extends AbstractRepositoryTest {

    private SimpleRelationalRepository<MainGenerated, Integer> repository;
    private TemplateWithCounter template;

    @Before
    public void init() {
        repository = simpleBuild();
        template = (TemplateWithCounter) repository.getTemplate();
    }

    @Test
    public void testCount() throws Exception {
        Assert.assertEquals(3, repository.count());
    }

    @Test
    public void testDelete() throws Exception {
        repository.delete(new MainGenerated(2, null, null)); // other attributes are irrelevant
        repository.delete(new MainGenerated(5, null, null)); // other attributes are irrelevant
        verifyDeleted(repository, 2, 2);
    }

    @Test
    public void testDelete1() throws Exception {
        repository.delete(2);
        repository.delete(4);
        verifyDeleted(repository, 2, 2);
    }

    @Test
    public void testDelete2() throws Exception {
        long queryCounter = template.getQueryCounter();
        repository.delete(Arrays.asList(new MainGenerated(2, null, null), new MainGenerated(3, null, null)));
        // DO NOT DO A FOREACH DELETE !
        Assert.assertEquals(queryCounter+1, template.getQueryCounter());
        repository.delete(Collections.singletonList(new MainGenerated(4, null, null)));
        verifyDeleted(repository, 1, 2, 3);
    }

    @Test
    public void testDeleteAll() throws Exception {
        repository.deleteAll();
        verifyDeleted(repository, 0, 1, 2, 3);
    }

    @Test
    public void testExists() throws Exception {
        assertTrue(repository.exists(2));
        assertFalse(repository.exists(4));
    }

    @Test
    public void testFindAll() throws Exception {
        verifyFindAll(repository.findAll(), 1, 2, 3);
    }


    @Test
    public void testFindAll1() throws Exception {
        List<MainGenerated> all = repository.findAll(new Sort(Arrays.asList(
                new Sort.Order(Sort.Direction.DESC, "name"),
                new Sort.Order(Sort.Direction.ASC, "created_date")
        )));
        verifyFindAll(all, 1, 2, 3);
        assertEquals(Arrays.asList(2, 3, 1), all.stream().map(MainGenerated::getId).collect(Collectors.toList()));
    }

    @Test
    public void testFindAll2() throws Exception {
        List<MainGenerated> all = repository.findAll(Arrays.asList(1, 3));
        verifyFindAll(all, 1, 3);
    }

    @Test
    public void testFindAllIndexed() throws Exception {
        Map<Integer, MainGenerated> all = repository.findAll(Stream.of(1, 3), repository.asIndex());
        verifyPojo1(all.get(1));
        verifyPojo3(all.get(3));
        assertEquals(2, all.size());
    }

    @Test
    public void testFindAll3() throws Exception {
        Page<MainGenerated> first = repository.findAll(new PageRequest(0, 2));
        Page<MainGenerated> last = repository.findAll(new PageRequest(1, 2));
        assertEquals(2, first.getTotalPages());
        assertEquals(3, first.getTotalElements());
        assertEquals(2, first.getContent().size());
        assertEquals(2, last.getTotalPages());
        assertEquals(3, last.getTotalElements());
        assertEquals(1, last.getContent().size());

        HashSet<MainGenerated> set = new HashSet<>();
        first.getContent().stream().forEach(set::add);
        last.getContent().stream().forEach(set::add);

        verifyFindAll(set, 1, 2, 3);

        Page<MainGenerated> firstPage = repository.findAll(new PageRequest(0, 2, new Sort(Collections.singletonList(
                new Sort.Order(Sort.Direction.DESC, "created_date")
        ))));
        verifyFindAll(firstPage.getContent(), 2, 1);
        assertEquals(Arrays.asList(2, 1), firstPage.getContent().stream().map(MainGenerated::getId).collect(Collectors.toList()));
    }

    @Test
    public void testFindOne() throws Exception {
        verifyPojo2(repository.findOne(2));
    }

    @Test
    public void testSave() throws Exception {
        MainGenerated before = new MainGenerated(null, "Youpi", new Date());

        Date createdDate = before.getCreatedDate();
        String name = before.getName();

        MainGenerated after = repository.save(before);
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
        MainGenerated beforeFirst = new MainGenerated(null, "First", createdDate);
        MainGenerated beforeSecond = new MainGenerated(null, "Second", createdDate);
        List<MainGenerated> all = (List<MainGenerated>) repository.save(Arrays.asList(beforeFirst, beforeSecond));

        assertEquals(createdDate, all.get(0).getCreatedDate());
        assertEquals("First", all.get(0).getName());
        assertEquals(4, all.get(0).getId().intValue());

        assertEquals(createdDate, all.get(1).getCreatedDate());
        assertEquals("Second", all.get(1).getName());
        assertEquals(5, all.get(1).getId().intValue());

        verifyEqualsInDB(beforeFirst);
        verifyEqualsInDB(beforeSecond);
    }

    protected void verifyEqualsInDB(MainGenerated beforeFirst) throws SQLException {
        repository.getTemplate().execute(con -> con.prepareStatement("SELECT id, name, created_date FROM MainGenerated WHERE id = " + beforeFirst.getId()), (PreparedStatement statement) -> {
            ResultSet rs = statement.executeQuery();
            assertTrue(rs.next());
            assertEquals(beforeFirst.getId().intValue(), rs.getInt(1));
            assertEquals(beforeFirst.getName(), rs.getString(2));
            assertEquals(beforeFirst.getCreatedDate(), rs.getTimestamp(3));
            return 1;
        });
    }

    @Test
    public void testUpdate() throws Exception {
        MainGenerated before = new MainGenerated(3, "Youpi", new Date());

        try {
            repository.save(before);
            // This is a conflict raised by the database!
            assert false;
        } catch (DuplicateKeyException ignored) {
        }

        repository.update(before);
        verifyEqualsInDB(before);

        try {
            repository.update(new MainGenerated(4, "Youpi", new Date()));
            assert false;
        } catch (IncorrectResultSizeDataAccessException ignored) {
        }
    }

    @Test
    public void testUpdate1() throws Exception {
        MainGenerated pojo2 = new MainGenerated(2, "Youpi", new Date());
        MainGenerated pojo3 = new MainGenerated(3, "Yeah", new Date());
        repository.update(Arrays.asList(pojo2, pojo3).stream());

        verifyEqualsInDB(pojo2);
        verifyEqualsInDB(pojo3);

        try {
            repository.update(Arrays.asList(pojo2, new MainGenerated(4, "Yeah", new Date())).stream());
            assert false;
        } catch (IncorrectResultSizeDataAccessException ignored) {
        }
    }

    @Test
    public void testStreamAll() throws Exception {
        assertEquals(Integer.valueOf(1), repository.fetchAll(out -> {
            Map<Integer, MainGenerated> result = out.collect(Collectors.toMap(MainGenerated::getId, it -> it));
            verifyPojo1(result.get(1));
            verifyPojo2(result.get(2));
            verifyPojo3(result.get(3));
            return 1;
        }));
    }

    @Test
    public void testStreamAllIds() throws Exception {
        assertTrue(repository.fetchAllIds(out -> {
            assertEquals(set(1, 2, 3), out.collect(Collectors.toSet()));
            return true;
        }));
    }

    protected void verifyFindAll(Collection<MainGenerated> all, Integer... ids) {
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

    private void verifyPojo3(MainGenerated pojo3) {
        assertEquals(3, pojo3.getId().intValue());
        assertEquals("Hey", pojo3.getName());
        assertEquals(dateFrom("2014-02-14T20:02:32"), pojo3.getCreatedDate());
    }

    private void verifyPojo2(MainGenerated pojo2) {
        assertEquals(2, pojo2.getId().intValue());
        assertEquals("Ho", pojo2.getName());
        assertEquals(dateFrom("2014-05-19T20:02:32"), pojo2.getCreatedDate());
    }

    private void verifyPojo1(MainGenerated pojo1) {
        assertEquals(1, pojo1.getId().intValue());
        assertEquals("Hey", pojo1.getName());
        assertEquals(dateFrom("2014-05-14T20:02:32"), pojo1.getCreatedDate());
    }

    protected SimpleRelationalRepository<MainGenerated, Integer> simpleBuild() {
        return getRepository(MainGenerated.class, MainGenerated.CREATE, MainGenerated.INSERT);
    }

    protected void verifyDeleted(SimpleRelationalRepository<MainGenerated, Integer> repository, int count, Integer... missingIds) throws SQLException {
        RelationalTemplate template = repository.getTemplate();
        for (Integer id : missingIds) {
            template.execute(con -> con.prepareStatement("SELECT * FROM MainGenerated WHERE id = " + id), (PreparedStatement statement) -> {
                ResultSet rs = statement.executeQuery();
                assertFalse(rs.next());
                return 1;
            });
        }

        template.execute(con -> con.prepareStatement("SELECT count(*) FROM MainGenerated"), (PreparedStatement statement) -> {
            ResultSet rs = statement.executeQuery();
            assertTrue(rs.next());
            assertEquals(count, rs.getInt(1));
            return 1;
        });
    }
}