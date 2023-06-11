package com.sesamecare.asyncRuleEvaluator;

import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;
import org.hamcrest.Matchers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class FiltrexArrayTests extends BaseFiltrexTest {
    @Test
    public void testInNotIn() {
        fails("5 in (1, 2, 3, 4)");
        pass("3 in (1, 2, 3, 4)");
        pass("5 not in (1, 2, 3, 4)");
        fails("3 not in (1, 2, 3, 4)");

        // array in array
        pass("(1, 2) in (1, 2, 3)");
        pass("(1, 2) in (2, 3, 1)");
        fails("(3, 4) in (1, 2, 3)");
        fails("(1, 2) not in (1, 2, 3)");
        fails("(1, 2) not in (2, 3, 1)");
        pass("(3, 4) not in (1, 2, 3)");

        // other edge cases
        fails("(1, 2) in 1");
        pass("1 in 1");
        pass("(1, 2) not in 1");
        fails("1 not in 1");
    }

    @Test
    public void testStrings() {
        pass("foo == \"hello\"", Map.of("foo", new Value("hello")));
        fails("foo == \"hello\"", Map.of("foo", new Value("bye")));
        fails("foo != \"hello\"", Map.of("foo", new Value("hello")));
        pass("foo != \"hello\"", Map.of("foo", new Value("bye")));
        pass("foo in (\"aa\", \"bb\")", Map.of("foo", new Value("aa")));
        fails("foo in (\"aa\", \"bb\")", Map.of("foo", new Value("cc")));
        fails("foo not in (\"aa\", \"bb\")", Map.of("foo", new Value("aa")));
        pass("foo not in (\"aa\", \"bb\")", Map.of("foo", new Value("cc")));

        assertThat(runFilter("\"\n\""), Matchers.comparesEqualTo(new Value("\n")));
        assertThat(runFilter("\"\u0000\""), Matchers.comparesEqualTo(new Value("\u0000")));
        assertThat(runFilter("\"\uD800\""), Matchers.comparesEqualTo(new Value("\uD800")));
    }

    @Test
    public void testArray() {
        var array = runFilter("(42, \"fifty\", pi)", Map.of("pi", new Value(BigDecimal.valueOf(Math.PI))));
        assertThat(array.getType(), Matchers.equalTo(Value.ValueType.ARRAY));
        assertThat(array.getArray().get(0).getType(), Matchers.equalTo(Value.ValueType.DECIMAL));
        assertThat(array.getArray().get(0).asDecimal(), Matchers.comparesEqualTo(BigDecimal.valueOf(42)));
        assertThat(array.getArray().get(1).getType(), Matchers.equalTo(Value.ValueType.STRING));
        assertThat(array.getArray().get(1).toString(), Matchers.equalTo("fifty"));
        assertThat(array.getArray().get(2).getType(), Matchers.equalTo(Value.ValueType.DECIMAL));
        assertThat(array.getArray().get(2).asDecimal(), Matchers.comparesEqualTo(BigDecimal.valueOf(Math.PI)));
        assertThat(array.getArray().size(), Matchers.equalTo(3));
    }

    @Test
    public void testInclude() {
        fails("1 in foo", Map.of("foo", new Value(List.of())));
        fails("1 in foo", Map.of("foo", new Value(List.of(new Value(0), new Value(2), new Value(3)))));
        pass("1 in foo", Map.of("foo", new Value(List.of(new Value(6), new Value(1), new Value(3)))));

        fails("1 in foo", Map.of("foo", new Value(List.of(new Value(6), new Value("1"), new Value(3)))));
        pass("1 in~ foo", Map.of("foo", new Value(List.of(new Value(6), new Value("1"), new Value(3)))));
        pass("1 in~ foo", Map.of("foo", new Value(List.of(new Value(6), new Value(1), new Value(3)))));

        fails("1 not in~ foo", Map.of("foo", new Value(List.of(new Value(6), new Value("1"), new Value(3)))));
        fails("1 not in~ foo", Map.of("foo", new Value(List.of(new Value(6), new Value(1), new Value(3)))));
        pass("1 not in~ foo", Map.of("foo", new Value(List.of(new Value(6), new Value(3)))));
    }
}
