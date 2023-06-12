package com.sesamecare.asyncRuleEvaluator;

/**
 * An exception encountered during the running of a filter expression
 */
public class FiltrexRuntimeException extends RuntimeException {
    /**
     * @param message
     */
    FiltrexRuntimeException(String message) {
        super(message);
    }
}
