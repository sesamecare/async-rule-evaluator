package com.sesamecare.asyncRuleEvaluator;

import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A flexible value type used during the execution of Filtrex expressions
 * and as input to a Filtrex expression
 */
public class Value implements Comparable<Value> {
    /**
     * Compare two values with Javascript-like type conversion
     * @param that the object to be compared.
     * @return -1, 0, or 1 if this value is less than, equal to, or greater than the other value
     */
    @Override
    public int compareTo(Value that) {
        var me = this.resolve();
        var them = that.resolve();
        if (me.canBeDecimal() && them.canBeDecimal()) {
            return me.asDecimal().compareTo(them.asDecimal());
        }
        return StringUtils.compare(me.toString(), them.toString());
    }

    private ValueType type;

    private boolean bool;
    private String str;

    private BigDecimal decimal;

    private List<Value> array;

    private Map<String, Value> map;

    private Function<List<Value>, Value> func;

    private Value(boolean b) {
        this.type = ValueType.BOOLEAN;
        this.bool = b;
    }

    /**
     * Create a new Value from a map of strings to values
     * @param map the map to be used
     */
    public Value(Map<String, Value> map) {
        this.type = ValueType.MAP;
        this.map = map;
    }

    /**
     * Create a new Value from a list of values
     * @param arr the list to be used
     */
    public Value(List<Value> arr) {
        this.type = ValueType.ARRAY;
        this.array = arr;
    }

    /**
     * Create a new Value from an integer (becomes a BigDecimal)
     * @param i the integer to be converted
     */
    public Value(int i) {
        this.type = ValueType.DECIMAL;
        this.decimal = BigDecimal.valueOf(i);
    }

    private Value() {
        this.type = ValueType.NULL;
    }

    /**
     * Create a new Value from a string
     * @param s the string to be used
     */
    public Value(String s) {
        this.type = ValueType.STRING;
        this.str = s;
    }

    /**
     * Create a new Value from a decimal
     * @param d the decimal to be used
     */
    public Value(BigDecimal d) {
        this.type = ValueType.DECIMAL;
        this.decimal = d;
    }

    /**
     * Create a new Value from a double (becomes a BigDecimal)
     * @param d the double to be converted
     */
    public Value(double d) {
        this.type = ValueType.DECIMAL;
        this.decimal = BigDecimal.valueOf(d);
    }

    /**
     * Create a new Value from a function that will be run once and then the value
     * will "become" the output of the run
     * @param f the function to be called, once when requested for the first time
     * @return a new Value
     */
    public static Value memoized(Function<List<Value>, Value> f) {
        var memo = new Value();
        memo.type = ValueType.MEMOIZED;
        memo.func = f;
        return memo;
    }

    /**
     * Create a new Value from a function that will be called each time the value
     * of the function is needed. Note that it can also be called multiple times
     * when trying to inspect what type of value it is.
     * @param f the function to be called
     * @return a new Value
     */
    public static Value func(Function<List<Value>, Value> f) {
        var memo = new Value();
        memo.type = ValueType.FUNCTION;
        memo.func = f;
        return memo;
    }

    /**
     * Used when this value is called as a function in the filter source,
     * as opposed to automatic resolution when referenced as a value. Note
     * that if the function was a memoized function and has already been
     * resolved, this will throw because it is no longer a function.
     * @param args the arguments to the function
     * @return a new Value, always called (not cached)
     */
    public Value apply(List<Value> args) {
        // TODO consider allowing an already-memoized function call with no arguments to just
        // return the value instead of throwing
        if (this.type != ValueType.FUNCTION) {
            throw new FiltrexRuntimeException("Cannot apply non-function type");
        }
        return this.func.apply(args);
    }

    Value resolve() {
        if (this.type == ValueType.MEMOIZED) {
            var resolved = this.func.apply(Value.EMPTY.array);
            // TODO is there a nicer way to do a brain transplant?
            this.type = resolved.type;
            this.bool = resolved.bool;
            this.map = resolved.map;
            this.str = resolved.str;
            this.array = resolved.array;
            this.decimal = resolved.decimal;
            this.func = resolved.func;
        } else if (this.type == ValueType.FUNCTION) {
            return this.func.apply(Value.EMPTY.array);
        }
        return this;
    }

    /**
     * Return the array or throw if the type is not array after resolving function type values
     * @return the array
     * @throws FiltrexRuntimeException if the type is not array
     */
    public List<Value> getArray() throws FiltrexRuntimeException {
        var resolved = this.resolve();
        if (resolved.type != ValueType.ARRAY) {
            throw new FiltrexRuntimeException("Invalid conversion of non-array type");
        }
        return resolved.array;
    }

