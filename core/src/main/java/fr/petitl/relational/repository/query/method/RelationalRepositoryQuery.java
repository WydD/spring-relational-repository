package fr.petitl.relational.repository.query.method;

import java.util.function.Function;
import java.util.stream.Stream;

import fr.petitl.relational.repository.template.QueryParser;
import fr.petitl.relational.repository.template.RelationalTemplate;
import fr.petitl.relational.repository.template.RowMapper;
import fr.petitl.relational.repository.template.query.AbstractQuery;
import fr.petitl.relational.repository.template.query.PagedSelectQuery;
import fr.petitl.relational.repository.template.query.SelectQuery;
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
    private final AbstractQuery selectQuery;

    public RelationalRepositoryQuery(String sql, RelationalTemplate template,
                                     RowMapper<S> mapper, QueryMethod method,
                                     Function<Object[], Function<Stream<S>, S>> fetchMethod,
                                     Parameters<?, ?> parametersConfiguration) {
        this.method = method;
        this.fetchMethod = fetchMethod;
        this.parametersConfig = parametersConfiguration;
        if (parametersConfig.hasPageableParameter()) {
            selectQuery = new PagedSelectQuery<>(sql, template, mapper);
        } else {
            selectQuery = new SelectQuery<>(sql, template, mapper);
        }
        // Quick check
        if (selectQuery.getQuery().getNumberOfArguments() != parametersConfig.getBindableParameters().getNumberOfParameters()) {
            throw new IllegalMappingException("Number of argument does not match between the given queries and the method signature: " + method.toString());
        }
        // Try to resolve everything for named parameter consistency check
        if (selectQuery.getParsedQuery().getParameterType() == QueryParser.ParameterType.NAMED_PARAMETER) {
            for (Parameter parameter : parametersConfiguration.getBindableParameters()) {
                try {
                    selectQuery.getParsedQuery().resolve(parameter.getName());
                } catch (IllegalArgumentException | IllegalStateException e) {
                    throw new IllegalMappingException("Impossible to map parameter " + parameter.toString() + " in method " + method.toString(), e);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object execute(Object[] parameters) {
        selectQuery.clearParameters();
        for (Parameter parameter : parametersConfig.getBindableParameters()) {
            Object value = parameters[parameter.getIndex()];
            if (parameter.isNamedParameter()) {
                selectQuery.setParameter(parameter.getName(), value);
            } else {
                selectQuery.setParameter(parameter.getIndex(), value);
            }
        }
        int pageableIndex = parametersConfig.getPageableIndex();
        if (pageableIndex >= 0) {
            Pageable pageable = (Pageable) parameters[pageableIndex];
            PagedSelectQuery<S> selectQuery = (PagedSelectQuery<S>) this.selectQuery;
            selectQuery.setPageable(pageable);
            return selectQuery.fetch();
        }

        return ((SelectQuery<S>) selectQuery).fetch(fetchMethod.apply(parameters));
    }

    @Override
    public QueryMethod getQueryMethod() {
        return method;
    }
}
