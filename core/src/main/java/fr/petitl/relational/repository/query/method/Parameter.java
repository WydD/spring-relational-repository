package fr.petitl.relational.repository.query.method;

import java.util.function.Function;

import fr.petitl.relational.repository.query.CollectorFunction;
import org.springframework.core.MethodParameter;

/**
 *
 */
public class Parameter extends org.springframework.data.repository.query.Parameter {

    private final boolean isCollectorFunction;
    private final Class<?> forcedDomainType;

    /**
     * Creates a new {@link Parameter} for the given {@link MethodParameter}.
     *
     * @param parameter must not be {@literal null}.
     */
    protected Parameter(MethodParameter parameter) {
        super(parameter);

        isCollectorFunction = parameter.hasParameterAnnotation(CollectorFunction.class);
        if (isCollectorFunction) {
            if (!Function.class.isAssignableFrom(parameter.getParameterType())) {
                throw new IllegalArgumentException("CollectorFunction parameter is not a function");
            }
            forcedDomainType = parameter.getParameterAnnotation(CollectorFunction.class).value();
        } else {
            forcedDomainType = null;
        }
    }

    public Class<?> getForcedDomainType() {
        return forcedDomainType;
    }

    public boolean isCollectorFunction() {
        return isCollectorFunction;
    }

    @Override
    public boolean isSpecialParameter() {
        return isCollectorFunction || super.isSpecialParameter();
    }
}
