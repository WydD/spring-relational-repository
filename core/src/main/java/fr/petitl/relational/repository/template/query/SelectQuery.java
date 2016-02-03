package fr.petitl.relational.repository.template.query;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.petitl.relational.repository.template.RelationalTemplate;
import fr.petitl.relational.repository.template.RowMapper;
import org.springframework.data.domain.Pageable;

/**
 *
 */
public class SelectQuery<E> extends AbstractQuery<SelectQuery<E>> {
    protected RowMapper<E> mapper;
    protected Pageable pageable;

    public SelectQuery(String sql, RelationalTemplate template, RowMapper<E> mapper) {
        super(sql, template);
        this.mapper = mapper;
    }

    public <F> F fetch(Function<Stream<E>, F> collectorFunction) {
        return template.executeQuery(query.getQueryString(), mapper, collectorFunction, query);
    }

    public List<E> list() {
        return fetch(stream -> stream.collect(Collectors.toList()));
    }

    public E findOne() {
        return fetch(stream -> stream.findFirst().orElse(null));
    }
}
