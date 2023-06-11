package com.sesamecare.asyncRuleEvaluator;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.Map;

public class FiltrexExecutor {
    ParseTree tree;

    public FiltrexExecutor(String expression) {
        var stream = CharStreams.fromString(expression);
        var lexer = new FiltrexLexer(stream);
        var tokens = new CommonTokenStream(lexer);
        var parser = new FiltrexParser(tokens);

        tree = parser.expressions();
    }

    public Value run(Map<String, Value> inputData) {
        var v = new FiltrexVisitorWithState(inputData, null);
        var finalState = v.visit(tree);
        return finalState;
    }
}
