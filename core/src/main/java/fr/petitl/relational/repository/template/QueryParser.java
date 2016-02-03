package fr.petitl.relational.repository.template;

import java.sql.SQLSyntaxErrorException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import fr.petitl.relational.repository.query.parametered.FullQuery;
import fr.petitl.relational.repository.query.parametered.ParameteredQueryPart;
import fr.petitl.relational.repository.query.parametered.SingleParameterQueryPart;
import fr.petitl.relational.repository.query.parametered.StringQueryPart;

/**
 *
 */
public class QueryParser {

    private FullQuery query;

    public enum ParameterType {
        NAMED_PARAMETER,
        POSITIONAL,
        QUESTION_MARKS,
        NONE
    }

    private int i = 0;
    private int length;
    private String sql;
    private boolean readable;
    private Map<String, Integer> namedParameterIndex = new HashMap<>();
    private int index = 0;
    private List<ParameteredQueryPart> queryParts = new LinkedList<>();
    private ParameterType parameterType = ParameterType.NONE;

    public QueryParser(String sql) throws SQLSyntaxErrorException {
        this.sql = sql;
        length = sql.length();
        readable = length > 0;
        parse();
    }

    private void parse() throws SQLSyntaxErrorException {
        StringBuilder plain = new StringBuilder();
        while (readable) {
            char c = read();
            if (c == '\'') {
                plain.append(stringLiteral());
            } else if (c == '?') {
                queryParts.add(new StringQueryPart(plain.toString()));
                plain = new StringBuilder();
                int n = integer();
                if (n < 0) {
                    selectParameterType(ParameterType.QUESTION_MARKS);
                    n = index++;
                } else {
                    selectParameterType(ParameterType.POSITIONAL);
                }
                queryParts.add(new SingleParameterQueryPart(n));
            } else if (c == ':') {
                queryParts.add(new StringQueryPart(plain.toString()));
                plain = new StringBuilder();
                String ident = ident();
                Integer i = namedParameterIndex.get(ident);
                if (i == null) {
                    namedParameterIndex.put(ident, index);
                    i = index++;
                }
                selectParameterType(ParameterType.NAMED_PARAMETER);
                queryParts.add(new SingleParameterQueryPart(i));
            } else {
                plain.append(c);
            }
        }
        if (plain.length() > 0) {
            queryParts.add(new StringQueryPart(plain.toString()));
        }
        query = new FullQuery(queryParts);
    }

    private void selectParameterType(ParameterType parameterType) throws SQLSyntaxErrorException {
        if (this.parameterType != ParameterType.NONE && this.parameterType != parameterType) {
            throw new SQLSyntaxErrorException("Impossible to mix parameter " + this.parameterType + " and " + parameterType);
        }
        this.parameterType = parameterType;
    }

    private String ident() {
        StringBuilder plain = new StringBuilder();
        while (readable) {
            char c = current();
            if (c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_') {
                plain.append(c);
            } else {
                break;
            }
            advance();
        }
        return plain.toString();
    }

    private int integer() {
        int n = 0;
        boolean first = true;
        while (readable) {
            char c = current();
            if (c >= '0' && c <= '9') {
                n = n * 10 + (c - '0');
            } else {
                if (first)
                    return -1;
                break;
            }
            advance();
            first = false;
        }
        if (first)
            return -1;
        return n;
    }


    private char current() {
        return sql.charAt(i);
    }

    private char read() {
        char c = current();
        advance();
        return c;
    }

    private boolean advance() {
        readable = ++i < length;
        return readable;
    }

    private String stringLiteral() throws SQLSyntaxErrorException {
        StringBuilder plain = new StringBuilder("'");
        while (readable) {
            char c = current();
            plain.append(c);
            if (c == '\\') {
                if (!advance()) {
                    throw new SQLSyntaxErrorException("Unparsable sql, expected a char after \\ in a string but EOF encountered");
                }
                plain.append(current());
            } else if (c == '\'') {
                advance();
                return plain.toString();
            }
            advance();
        }
        throw new SQLSyntaxErrorException("Unclosed string literal");
    }

    public ParameterType getParameterType() {
        return parameterType;
    }

    public List<ParameteredQueryPart> getQueryParts() {
        return queryParts;
    }

    public FullQuery getQuery() {
        return query;
    }

    public int resolve(String name) {
        if (parameterType != ParameterType.NAMED_PARAMETER) {
            throw new IllegalStateException("Setting a named parameter in query that is not a named parameter [" + parameterType + "]");
        }
        Integer index = namedParameterIndex.get(name);
        if (index == null) {
            throw new IllegalArgumentException("Unknown named parameter " + name + ", unable to resolve parameter number");
        }
        return index;
    }

    public Map<String, Integer> getNamedParameterIndex() {
        return namedParameterIndex;
    }
}
