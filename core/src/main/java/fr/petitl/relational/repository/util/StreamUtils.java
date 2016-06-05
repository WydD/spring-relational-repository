package fr.petitl.relational.repository.util;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Created by WydD on 05/06/2016.
 */
public class StreamUtils {
    public static <T> Stream<List<T>> bulk(Stream<T> stream, int bulkSize) {
        return StreamSupport.stream(new WindowedSpliterator<>(stream.spliterator(), bulkSize, bulkSize), false);
    }

    public static <T, ID> Function<Stream<T>, Map<ID, T>> asIndex(Function<T, ID> getter) {
        return stream -> stream.collect(Collectors.toMap(getter, it -> it));
    }

    public static <T, ID> Function<Stream<T>, Map<ID, List<T>>> asIndexMultiple(Function<T, ID> getter) {
        return stream -> stream.collect(Collectors.groupingBy(getter));
    }
}
