package fr.petitl.relational.repository.template.bean;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
public class CamelToSnakeConvention implements NamingConvention {

    private String fkPostfix;

    public CamelToSnakeConvention(String fkPostfix) {
        this.fkPostfix = fkPostfix;
    }

    public CamelToSnakeConvention() {
        this("_id");
    }

    public String generateDefaultColumnName(Field field, boolean hasFK) {
        String fieldName = field.getName();
        String colName = camelToSnake(fieldName);
        if (fkPostfix != null) {
            if (hasFK && !colName.endsWith(fkPostfix)) { // createdDate
                colName += fkPostfix;
            }
        }
        return colName;
    }

    /**
     * Stolen and majorly readapted from the StringUtils from commons-lang from Apache
     * <p>
     * Main adjustment: camel case identifiers are iso, and
     */
    protected static String camelToSnake(String str) {
        char[] c = str.toCharArray();
        List<String> list = new LinkedList<>();
        int tokenStart = 0;
        // Manage the "_field" case
        if (c[tokenStart] == '_') {
            list.add("");
            tokenStart += 1;
        }
        int currentType = Character.getType(c[tokenStart]);
        for (int pos = tokenStart + 1; pos < c.length; pos++) {
            char currentChar = c[pos];
            // If a '_' is found
            if (currentChar == '_') {
                // Split the chain
                list.add(new String(c, tokenStart, pos - tokenStart));
                // Skip the current character
                pos += 1;
                tokenStart = pos;
                if (pos >= c.length) // oob safety
                    break;
                // Reinit the current type
                currentType = Character.getType(c[tokenStart]);
                // In "_hello", tokenStart points to 'h' and the next iteration of pos with be on 'e'
                continue;
            }
            int type = Character.getType(currentChar);
            if (type == currentType) {
                continue;
            }
            if (type == Character.LOWERCASE_LETTER && currentType == Character.UPPERCASE_LETTER) {
                int newTokenStart = pos - 1;
                if (newTokenStart != tokenStart) {
                    list.add(new String(c, tokenStart, newTokenStart - tokenStart));
                    tokenStart = newTokenStart;
                }
            } else {
                list.add(new String(c, tokenStart, pos - tokenStart));
                tokenStart = pos;
            }
            currentType = type;
        }
        list.add(new String(c, tokenStart, c.length - tokenStart));
        return list.stream().collect(Collectors.joining("_")).toLowerCase();
    }
}
