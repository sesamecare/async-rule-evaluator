package com.sesamecare.asyncRuleEvaluator;

import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.RegExUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class FiltrexVisitorWithState extends FiltrexBaseVisitor<Value> {
    Map<String, Value> inputData;
    Map<String, Supplier<Value>> functions;

    public FiltrexVisitorWithState(Map<String, Value> inputData, Map<String, Supplier<Value>> additionalFunctions) {
        this.inputData = inputData;
        this.functions = additionalFunctions;
    }

    private Value agg(Value head, Value tail) {
        List<Value> list = new ArrayList<>(head.getArray());
        list.add(tail);
        return new Value(list);
    }

    private Value resolve(String symbol) {
        var paths = symbol.split("\\.");
        Map<String, Value> current = inputData;
        for (int i = 0; i < paths.length; i++) {
            var path = paths[i];
            if (current.containsKey(path)) {
                var value = current.get(path).resolve();
                if (value.getType() == ValueType.MAP) {
                    current = value.getMap();
                }
                if (i == paths.length - 1) {
                    return value;
                }
            } else {
                return Value.NULL;
            }
        }
        return Value.NULL;
    }

    @Override
    public Value visitExpressions(FiltrexParser.ExpressionsContext ctx) {
        return visit(ctx.e());
    }

    @Override
    public Value visitMulDiv(FiltrexParser.MulDivContext ctx) {
        var lhs = visit(ctx.e(0));
        var rhs = visit(ctx.e(1));
        if ("*".equals(ctx.op.getText())) {
            return new Value(lhs.asDecimal().multiply(rhs.asDecimal()));
        }
        return new Value(lhs.asDecimal().divide(rhs.asDecimal()));
    }

    @Override
    public Value visitArrayWithCommaExpression(FiltrexParser.ArrayWithCommaExpressionContext ctx) {
        return agg(visit(ctx.array()), visit(ctx.e()));
    }

    @Override
    public Value visitOr(FiltrexParser.OrContext ctx) {
        var lhs = visit(ctx.e(0));
        if (lhs.asBoolean()) {
            return Value.TRUE;
        }
        var rhs = visit(ctx.e(1));
        return new Value(rhs.asBoolean());
    }

    @Override
    public Value visitIn(FiltrexParser.InContext ctx) {
        var target = visit(ctx.e(0));
        var arr = visit(ctx.e(1));
        return target.in(arr, true);
    }

    @Override
    public Value visitNotIn(FiltrexParser.NotInContext ctx) {
        var target = visit(ctx.e(0));
        var arr = visit(ctx.e(1));
        return target.in(arr, true) == Value.TRUE ? Value.FALSE : Value.TRUE;
    }

    @Override
    public Value visitSymbol(FiltrexParser.SymbolContext ctx) {
        var symbol = ctx.SYMBOL().toString();
        return resolve(symbol);
    }

    @Override
    public Value visitString(FiltrexParser.StringContext ctx) {
        var withQuotes = ctx.STRING().toString();
        return new Value(withQuotes.substring(1, withQuotes.length() - 1));
    }

    @Override
    public Value visitArrayWithCommaBracketExpression(FiltrexParser.ArrayWithCommaBracketExpressionContext ctx) {
        return agg(visit(ctx.array()), visit(ctx.e()));
    }

    @Override
    public Value visitLessThan(FiltrexParser.LessThanContext ctx) {
        var lhs = visit(ctx.e(0));
        var rhs = visit(ctx.e(1));
        return new Value(lhs.compareTo(rhs) == -1);
    }

    @Override
    public Value visitSymbolFunctionCallWithArgs(FiltrexParser.SymbolFunctionCallWithArgsContext ctx) {
        var fn = ctx.SYMBOL().getText();
        var args = visit(ctx.argsList());
        var result = BuiltInFunctions.execute(fn, args.getArray());
        if (result == null) {
            var custom = resolve(fn);
            result = custom.apply(args.getArray());
        }
        return result;
    }

    @Override
    public Value visitSymbolFunctionCall(FiltrexParser.SymbolFunctionCallContext ctx) {
        var fn = ctx.SYMBOL().getText();
        var result = BuiltInFunctions.execute(fn, Value.EMPTY.getArray());
        if (result == null) {
            var custom = resolve(fn);
            result = custom.apply(Value.EMPTY.getArray());
        }
        return result;
    }

    @Override
    public Value visitNumber(FiltrexParser.NumberContext ctx) {
        var strNum = ctx.NUMBER().toString();
        if (strNum.indexOf('.') >= 0) {
            return new Value(new BigDecimal(strNum));
        }
        return new Value(Integer.valueOf(strNum));
    }

    @Override
    public Value visitGreaterThan(FiltrexParser.GreaterThanContext ctx) {
        var lhs = visit(ctx.e(0));
        var rhs = visit(ctx.e(1));
        return new Value(lhs.compareTo(rhs) == 1);
    }

    @Override
    public Value visitAddSub(FiltrexParser.AddSubContext ctx) {
        var lhs = visit(ctx.e(0));
        var rhs = visit(ctx.e(1));
        if ("+".equals(ctx.op.getText())) {
            return new Value(lhs.asDecimal().add(rhs.asDecimal()));
        }
        return new Value(lhs.asDecimal().subtract(rhs.asDecimal()));
    }

    @Override
    public Value visitTernary(FiltrexParser.TernaryContext ctx) {
        var condition = visit(ctx.e(0));
        return visit(ctx.e(condition.asBoolean() ? 1 : 2));
    }

    @Override
    public Value visitModulo(FiltrexParser.ModuloContext ctx) {
        var lhs = visit(ctx.e(0));
        var rhs = visit(ctx.e(1));
        return new Value(lhs.asDecimal().remainder(rhs.asDecimal()));
    }

    @Override
    public Value visitUnaryMinus(FiltrexParser.UnaryMinusContext ctx) {
        var operand = visit(ctx.e());
        return new Value(operand.asDecimal().negate());
    }

    @Override
    public Value visitAlternativeNot(FiltrexParser.AlternativeNotContext ctx) {
        return null;
    }

    @Override
    public Value visitGreaterThanEquals(FiltrexParser.GreaterThanEqualsContext ctx) {
        var lhs = visit(ctx.e(0));
        var rhs = visit(ctx.e(1));
        return new Value(lhs.compareTo(rhs) != -1);
    }

    @Override
    public Value visitNot(FiltrexParser.NotContext ctx) {
        return new Value(!visit(ctx.e()).asBoolean());
    }

    @Override
    public Value visitEquals(FiltrexParser.EqualsContext ctx) {
        var lhs = visit(ctx.e(0));
        var rhs = visit(ctx.e(1));
        return new Value(lhs.compareTo(rhs) == 0);
    }

    @Override
    public Value visitNotEquals(FiltrexParser.NotEqualsContext ctx) {
        var lhs = visit(ctx.e(0));
        var rhs = visit(ctx.e(1));
        return new Value(lhs.compareTo(rhs) != 0);
    }

    @Override
    public Value visitAnd(FiltrexParser.AndContext ctx) {
        var lhs = visit(ctx.e(0));
        var rhs = visit(ctx.e(1));
        return new Value(lhs.asBoolean() && rhs.asBoolean());
    }

    @Override
    public Value visitParenExpression(FiltrexParser.ParenExpressionContext ctx) {
        return visit(ctx.e());
    }

    @Override
    public Value visitArrayExpression(FiltrexParser.ArrayExpressionContext ctx) {
        return new Value(visit(ctx.e()).asArray());
    }

    @Override
    public Value visitRegexMatch(FiltrexParser.RegexMatchContext ctx) {
        var target = visit(ctx.e(0));
        var exp = visit(ctx.e(1));
        var pattern = Pattern.compile(exp.toString());
        var matcher = pattern.matcher(target.toString());
        return matcher.find() ? Value.TRUE : Value.FALSE;
    }

    @Override
    public Value visitInexactIn(FiltrexParser.InexactInContext ctx) {
        var target = visit(ctx.e(0));
        var arr = visit(ctx.e(1));
        return target.in(arr, false);
    }

    @Override
    public Value visitNotInexactIn(FiltrexParser.NotInexactInContext ctx) {
        var target = visit(ctx.e(0));
        var arr = visit(ctx.e(1));
        return target.in(arr, false).asBoolean() ? Value.FALSE : Value.TRUE;
    }

    @Override
    public Value visitPower(FiltrexParser.PowerContext ctx) {
        var lhs = visit(ctx.e(0));
        var rhs = visit(ctx.e(1));
        return new Value(lhs.asDecimal().pow(rhs.asDecimal().intValue()));
    }

    @Override
    public Value visitLessThanEquals(FiltrexParser.LessThanEqualsContext ctx) {
        var lhs = visit(ctx.e(0));
        var rhs = visit(ctx.e(1));
        return new Value(lhs.compareTo(rhs) != 1);
    }

    @Override
    public Value visitSingleArg(FiltrexParser.SingleArgContext ctx) {
        var arg = visit(ctx.e());
        var args = new ArrayList<Value>();
        args.add(arg);
        return new Value(args);
    }

    @Override
    public Value visitArgs(FiltrexParser.ArgsContext ctx) {
        return agg(visit(ctx.argsList()), visit(ctx.e()));
    }

    @Override
    public Value visitSingleElement(FiltrexParser.SingleElementContext ctx) {
        return new Value(visit(ctx.e()).asArray());
    }

    @Override
    public Value visitArrayElements(FiltrexParser.ArrayElementsContext ctx) {
        return agg(visit(ctx.array()), visit(ctx.e()));
    }

    @Override
    public Value visitChildren(RuleNode node) {
        return null;
    }

    @Override
    public Value visitTerminal(TerminalNode node) {
        return null;
    }

    @Override
    public Value visitErrorNode(ErrorNode node) {
        return null;
    }
}
