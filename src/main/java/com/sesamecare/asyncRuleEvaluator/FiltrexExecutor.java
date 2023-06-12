package com.sesamecare.asyncRuleEvaluator;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.Map;

/**
 * This class is used to execute a Filtrex expression.
 * <p>
 * Example usage:
 * <pre>
 * var executor = new FiltrexExecutor("foo > 5");
 * var result = executor.run(Map.of("foo", new Value(2)));
 * </pre>
 *
 * The executor can be reused and the run method can be called from multiple threads at once
 */
public class FiltrexExecutor {
    ParseTree tree;

    /**
     * Create a new executor for the given expression
     * @param expression Filtrex rule code
     */
    public FiltrexExecutor(String expression) {
        var stream = CharStreams.fromString(expression);
        var lexer = new FiltrexLexer(stream);
        var tokens = new CommonTokenStream(lexer);
        var parser = new FiltrexParser(tokens);

        tree = parser.expressions();
    }

    /**
     * Run a parsed expression with the given input data
     * @param inputData Values made available to the filtrex rules
     * @return The result of the run, as a Value type, which often is checked with asBoolean
     */
    public Value run(Map<String, Value> inputData) {
        var v = new FiltrexVisitorWithState(inputData, null);
        var finalState = v.visit(tree);
        return finalState;
    }
}
