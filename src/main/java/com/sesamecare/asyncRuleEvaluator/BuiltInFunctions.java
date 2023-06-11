package com.sesamecare.asyncRuleEvaluator;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

public class BuiltInFunctions {
    static MathContext mathContext = new MathContext(4, RoundingMode.HALF_UP);
    public static Value execute(String fn, List<Value> args) {
        switch (fn) {
            case "abs":
                return new Value(args.get(0).asDecimal().abs());
            case "ceil":
                return new Value(args.get(0).asDecimal().setScale(0, RoundingMode.CEILING));
            case "floor":
                return new Value(args.get(0).asDecimal().setScale(0, RoundingMode.FLOOR));
            case "round":
                return new Value(args.get(0).asDecimal().setScale(0, RoundingMode.HALF_UP));
            case "sqrt":
                return new Value(args.get(0).asDecimal().sqrt(mathContext));
            case "min":
                return best(args, -1);
            case "max":
                return best(args, 1);
            case "random":
                if (args.size() == 0) {
                    return new Value(BigDecimal.valueOf(Math.random()));
                }
                return new Value(
                        BigDecimal.valueOf(
                                Math.random()).multiply(args.get(0).asDecimal()
                        ).setScale(0, RoundingMode.HALF_UP));
            case "length":
                switch (args.get(0).getType()) {
                    case ARRAY:
                        return new Value(args.get(0).getArray().size());
                    case STRING:
                        return new Value(args.get(0).toString().length());
                    case NULL:
                        return new Value(0);
                    case MAP:
                        return new Value(args.get(0).getMap().size());
                    case DECIMAL:
                        return new Value(args.get(0).asDecimal().toString().length());
                    case BOOLEAN:
                        // I guess? true is 4, false is 5...
                        return new Value(args.get(0).asBoolean() ? 4 : 5);
                }
                break;
            case "lower":
                return new Value(args.get(0).toString().toLowerCase());
            case "substr":
                return substr(
                        args.get(0).toString(),
                        args.size() > 1 ? args.get(1) : null,
                        args.size() > 2 ? args.get(2) : null
                );
        }
        return null;
    }

    static Value substr(String s, Value start, Value chars) {
        int startIndex = (start != null && start.getType() != Value.ValueType.NULL) ? start.asDecimal().intValue() : 0;
        if (startIndex < 0) {
            startIndex = s.length() + startIndex;
        }
        int endIndex = (chars != null && chars.getType() != Value.ValueType.NULL) ? (startIndex + chars.asDecimal().intValue()) : s.length();
        return new Value(s.substring(startIndex, endIndex));
    }

    static Value best(List<Value> args, int sign) {
        if (args.size() == 0) {
            return Value.NULL;
        }
        BigDecimal best = args.get(0).asDecimal();
        for (int i = 1; i < args.size(); i++) {
            BigDecimal current = args.get(i).asDecimal();
            if (current.compareTo(best) * sign > 0) {
                best = current;
            }
        }
        return new Value(best);
    }
}

