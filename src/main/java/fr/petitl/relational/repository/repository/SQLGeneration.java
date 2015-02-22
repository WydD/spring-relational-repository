package fr.petitl.relational.repository.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 *
 */
public interface SQLGeneration {
    String countStar();

    String deleteById();

    String delete();

    String exists();

    String selectById();

    String selectAll();

    String selectIds();

    String insertInto();

    String update();

    String selectAll(int idCount);

    String selectAll(Pageable page);

    String selectAll(Sort sort);
}
