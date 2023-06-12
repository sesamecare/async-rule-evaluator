package com.sesamecare.asyncRuleEvaluator;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class FiltrexFunctionTests extends BaseFiltrexTest {
    @Test
    public void testBasicFunctionTypes() {
        AtomicBoolean called = new AtomicBoolean(false);
        var fns = Map.of(
                "one", Value.func(args -> new Value(1)),
                "thing", new Value(Map.of(
                        "echo", Value.func(args -> args.get(0))
                )),
                "once", Value.memoized(args -> {
                    if (called.get()) {
                        throw new RuntimeException("once() called twice");
                    }
                    called.set(true);
                    return new Value(1);
                })
        );
        pass("one() == 1", fns);
        fails("one() == 2", fns);
        pass("one == 1", fns);
        fails("one == 2", fns);
        pass("thing.echo(4) == 4", fns);
        pass("once == 1 and once == 1", fns);
    }

}
