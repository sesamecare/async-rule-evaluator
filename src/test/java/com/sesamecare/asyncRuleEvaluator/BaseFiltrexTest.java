package com.sesamecare.asyncRuleEvaluator;

import org.hamcrest.Matchers;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;

public class BaseFiltrexTest {
    Value runFilter(String code) {
        return runFilter(code, new HashMap<>());
    }

    Value runFilter(String code, Map<String, Value> context) {
        var exec = new FiltrexExecutor(code);
        return exec.run(context);
    }

    void mathTest(int expected, String code) {
        assertThat(runFilter(code).asDecimal(), Matchers.comparesEqualTo(BigDecimal.valueOf(expected)));
    }

    void mathTest(int expected, String code, Map<String, Value> context) {
        assertThat(runFilter(code, context).asDecimal(), Matchers.comparesEqualTo(BigDecimal.valueOf(expected)));
    }

    void mathTest(double expected, String code) {
        assertThat(runFilter(code).asDecimal(), Matchers.comparesEqualTo(BigDecimal.valueOf(expected)));
    }

    void mathTest(double expected, String code, Map<String, Value> context) {
        assertThat(runFilter(code, context).asDecimal(), Matchers.comparesEqualTo(BigDecimal.valueOf(expected)));
    }

    void pass(String code) {
        mathTest(1, code);
    }

    void pass(String code, Map<String, Value> context) {
        mathTest(1, code, context);
    }

    void fails(String code) {
        mathTest(0, code);
    }

    void fails(String code, Map<String, Value> context) {
        mathTest(0, code, context);
    }
}
