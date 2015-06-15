package fr.petitl.relational.repository.query.method;

import java.util.function.Function;
import java.util.stream.Stream;

import fr.petitl.relational.repository.template.RelationalQuery;
import fr.petitl.relational.repository.template.RelationalTemplate;
import fr.petitl.relational.repository.template.RowMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mapping.model.IllegalMappingException;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;

/**
 *
 */
public class RelationalRepositoryQuery<S> implements RepositoryQuery {
    private final QueryMethod method;
    private Function<Object[], Function<Stream<S>, S>> fetchMethod;
    private Parameters<?, ?> parametersConfig;
    private final RelationalQuery<S> relationalQuery;

    public RelationalRepositoryQuery(String sql, RelationalTemplate template,
                                     RowMapper<S> mapper, QueryMethod method,
                                     Function<Object[], Function<Stream<S>, S>> fetchMethod,
                                     Parameters<?, ?> parametersConfiguration) {
        this.method = method;
        this.fetchMethod = fetchMethod;
        this.parametersConfig = parametersConfiguration;
        relationalQuery = new RelationalQuery<>(sql, template, mapper);
        // Quick check
        if (relationalQuery.getSql().getNumberOfArguments() != parametersConfig.getBindableParameters().getNumberOfParameters()) {
            throw new IllegalMappingException("Number of argument does not match between the given queries and the method signature: " + method.toString());
        }
        // Try to resolve everything
        for (Parameter parameter : parametersConfiguration.getBindableParameters()) {
            try {
                if (parameter.isNamedParameter()) {
                    relationalQuery.getSql().resolve(parameter.getName());
                } else {
                    relationalQuery.getSql().resolve(parameter.getIndex());
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                throw new IllegalMappingException("Impossible to map parameter " + parameter.toString() + " in method " + method.toString(), e);
            }
        }
    }

    @Override
    public Object execute(Object[] parameters) {
        for (Parameter parameter : parametersConfig.getBindableParameters()) {
            Object value = parameters[parameter.getIndex()];
            if (parameter.isNamedParameter()) {
                relationalQuery.setParameter(parameter.getName(), value);
            } else {
                relationalQuery.setParameter(parameter.getIndex(), value);
            }
        }
        int pageableIndex = parametersConfig.getPageableIndex();
        if (pageableIndex >= 0) {
            Pageable pageable = (Pageable) parameters[pageableIndex];
            relationalQuery.setPageable(pageable);
            return relationalQuery.page();
        }
        return relationalQuery.fetch(fetchMethod.apply(parameters));
    }

    @Override
    public QueryMethod getQueryMethod() {
        return method;
    }
}