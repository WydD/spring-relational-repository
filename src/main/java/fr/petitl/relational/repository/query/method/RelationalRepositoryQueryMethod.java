package fr.petitl.relational.repository.query.method;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.petitl.relational.repository.query.Query;
import fr.petitl.relational.repository.template.RelationalTemplate;
import fr.petitl.relational.repository.template.RowMapper;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.util.QueryExecutionConverters;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

/**
 *
 */
public class RelationalRepositoryQueryMethod extends QueryMethod {
    static {
        QueryExecutionConverters.supports(Stream.class);
    }

    private final Method method;
    private RelationalTemplate template;
    private Class mappingType;

    /**
     * Creates a new {@link org.springframework.data.repository.query.QueryMethod} from the given parameters. Looks up the correct query to use for following
     * invocations of the method given.
     *
     * @param method   must not be {@literal null}
     * @param metadata must not be {@literal null}
     */
    public RelationalRepositoryQueryMethod(Method method, RepositoryMetadata metadata, RelationalTemplate template) {
        super(method, metadata);

        this.method = method;
        this.template = template;
        // TODO: dirty hack for now...
        mappingType = this.getReturnedObjectType().equals(Object.class) ? this.getDomainClass() : this.getReturnedObjectType();
    }

    public Query getAnnotation() {
        return method.getAnnotation(Query.class);
    }

    public TypeInformation<?> getReturnType() {
        return ClassTypeInformation.fromReturnTypeOf(method);
    }

    @SuppressWarnings("unchecked")
    public RepositoryQuery createAnnotationBased() {
        Query annotation = getAnnotation();
        String sql = annotation.value();

        RowMapper mapper = template.<Object>getMappingData(mappingType).getMapper();
        if (this.isPageQuery() || this.isSliceQuery()) {
            // Paged queries
            throw new IllegalStateException("Unsupported page queries");
        } else {
            Parameters<?, ?> parameters = this.getParameters();
            return new RelationalRepositoryQuery(sql, template, mapper, this, computeFetchMethod(), parameters);
        }
    }

    @Override
    protected Parameters<?, ?> createParameters(Method method) {
        return new StreamParameters(method);
    }

    protected Function<Object[], Function<Stream<Object>, Object>> computeFetchMethod() {
        if (mappingType.isAssignableFrom(this.getReturnType().getType())) {
            return obj -> it -> it.findFirst().orElse(null);
        }

        StreamParameters parameters = (StreamParameters) getParameters();
        Parameter functionParameter = parameters.getFunctionParameter();
        if (functionParameter != null) {
            //noinspection unchecked
            return obj -> (Function<Stream<Object>, Object>) obj[functionParameter.getIndex()];
        }
        Function<Stream<Object>, Object> collector = computeCollectionFetchMethod();
        if (collector != null)
            return obj -> collector;
        throw new IllegalStateException("Unsupported fetch method");
    }

    private Function<Stream<Object>, Object> computeCollectionFetchMethod() {
        Class<?> returnType = method.getReturnType();
        if (this.isCollectionQuery()) {
            Collector<Object, ?, ?> collector;
            if (returnType.isAssignableFrom(List.class))
                collector = Collectors.toList();
            else if (returnType.isAssignableFrom(Set.class))
                collector = Collectors.toSet();
            else if (returnType.isArray()) {
                throw new IllegalStateException("Unsupported operation");
            } else {
                collector = Collectors.toList();
            }
            return it -> it.collect(collector);
        } else if (returnType.isAssignableFrom(Stream.class)) {
            throw new IllegalStateException("Cannot return a stream as this must be done in a transaction (consider passing a Function<Stream<A>, B> as argument).");
        } else if (returnType.isAssignableFrom(Optional.class)) {
            return Stream::findFirst;
        }
        return null;
    }
}
