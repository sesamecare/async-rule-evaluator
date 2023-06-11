package com.sesamecare.asyncRuleEvaluator;

import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Value implements Comparable<Value> {
    @Override
    public int compareTo(Value o) {
        if (this.canBeDecimal() && o.canBeDecimal()) {
            return this.asDecimal().compareTo(o.asDecimal());
        }
        return StringUtils.compare(this.asStringOrNull(), o.asStringOrNull());
    }

    enum ValueType { BOOLEAN, STRING, NULL, DECIMAL, ARRAY, MAP };

    private ValueType type;

    private boolean bool;
    private String str;

    private BigDecimal decimal;

    private List<Value> array;

    private Map<String, Value> map;

    public Value(boolean b) {
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

    public List<Value> getArray() throws FiltrexRuntimeException {
        if (this.type != ValueType.ARRAY) {
            throw new FiltrexRuntimeException("Invalid conversion of non-array type");
        }
        return array;
    }

    public Map<String, Value> getMap() {
        if (this.type != ValueType.MAP) {
            throw new Error("Invalid conversion of non-map type");
        }
        return map;
    }

    public ValueType getType() {
        return type;
    }

    public boolean asBoolean() {
        switch (type) {
            case BOOLEAN:
                return bool;
            case DECIMAL:
                return decimal.compareTo(BigDecimal.ZERO) != 0;
            case NULL:
                return false;
            case STRING:
                return str != null && str.length() > 0;
            case ARRAY:
                return true;
            case MAP:
                throw new RuntimeException("Invalid conversion of map to boolean");
        }
        return false;
    }

    public String asStringOrNull() {
        switch (type) {
            case BOOLEAN:
                return Boolean.toString(this.bool);
            case NULL:
                return null;
            case STRING:
                return this.str;
            case DECIMAL:
                return this.decimal.toString();
            case ARRAY:
                return this.array.stream().map(Value::asStringOrNull).collect(Collectors.joining(","));
            case MAP:
                throw new Error("Cannot convert map to string");
        }
        return null;
    }

    @Override
    public String toString() {
        switch (type) {
            case BOOLEAN:
                return Boolean.toString(this.bool);
            case NULL:
                return "";
            case STRING:
                return this.str;
            case DECIMAL:
                return this.decimal.stripTrailingZeros().toString();
            case ARRAY:
                return "[" + this.array.stream().map(Value::toString).collect(Collectors.joining(",")) + "]";
            case MAP:
                return "{" + this.map.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue().toString()).collect(Collectors.joining(",")) + "}";
        }
        throw new FiltrexRuntimeException("Unknown type");
    }

    public BigDecimal asDecimal() {
        switch (type) {
            case BOOLEAN:
                return bool ? BigDecimal.ONE : BigDecimal.ZERO;
            case NULL:
                return BigDecimal.ZERO;
            case STRING:
                return new BigDecimal(str);
            case DECIMAL:
                return decimal;
            case ARRAY:
                throw new RuntimeException("Invalid conversion of array to decimal");
            case MAP:
                throw new RuntimeException("Invalid conversion of map to decimal");
        }
        return BigDecimal.ZERO;
    }

    public boolean canBeDecimal() {
        if (this.type == ValueType.DECIMAL || this.type == ValueType.BOOLEAN) {
            return true;
        }
        if (this.type == ValueType.STRING) {
            try {
                Double.valueOf(this.str);
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
        if (this.type == ValueType.ARRAY) {
            for (var v : this.getArray()) {
                if (!v.in(other, exactMatch).asBoolean()) {
                    return Value.FALSE;
                }
            }
            return Value.TRUE;
        }
        var otherArray = other.type == ValueType.ARRAY ? other.getArray() : List.of(other);
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
