package fr.petitl.relational.repository.support;

import java.io.Serializable;

import fr.petitl.relational.repository.query.RelationalRepositoryQueryMethod;
import fr.petitl.relational.repository.query.Query;
import fr.petitl.relational.repository.template.RelationalTemplate;
import fr.petitl.relational.repository.repository.SimpleRelationalRepository;
import fr.petitl.relational.repository.template.bean.MappingFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.QueryLookupStrategy;

/**
 *
 */
public class RelationalRepositoryFactorySupport extends RepositoryFactorySupport {

    private RelationalTemplate operations;

    public RelationalRepositoryFactorySupport(RelationalTemplate operations) {
        this.operations = operations;
    }

    @Override
    public <T, ID extends Serializable> RelationalEntityInformation<T, ID> getEntityInformation(Class<T> c) {
        return new RelationalEntityInformation<>(MappingFactory.beanMapping(c));
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
            RelationalRepositoryQueryMethod queryMethod = new RelationalRepositoryQueryMethod(method, metadata, operations);

            Query annotation = queryMethod.getAnnotation();
            if (annotation == null) {
                throw new IllegalStateException("Methods without @Query are not supported yet.");
//                PartTree p = new PartTree(method.getName(), metadata.getDomainType());
//                for (PartTree.OrPart orPart : p) {
//                    orPart.toString();
//                    for (Part part : orPart) {
//                        part.getProperty();
//                    }
//                }
            } else {
                return queryMethod.createAnnotationBased();
            }
        };
    }
}
