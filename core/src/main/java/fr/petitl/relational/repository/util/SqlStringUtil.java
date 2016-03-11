package fr.petitl.relational.repository.util;

/**
 *
 */
public class SqlStringUtil {

    public static String questionMarks(int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0)
                builder.append(", ");
            builder.append("?");
        }
        return builder.toString();
    }
}
