package fr.petitl.relational.repository.query.method;

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.data.repository.query.Parameters;

/**
 *
 */
public class StreamParameters extends Parameters<StreamParameters, Parameter> {

    private Parameter collectorParameter;

    public StreamParameters(Method method) {
        super(method);
        Class<?>[] types = method.getParameterTypes();
        for (int i = 0; i < types.length; i++) {
            Parameter parameter = getParameter(i);
            if (parameter.isCollectorFunction()) {
                collectorParameter = parameter;
            }
        }
    }

    public StreamParameters(List<Parameter> originals) {
        super(originals);
    }

    public Parameter getCollectorParameter() {
        return collectorParameter;
    }

    /*
             * (non-Javadoc)
             * @see org.springframework.data.repository.query.Parameters#createParameter(org.springframework.core.MethodParameter)
             */
    @Override
    protected Parameter createParameter(MethodParameter parameter) {
        return new Parameter(parameter);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.query.Parameters#createFrom(java.util.List)
     */
    @Override
    protected StreamParameters createFrom(List<Parameter> parameters) {
        return new StreamParameters(parameters);
    }
}
