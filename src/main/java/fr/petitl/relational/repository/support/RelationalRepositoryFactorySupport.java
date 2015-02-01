package fr.petitl.relational.repository.support;

import java.io.Serializable;

import fr.petitl.relational.repository.query.JdbcQueryMethod;
import fr.petitl.relational.repository.query.Query;
import fr.petitl.relational.repository.template.RelationalTemplateBak;
import fr.petitl.relational.repository.repository.SimpleRelationalRepository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;

/**
 *
 */
public class RelationalRepositoryFactorySupport extends RepositoryFactorySupport {

    private RelationalTemplateBak operations;

    public RelationalRepositoryFactorySupport(RelationalTemplateBak operations) {
        this.operations = operations;
    }

    @Override
    public <T, ID extends Serializable> RelationalEntityInformation<T, ID> getEntityInformation(Class<T> c) {
        return new RelationalEntityInformation<>(c);
    }

    @Override
    protected Object getTargetRepository(RepositoryMetadata metadata) {
        return new SimpleRelationalRepository<>(getEntityInformation(metadata.getDomainType()), operations);
    }

    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata repositoryMetadata) {
        return SimpleRelationalRepository.class;
    }

    @Override
    protected QueryLookupStrategy getQueryLookupStrategy(QueryLookupStrategy.Key key, EvaluationContextProvider evaluationContextProvider) {
        return (method, metadata, namedQueries) -> {
            JdbcQueryMethod queryMethod = new JdbcQueryMethod(method, metadata);

            Query annotation = queryMethod.getAnnotation();
            PartTree  p = new PartTree(method.getName(), metadata.getDomainType());
            for (PartTree.OrPart orPart : p) {
                orPart.toString();
                for (Part part : orPart) {
                    part.getProperty();
                }
            }

            return null;
        };
    }
}
