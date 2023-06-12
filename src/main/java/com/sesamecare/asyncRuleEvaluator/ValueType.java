package com.sesamecare.asyncRuleEvaluator;

/**
 * The different types of values that can be used in a Filtrex expression
 */
public enum ValueType {
    /**
     * A boolean value
     */
    BOOLEAN,
    /**
     * A string value, not including null
     */
    STRING,
    /**
     * Not defined/null
     */
    NULL,
    /**
     * An arbitrary precision decimal
     */
    DECIMAL,
    /**
     * An array of values
     */
    ARRAY,
    /**
     * A map of strings to values
     */
    MAP,
    /**
     * A function that is evaluated whenever a value (or data about a value) is needed
     */
    FUNCTION,
    /**
     * A function that is evaluated once and then cached
     */
    MEMOIZED
};
