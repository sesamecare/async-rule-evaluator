package com.sesamecare.asyncRuleEvaluator;

import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Value implements Comparable<Value> {
    @Override
    public int compareTo(Value that) {
        var me = this.resolve();
        var them = that.resolve();
        if (me.canBeDecimal() && them.canBeDecimal()) {
            return me.asDecimal().compareTo(them.asDecimal());
        }
        return StringUtils.compare(me.asStringOrNull(), them.asStringOrNull());
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

    public Value(Map<String, Value> map) {
        this.type = ValueType.MAP;
        this.map = map;
    }

    public Value(List<Value> arr) {
        this.type = ValueType.ARRAY;
        this.array = arr;
    }

    public Value(int i) {
        this.type = ValueType.DECIMAL;
        this.decimal = BigDecimal.valueOf(i);
    }

    private Value() {
        this.type = ValueType.NULL;
    }

    public Value(String s) {
        this.type = ValueType.STRING;
        this.str = s;
    }

    public Value(BigDecimal d) {
        this.type = ValueType.DECIMAL;
        this.decimal = d;
    }

    public Value(double d) {
        this.type = ValueType.DECIMAL;
        this.decimal = BigDecimal.valueOf(d);
    }

    public static Value memoized(Function<List<Value>, Value> f) {
        var memo = new Value();
        memo.type = ValueType.MEMOIZED;
        memo.func = f;
        return memo;
    }

    public static Value func(Function<List<Value>, Value> f) {
        var memo = new Value();
        memo.type = ValueType.FUNCTION;
        memo.func = f;
        return memo;
    }

    /**
     * Used when this value is called as a function in the filter source,
     * as opposed to automatic resolution when referenced as a value
     */
    public Value apply(List<Value> args) {
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

    public List<Value> getArray() throws FiltrexRuntimeException {
        var resolved = this.resolve();
        if (resolved.type != ValueType.ARRAY) {
            throw new FiltrexRuntimeException("Invalid conversion of non-array type");
        }
        return resolved.array;
    }

    public Map<String, Value> getMap() {
        var resolved = this.resolve();
        if (resolved.type != ValueType.MAP) {
            throw new Error("Invalid conversion of non-map type");
        }
        return resolved.map;
    }

    public ValueType getType() {
        return type;
    }

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

    public String asStringOrNull() {
        var resolved = this.resolve();
        switch (type) {
            case BOOLEAN:
                return Boolean.toString(resolved.bool);
            case NULL:
                return null;
            case STRING:
                return resolved.str;
            case DECIMAL:
                return resolved.decimal.toString();
            case ARRAY:
                return resolved.array.stream().map(Value::asStringOrNull).collect(Collectors.joining(","));
            case MAP:
                throw new Error("Cannot convert map to string");
        }
        return null;
    }

    @Override
    public String toString() {
        var resolved = this.resolve();
        switch (type) {
            case BOOLEAN:
                return Boolean.toString(resolved.bool);
            case NULL:
                return "";
            case STRING:
                return resolved.str;
            case DECIMAL:
                return resolved.decimal.stripTrailingZeros().toString();
            case ARRAY:
                return "[" + resolved.array.stream().map(Value::toString).collect(Collectors.joining(",")) + "]";
            case MAP:
                return "{" + resolved.map.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue().toString()).collect(Collectors.joining(",")) + "}";
        }
        throw new FiltrexRuntimeException("Unknown type");
    }

    public List<Value> asArray() {
        var resolved = this.resolve();
        if (resolved.type == ValueType.ARRAY) {
            return resolved.array;
        }
        var list = new ArrayList<Value>();
        list.add(resolved);
        return list;
    }

    public BigDecimal asDecimal() {
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
                throw new RuntimeException("Invalid conversion of array to decimal");
            case MAP:
                throw new RuntimeException("Invalid conversion of map to decimal");
        }
        return BigDecimal.ZERO;
    }

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
