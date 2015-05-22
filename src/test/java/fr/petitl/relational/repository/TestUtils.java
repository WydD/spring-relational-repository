package fr.petitl.relational.repository;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class TestUtils {
    public static <E> Set<E> set(E... values) {
        HashSet<E> set = new HashSet<>();
        Collections.addAll(set, values);
        return set;
    }
}
