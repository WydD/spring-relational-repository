package fr.petitl.relational.repository.repository;

/**
 *
 */
public class StreamSqlException extends RuntimeException {
    public StreamSqlException() {
    }

    public StreamSqlException(String message) {
        super(message);
    }

    public StreamSqlException(String message, Throwable cause) {
        super(message, cause);
    }

    public StreamSqlException(Throwable cause) {
        super(cause);
    }

    public StreamSqlException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
