package fr.petitl.relational.repository.query.method;

import java.util.function.Function;

import org.springframework.core.MethodParameter;

/**
 *
 */
public class Parameter extends org.springframework.data.repository.query.Parameter {

    private final boolean isFunc;

    /**
     * Creates a new {@link Parameter} for the given {@link MethodParameter}.
     *
     * @param parameter must not be {@literal null}.
     */
    protected Parameter(MethodParameter parameter) {
        super(parameter);

        isFunc = Function.class.isAssignableFrom(parameter.getParameterType());
    }

    public boolean isFunction() {
        return isFunc;
    }

    @Override
    public boolean isSpecialParameter() {
        return isFunc || super.isSpecialParameter();
    }
}