    /**
     * Return the value as a map or throw if it is not a map after resolving function type values
     * @return the map
     * @throws FiltrexRuntimeException if the type is not array
     */
    public Map<String, Value> getMap() throws FiltrexRuntimeException {
        var resolved = this.resolve();
        if (resolved.type != ValueType.MAP) {
            throw new FiltrexRuntimeException("Invalid conversion of non-map type");
        }
        return resolved.map;
    }

    /**
     * Return the type of this value with no function resolution (i.e. memoized will stay memoized)
     * @return the type. note that null strings will be type "null"
     */
    public ValueType getType() {
        if (this.type == ValueType.STRING && this.str == null) {
            return ValueType.NULL;
        }
        return type;
    }

    /**
     * Return the value as a boolean, using Javascript like rules
     * @return the boolean value
     */
    public boolean asBoolean() {
        var resolved = this.resolve();
        switch (resolved.type) {
            case BOOLEAN:
                return resolved.bool;
            case DECIMAL:
                return resolved.decimal.compareTo(BigDecimal.ZERO) != 0;
            case NULL:
                return false;
            case STRING:
                return resolved.str != null && resolved.str.length() > 0;
            case ARRAY:
                return true;
            case MAP:
                throw new RuntimeException("Invalid conversion of map to boolean");
        }
        return false;
    }

    /**
     * Return the value as a string, or null if it is null
     * @return the string value
     */
    @Override
    public String toString() {
        var resolved = this.resolve();
        switch (type) {
            case BOOLEAN:
                return Boolean.toString(resolved.bool);
            case NULL:
                return null;
            case STRING:
                return resolved.str;
            case DECIMAL:
                return resolved.decimal.stripTrailingZeros().toString();
            case ARRAY:
                return resolved.array.stream().map(Value::toString).collect(Collectors.joining(","));
            case MAP:
                return "{" + resolved.map.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue().toString()).collect(Collectors.joining(",")) + "}";
        }
        throw new FiltrexRuntimeException("Unknown type");
    }

    /**
     * Return the value as an array by casting to a single element array if necessary
     * @return the array value
     */
    public List<Value> asArray() {
        var resolved = this.resolve();
        if (resolved.type == ValueType.ARRAY) {
            return resolved.array;
        }
        var list = new ArrayList<Value>();
        list.add(resolved);
        return list;
    }

    /**
     * Return the value as a decimal, or zero if it is null
     * @return The value as a decimal, if possible
     * @throws FiltrexRuntimeException if the value cannot be converted to a decimal (essentially a non numeric string, array, or map)
     */
    public BigDecimal asDecimal() throws FiltrexRuntimeException {
        var resolved = this.resolve();
        switch (type) {
            case BOOLEAN:
                return resolved.bool ? BigDecimal.ONE : BigDecimal.ZERO;
            case NULL:
                return BigDecimal.ZERO;
            case STRING:
                return new BigDecimal(resolved.str);
            case DECIMAL:
                return resolved.decimal;
            case ARRAY:
                throw new FiltrexRuntimeException("Invalid conversion of array to decimal");
            case MAP:
                throw new FiltrexRuntimeException("Invalid conversion of map to decimal");
        }
        return BigDecimal.ZERO;
    }

    /**
     * Return true if this value can be converted to a decimal (resolves if necessary)
     * @return true if this value can be converted to a decimal
     */
    public boolean canBeDecimal() {
        var resolved = this.resolve();
        if (resolved.type == ValueType.DECIMAL || resolved.type == ValueType.BOOLEAN) {
            return true;
        }
        if (resolved.type == ValueType.STRING) {
            try {
                Double.valueOf(resolved.str);
                return true;
            } catch (NumberFormatException n) {
                return false;
            }
        }
        return false;
    }

    /**
     * Return true if this value can be found in "other" which will be treated as an array or converted to one
     * @param other An array or non-array value that will be considered a single value array
     * @param exactMatch Whether to convert types to check the match or just compare the values
     * @return true if this value can be found in "other"
     */
    public Value in(Value other, boolean exactMatch) {
        // NOTE: this means that the following is true:
        // [[1]] in [1]
        var resolved = this.resolve();
        if (resolved.type == ValueType.ARRAY) {
            for (var v : resolved.getArray()) {
                if (!v.in(other, exactMatch).asBoolean()) {
                    return Value.FALSE;
                }
            }
            return Value.TRUE;
        }
        var otherArray = other.asArray();
        for (var v : otherArray) {
            if ((!exactMatch || v.type == this.type) && v.compareTo(this) == 0) {
                return Value.TRUE;
            }
        }
        return Value.FALSE;
    }

    static Value NULL = new Value();
    static Value EMPTY = new Value(new ArrayList<>());
    static Value TRUE = new Value(true);
    static Value FALSE = new Value(false);
}
