package fr.petitl.relational.repository.query;

import java.lang.reflect.Method;

import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

/**
 *
 */
public class JdbcQueryMethod extends QueryMethod {
    private final Method method;

    /**
     * Creates a new {@link org.springframework.data.repository.query.QueryMethod} from the given parameters. Looks up the correct query to use for following
     * invocations of the method given.
     *
     * @param method   must not be {@literal null}
     * @param metadata must not be {@literal null}
     */
    public JdbcQueryMethod(Method method, RepositoryMetadata metadata) {
        super(method, metadata);

        this.method = method;
    }

    public Query getAnnotation() {
        return method.getAnnotation(Query.class);
    }

    public TypeInformation<?> getReturnType() {
        return ClassTypeInformation.fromReturnTypeOf(method);
    }
}
