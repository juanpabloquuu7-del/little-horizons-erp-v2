package com.littlehorizons.erp.util;

/**
 * Container of the application's custom unchecked exceptions.
 */
public final class CustomExceptions {

    private CustomExceptions() {
    }

    /** Raised when a database operation fails. */
    public static class DataAccessException extends RuntimeException {
        public DataAccessException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /** Raised when a business rule is violated (message is user-facing, in Spanish). */
    public static class BusinessException extends RuntimeException {
        public BusinessException(String message) {
            super(message);
        }
    }

    /** Raised when a requested entity does not exist. */
    public static class NotFoundException extends BusinessException {
        public NotFoundException(String message) {
            super(message);
        }
    }

    /** Raised when a document (e.g. invoice PDF) cannot be generated. */
    public static class DocumentException extends RuntimeException {
        public DocumentException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
