package fr.petitl.relational.repository.repository;

import fr.petitl.relational.repository.util.StreamUtils;

import java.io.Serializable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FKResolver<E, F> {
    private final Function<E, F> create;
    private final List<ResolveStep<E, F, ?, ?>> steps;

    public static <E, F> Builder<E, F> of(Function<E, F> create) {
        return new Builder<>(create);
    }

    public F resolve(E entry) {
        return doResolve(Collections.singletonList(entry)).findFirst().get();
    }

    public Stream<F> resolve(Stream<E> stream) {
        List<E> orig = stream.collect(Collectors.toList());
        return doResolve(orig);
    }

    public Stream<F> resolve(Stream<E> stream, int bulkSize) {
        return StreamUtils.bulk(stream, bulkSize).map(this::doResolve).flatMap(it -> it);
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

        public <ID extends Serializable, T> Builder<E, F> add(Function<E, ID> fkGetter, RelationalRepository<T, ID> rel, BiConsumer<F, List<T>> fkSetter) {
            return add(fkGetter, rel, fkSetter, false);
        }

        public <ID extends Serializable, T> Builder<E, F> add(Function<E, ID> fkGetter, RelationalRepository<T, ID> rel, BiConsumer<F, List<T>> fkSetter, boolean setIfNull) {
            return add(fkGetter, rel, rel.pkGetter(), fkSetter, setIfNull);
        }

        public <ID extends Serializable, T> Builder<E, F> add(Function<E, ID> fkGetter, RepositoryResolution<T, ID> resolution, Function<T, ID> pkGetter, BiConsumer<F, List<T>> fkSetter) {
            return add(fkGetter, resolution, pkGetter, fkSetter, false);
        }

        public <ID extends Serializable, T> Builder<E, F> add(Function<E, ID> fkGetter, RepositoryResolution<T, ID> resolution, Function<T, ID> pkGetter, BiConsumer<F, List<T>> fkSetter, boolean setIfNull) {
            steps.add(new ResolveStep<>(fkGetter, resolution, pkGetter, fkSetter, setIfNull));
            return this;
        }

        public FKResolver<E, F> build() {
            return new FKResolver<>(create, steps);
        }
    }

    private static class ResolveStep<E, F, T, ID extends Serializable> {
        public Function<E, ID> fkGetter;
        public RepositoryResolution<T, ID> resolution;
        public BiConsumer<F, List<T>> fkSetter;
        public Map<Object, List<T>> resolved;
        public boolean setIfNull;
        public Function<T, Object> pkGetter;

        public ResolveStep(Function<E, ID> fkGetter, RepositoryResolution<T, ID> resolution, Function<T, ID> pkGetter, BiConsumer<F, List<T>> fkSetter, boolean setIfNull) {
            this.fkGetter = fkGetter;
            this.resolution = resolution;
            this.fkSetter = fkSetter;
            this.setIfNull = setIfNull;
            this.pkGetter = it -> ensureId(pkGetter.apply(it));
        }

        public void set(E e, F f) {
            Object id = fkGetter.apply(e);
            if (id == null) {
                if (setIfNull)
                    fkSetter.accept(f, null);
                return;
            }
            List<T> result = resolved.get(ensureId(id));
            if (result == null) {
                fkSetter.accept(f, new LinkedList<>());
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
            resolved = resolution.findAll(orig.stream().map(fkGetter).filter(it -> it != null), StreamUtils.asIndexMultiple(pkGetter));
        }
    }

    protected FKResolver(Function<E, F> create, List<ResolveStep<E, F, ?, ?>> steps) {
        this.create = create;
        this.steps = steps;
    }
}
