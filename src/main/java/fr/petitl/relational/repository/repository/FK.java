package fr.petitl.relational.repository.repository;

import java.io.Serializable;
import java.util.function.Function;

/**
 *
 */
public class FK<ID extends Serializable, TYPE> {
    private ID id;
    private Function<ID, TYPE> resolver;
    private TYPE resolved;

    protected FK(ID id, Function<ID, TYPE> resolver) {
        if (id == null) {
            throw new IllegalArgumentException("Null ID given as foreign key argument");
        }
        this.id = id;
        this.resolver = resolver;
    }

    protected FK(ID id, TYPE resolved, Function<ID, TYPE> resolver) {
        this(id, resolver);
        this.resolved = resolved;
    }

    public ID getId() {
        return id;
    }

    public TYPE resolve() {
        if (resolved != null) {
            return resolved;
        }
        return forceResolve();
    }

    public TYPE forceResolve() {
        resolved = resolver.apply(id);
        return resolved;
    }
}
