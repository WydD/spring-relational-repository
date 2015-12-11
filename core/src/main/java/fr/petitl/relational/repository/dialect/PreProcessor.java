package fr.petitl.relational.repository.dialect;

import fr.petitl.relational.repository.template.AbstractQuery;
import fr.petitl.relational.repository.template.ColumnMapper;

import java.util.List;
import java.util.function.Function;

/**
 * Created by loic on 11/12/2015.
 */
public interface PreProcessor {
    List<Executor> process(String sql);

    interface Executor {
        int from();
        int to();
        int parameterCount();

        void setParameter(int position, Object parameter, Function<Object, ColumnMapper> defaultSetter);
        AbstractQuery.PrepareStep getPrepareStep();
        String getFragment();
        int getFinalParameterCount();
    }
}
