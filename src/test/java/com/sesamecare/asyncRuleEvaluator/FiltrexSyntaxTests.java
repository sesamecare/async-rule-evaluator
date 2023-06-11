package com.sesamecare.asyncRuleEvaluator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

public class FiltrexSyntaxTests extends BaseFiltrexTest {
    @Test
    public void testSimpleNumerics() {
        mathTest(7, "1 + 2 * 3");
        mathTest(7, "2 * 3 + 1");
        mathTest(7, "1 + (2 * 3)");
        mathTest(9, "(1 + 2) * 3");
        mathTest(8, "2 ^ 3");
        mathTest(-19, "((1 + 2) * 3 / 2 + 1 - 4 + (2 ^ 3)) * -2");
        mathTest(1.54, "1.4 * 1.1");
        mathTest(7, "97 % 10");
    }

    @Test
    public void testMathFunctions() {
        mathTest(5, "abs(-5)");
        mathTest(5, "abs(5)");
        mathTest(5, "ceil(4.1)");
        mathTest(5, "ceil(4.6)");
        mathTest(4, "floor(4.1)");
        mathTest(4, "floor(4.6)");
        mathTest(4, "round(4.1)");
        mathTest(5, "round(4.6)");
        mathTest(3, "sqrt(9)");
        mathTest(100000, "sqrt(10^10)");
    }

    @Test
    public void testArgArrangements() {
        assertTrue(runFilter("random() >= 0").asBoolean());
        assertFalse(runFilter("random() < 0").asBoolean());
        mathTest(2, "min(2)");
        mathTest(2, "max(2)");
        mathTest(2, "min(2, 5)");
        mathTest(5, "max(2, 5)");
        mathTest(2, "min(2, 5,6)");
        mathTest(6, "max(2, 5,6)");
        pass("min(2, 5,6, 1)");
        mathTest(6, "max(2, 5,6, 1)");
        pass("min(2, 5,6, 1, 4 + 5)");
        mathTest(9, "max(2, 5,6, 1, 4 + 5)");
    }

    @Test
    public void testComparisons() {
        pass( "foo == 4", Map.of("foo", new Value(4)));
        fails("foo == 4", Map.of("foo", new Value(3)));
        fails("foo == 4", Map.of("foo", new Value(-4)));
        fails("foo != 4", Map.of("foo", new Value(4)));
        pass("foo != 4", Map.of("foo", new Value(3)));
        pass("foo != 4", Map.of("foo", new Value(-4)));

        fails("foo > 4", Map.of("foo", new Value(3)));
        fails("foo > 4", Map.of("foo", new Value(4)));
        pass("foo > 4", Map.of("foo", new Value(5)));
        fails("foo >= 4", Map.of("foo", new Value(3)));
        pass("foo >= 4", Map.of("foo", new Value(4)));
        pass("foo >= 4", Map.of("foo", new Value(5)));
        pass("foo < 4", Map.of("foo", new Value(3)));
        fails("foo < 4", Map.of("foo", new Value(4)));
        fails("foo < 4", Map.of("foo", new Value(5)));
        pass("foo <= 4", Map.of("foo", new Value(3)));
        pass("foo <= 4", Map.of("foo", new Value(4)));
        fails("foo <= 4", Map.of("foo", new Value(5)));
    }

    @Test
    public void testBooleanLogic() {
        fails("0 and 0");
        fails("0 and 1");
        fails("1 and 0");
        pass("1 and 1");

        fails("0 or 0");
        pass("0 or 1");
        pass("1 or 0");
        pass("1 or 1");

        pass("this_is_undefined or 1");
        fails("this_is_undefined and 1");
        pass("this_is_undefined.really or 1");
        pass("this_is and 1", Map.of("this_is", new Value(1)));
        fails("this_is.really and 1", Map.of("this_is", new Value(1)));
        pass("this_is.really and 1", Map.of("this_is", new Value(Map.of("really", new Value(1)))));

        fails("not 1");
        pass("not 0");

        pass("(0 and 1) or 1");
        fails("0 and (1 or 1)");
        pass("0 and 1 or 1");
        pass("1 or 1 and 0");
        fails("not 1 and 0");
        pass("not 0 or 0");
    }

    @Test
    public void testRegexp() {
        pass("foo ~= \"^[hH]ello\"", Map.of("foo", new Value("hello")));
        fails("foo ~= \"^[hH]ello\"", Map.of("foo", new Value("goodbye")));
    }

    @Test
    public void testTernary() {
        mathTest(4,"1 > 2 ? 3 : 4");
        mathTest(3, "1 < 2 ? 3 : 4");
    }

    @Test
    public void testKitchenSink() {
        var program = "4 > lowNumber * 2 and (max(a, b) < 20 or foo) ? 1.1 : 9.4";
        mathTest(1.1, program, Map.of("lowNumber", new Value(1.5), "a", new Value(10), "b", new Value(12), "foo", Value.FALSE));
        mathTest(9.4, program, Map.of("lowNumber", new Value(3.5), "a", new Value(10), "b", new Value(12), "foo", Value.FALSE));
    }

    @Test
    public void testFunctions() {
        mathTest(3, "length(\"foo\")");
        mathTest(3, "length([1, 2, 3])");
        pass("substr(\"foo\", 1, 2) == \"oo\"");
        pass("substr(\"foo\", -2, 2) == \"oo\"");
        pass("substr(\"foo\", 0, 2) == \"fo\"");
    }
}
