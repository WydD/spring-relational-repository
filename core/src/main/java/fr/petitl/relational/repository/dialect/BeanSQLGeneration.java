package fr.petitl.relational.repository.dialect;

/**
 *
 */
public interface BeanSQLGeneration {
    String countStar();

    String deleteById();

    String delete();

    String exists();

    String selectById();

    String selectAll();

    String selectIds();

    String insertInto();

    String update();

    String selectByIds();

    String deleteByIds();
}
