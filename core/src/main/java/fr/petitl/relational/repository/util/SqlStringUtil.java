package fr.petitl.relational.repository.util;

/**
 *
 */
public class SqlStringUtil {

    public static String questionMarks(int count) {
        return joinRepetitive(count, "?", ", ");
    }

    public static String joinRepetitive(int count, String repetitive, String join) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0)
                builder.append(join);
            builder.append(repetitive);
        }
        return builder.toString();
    }
}
