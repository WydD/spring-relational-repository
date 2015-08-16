package fr.petitl.relational.repository.repository;

import fr.petitl.relational.repository.util.WindowedSpliterator;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class FKResolver<E, F> {
    private final Function<E, F> create;
    private final List<ResolveStep<E, F, ?, ?>> steps;

    public static <E, F> Builder<E, F> of(Function<E, F> create) {
        return new Builder<>(create);
    }

    public Stream<F> resolve(Stream<E> stream) {
        List<E> orig = stream.collect(Collectors.toList());
        return doResolve(orig);
    }

    public Stream<F> resolve(Stream<E> stream, int bulkSize) {
        Stream<List<E>> windowedStream = StreamSupport.stream(new WindowedSpliterator<>(stream.spliterator(), bulkSize, bulkSize), false);
        return windowedStream.map(this::doResolve).flatMap(it -> it);
    }

    private Stream<F> doResolve(List<E> orig) {
        steps.forEach(it -> it.resolve(orig));
        return orig.stream().map(it -> {
            F newObject = create.apply(it);
            steps.forEach(step -> step.set(it, newObject));
            return newObject;
        });
    }

    public static class Builder<E, F> {
        private final Function<E, F> create;
        private final List<ResolveStep<E, F, ?, ?>> steps = new LinkedList<>();

        public Builder(Function<E, F> create) {
            this.create = create;
        }

        public <ID extends Serializable, T> Builder<E, F> add(Function<E, ID> fkGetter, RelationalRepository<T, ID> rel, BiConsumer<F, T> fkSetter) {
            return add(fkGetter, rel, fkSetter, false);
        }

        public <ID extends Serializable, T> Builder<E, F> add(Function<E, ID> fkGetter, RelationalRepository<T, ID> rel, BiConsumer<F, T> fkSetter, boolean setIfNull) {
            steps.add(new ResolveStep<>(fkGetter, rel, fkSetter, setIfNull));
            return this;
        }

        public FKResolver<E, F> build() {
            return new FKResolver<>(create, steps);
        }
    }

    private static class ResolveStep<E, F, T, ID extends Serializable> {
        public Function<E, ID> fkGetter;
        public RelationalRepository<T, ID> rel;
        public BiConsumer<F, T> fkSetter;
        public Map<Object, T> resolved;
        public boolean setIfNull;

        public ResolveStep(Function<E, ID> fkGetter, RelationalRepository<T, ID> rel, BiConsumer<F, T> fkSetter, boolean setIfNull) {
            this.fkGetter = fkGetter;
            this.rel = rel;
            this.fkSetter = fkSetter;
            this.setIfNull = setIfNull;
        }

        public void set(E e, F f) {
            Object id = fkGetter.apply(e);
            if (id == null) return;
            T result = resolved.get(ensureId(id));
            if (result == null) {
                if (setIfNull)
                    fkSetter.accept(f, null);
                return;
            }
            fkSetter.accept(f, result);
        }

        private Object ensureId(Object id) {
            if (id.getClass().isArray()) {
                // We have a problem
                return Arrays.asList((Object[])id);
            }
            return id;
        }

        public void resolve(List<E> orig) {
            resolved = rel.resolveFK(orig.stream(), fkGetter, stream -> stream.collect(Collectors.toMap(it -> ensureId(rel.pkGetter().apply(it)), it -> it)));
        }
    }

    protected FKResolver(Function<E, F> create, List<ResolveStep<E, F, ?, ?>> steps) {
        this.create = create;
        this.steps = steps;
    }
}
