package fr.petitl.relational.repository.template;

import java.sql.SQLSyntaxErrorException;
import java.util.*;
import java.util.stream.Collectors;

import fr.petitl.relational.repository.query.macro.MacroFunction;
import fr.petitl.relational.repository.query.parametered.*;

/**
 *
 */
public class QueryParser {

    public static final HashSet<Character> OUT_OF_PARAMETER_CHARS = new HashSet<>(Arrays.asList(';', '}'));
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
    private RelationalTemplate template;
    private Map<String, MacroFunction> allowedMacros;
    private boolean readable;
    private Map<String, Integer> namedParameterIndex = new HashMap<>();
    private int index = 0;
    private List<ParameteredQueryPart> queryParts = new LinkedList<>();
    private ParameterType parameterType = ParameterType.NONE;

    public QueryParser(String sql, RelationalTemplate template) throws SQLSyntaxErrorException {
        this.sql = sql;
        this.template = template;
        // indexed macros
        this.allowedMacros = template.getAvailableMacros().stream().collect(Collectors.toMap(it -> it.name() + "/" + it.numberOfParameters(), it -> it));
        length = sql.length();
        readable = length > 0;
        parse();
    }

    private void parse() throws SQLSyntaxErrorException {
        parseMain(queryParts, null);
        query = new FullQuery(queryParts);
    }

    private void parseMain(List<ParameteredQueryPart> queryParts, Set<Character> outChars) throws SQLSyntaxErrorException {
        StringBuilder plain = new StringBuilder();
        while (readable) {
            char c = current();
            if (outChars != null && outChars.contains(c)) {
                break;
            }
            advance();
            if (c == '\'') {
                plain.append(stringLiteral());
            } else if (c == '?') {
                plain = appendStringPart(queryParts, plain);
                int n = integer();
                if (n < 0) {
                    selectParameterType(ParameterType.QUESTION_MARKS);
                    n = index++;
                } else {
                    selectParameterType(ParameterType.POSITIONAL);
                }
                queryParts.add(new SingleParameterQueryPart(n));
            } else if (c == '#') {
                plain = appendStringPart(queryParts, plain);
                String macroName = ident();
                List<List<ParameteredQueryPart>> parameters = parameters();
                MacroFunction macro = allowedMacros.get(macroName.toLowerCase() + "/" + parameters.size());
                if (macro == null) {
                    // Try variable size
                    macro = allowedMacros.get(macroName.toLowerCase() + "/-1");
                    if (macro == null) {
                        throw new SQLSyntaxErrorException("No macro matches signature " + macroName + "/" + parameters.size());
                    }
                }
                queryParts.add(macro.build(parameters, template));
            } else if (c == ':') {
                plain = appendStringPart(queryParts, plain);
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
    }

    private StringBuilder appendStringPart(List<ParameteredQueryPart> queryParts, StringBuilder plain) {
        if (plain.length() > 0)
            queryParts.add(new StringQueryPart(plain.toString()));
        plain = new StringBuilder();
        return plain;
    }

    private List<List<ParameteredQueryPart>> parameters() throws SQLSyntaxErrorException {
        List<List<ParameteredQueryPart>> result = new LinkedList<>();
        space();
        if (current() == '{') {
            advance();
        } else {
            // No parameters
            return result;
        }
        if (current() == '}') {
            // Empty parameters list
            advance();
            return result;
        }
        while (readable) {
            result.add(parameter());

            char c = current();
            if (c == '}') {
                advance();
                break;
            } else if (c == ';') {
                advance();
            }
        }
        return result;
    }

    private void space() throws SQLSyntaxErrorException {
        while (readable) {
            char c = current();
            if (Character.isSpaceChar(c)) {
                advance();
            } else {
                return;
            }
        }
    }

    private List<ParameteredQueryPart> parameter() throws SQLSyntaxErrorException {
        space();
        LinkedList<ParameteredQueryPart> parameterParts = new LinkedList<>();
        parseMain(parameterParts, OUT_OF_PARAMETER_CHARS);
        ParameteredQueryPart part = parameterParts.get(parameterParts.size() - 1);
        // we dont want empty parameters
        if (part instanceof StringQueryPart) {
            String f = ((StringQueryPart) part).trimFragment();
            // If we have trimed to void, remove. Can happen in '? '
            if (f.isEmpty()) {
                parameterParts.remove(parameterParts.size() - 1);
            }
        }
        return parameterParts;
    }

    private void selectParameterType(ParameterType parameterType) throws SQLSyntaxErrorException {
        if (this.parameterType != ParameterType.NONE && this.parameterType != parameterType) {
            throw new SQLSyntaxErrorException("Impossible to mix parameter " + this.parameterType + " and " + parameterType);
        }
        this.parameterType = parameterType;
    }

    private String ident() throws SQLSyntaxErrorException {
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

    private int integer() throws SQLSyntaxErrorException {
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


    private char current() throws SQLSyntaxErrorException {
        if (i >= sql.length()) {
            throw new SQLSyntaxErrorException("Encountered unexpected EOF");
        }
        return sql.charAt(i);
    }

    private char read() throws SQLSyntaxErrorException {
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
