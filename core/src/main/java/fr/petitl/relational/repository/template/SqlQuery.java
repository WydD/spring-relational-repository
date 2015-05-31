package fr.petitl.relational.repository.template;

import java.sql.SQLSyntaxErrorException;
import java.util.*;

/**
 *
 */
public class SqlQuery {

    public static enum ParameterType {
        NAMED_PARAMETER,
        POSITIONAL,
        QUESTION_MARKS,
        NONE
    }

    private static final String SEPARATORS = " \n\r\f\t,()=<>&|+-=/*'^![]#~\\{}";

    private ParameterType type;
    private String original;
    private String nativeSql;

    private Map<String, List<Integer>> namedParameters;
    private Map<Integer, List<Integer>> positionParameters;
    private int numberOfArguments;

    protected SqlQuery(ParameterType type, String original, String nativeSql,
                       Map<String, List<Integer>> namedParameters, Map<Integer, List<Integer>> positionParameters, int numberOfArguments) {
        this.type = type;
        this.original = original;
        this.nativeSql = nativeSql;
        this.namedParameters = namedParameters;
        this.positionParameters = positionParameters;
        this.numberOfArguments = numberOfArguments;
    }

    public List<Integer> resolve(String name) {
        if (type != ParameterType.NAMED_PARAMETER)
            throw new IllegalStateException("Resolving a named parameter on a non-named query");
        List<Integer> result = namedParameters.get(name);
        if (result == null)
            throw new IllegalArgumentException("Unknown named parameter [" + name + "]");
        return result;
    }

    public List<Integer> resolve(int position) {
        if (type == ParameterType.NONE)
            throw new IllegalStateException("Trying to resolve a position on a query without parameters");
        if (type == ParameterType.QUESTION_MARKS) {
            if (position > this.numberOfArguments)
                throw new IllegalArgumentException("Unknown position parameter [" + position + "]");

            return Arrays.asList(position);
        }
        if (type == ParameterType.POSITIONAL) {
            List<Integer> result = positionParameters.get(position);
            if (result == null)
                throw new IllegalArgumentException("Unknown position parameter [" + position + "]");
            return result;
        }
        throw new IllegalStateException("Resolving a position parameter on a named query");
    }

    public ParameterType getType() {
        return type;
    }

    public String getOriginal() {
        return original;
    }

    public String getNativeSql() {
        return nativeSql;
    }

    public int getNumberOfArguments() {
        return numberOfArguments;
    }

    public static SqlQuery parse(String sql) throws SQLSyntaxErrorException {
        int length = sql.length();

        boolean inQuote = false;
        boolean escape = false;

        StringBuilder builder = new StringBuilder();
        ParameterType type = ParameterType.NONE;
        int parameterCount = 1;
        Map<String, List<Integer>> namedParameters = null;
        Map<Integer, List<Integer>> positionParameters = null;

        for (int idx = 0; idx < length; idx++) {
            char c = sql.charAt(idx);
            if (inQuote) {
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '\'') {
                    inQuote = false;
                }
                builder.append(c);
                continue;
            }
            if (c == '\'') {
                inQuote = true;
                builder.append(c);
                continue;
            }
            if (c == ':') {
                if (type != ParameterType.NONE && type != ParameterType.NAMED_PARAMETER)
                    throw new SQLSyntaxErrorException("Mixing named parameters and interrogation marks");
                if (namedParameters == null)
                    namedParameters = new HashMap<>();

                String parameterName = extract(idx + 1, length, sql);
                if (parameterName.isEmpty()) {
                    throw new SQLSyntaxErrorException("Invalid characters after parameter prefix ':'@" + (idx + 1) + " [" + sql + "]");
                }
                type = ParameterType.NAMED_PARAMETER;
                builder.append('?');
                List<Integer> resolve = namedParameters.get(parameterName);
                if (resolve == null) {
                    resolve = new LinkedList<>();
                    namedParameters.put(parameterName, resolve);
                }
                resolve.add(parameterCount++);
                idx += parameterName.length();
                continue;
            }
            if (c == '?') {
                String parameterName = extract(idx + 1, length, sql);
                if (parameterName.isEmpty()) {
                    if (type != ParameterType.NONE && type != ParameterType.QUESTION_MARKS)
                        throw new SQLSyntaxErrorException("Mixing named parameters and interrogation marks");
                    type = ParameterType.QUESTION_MARKS;
                    builder.append('?');
                    parameterCount++;
                } else {
                    if (positionParameters == null)
                        positionParameters = new HashMap<>();
                    int parameterIdx;
                    try {
                        parameterIdx = Integer.parseInt(parameterName);
                    } catch (NumberFormatException e) {
                        throw new SQLSyntaxErrorException("Invalid characters after parameter prefix '?' [" + parameterName + "]");
                    }
                    if (type != ParameterType.NONE && type != ParameterType.POSITIONAL)
                        throw new SQLSyntaxErrorException("Mixing named parameters and interrogation marks");
                    type = ParameterType.POSITIONAL;
                    builder.append('?');
                    List<Integer> resolve = positionParameters.get(parameterIdx);
                    if (resolve == null) {
                        resolve = new LinkedList<>();
                        positionParameters.put(parameterIdx, resolve);
                    }
                    resolve.add(parameterCount++);
                }
                idx += parameterName.length();
                continue;
            }
            builder.append(c);
        }
        return new SqlQuery(type, sql, builder.toString(), namedParameters, positionParameters, parameterCount - 1);
    }

    protected static String extract(int start, int length, String sql) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < length; i++) {
            char c = sql.charAt(i);
            if (SEPARATORS.indexOf(c) >= 0)
                break;
            builder.append(c);
        }
        return builder.toString();
    }
}
