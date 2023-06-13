package com.sesamecare.asyncRuleEvaluator;

import org.junit.jupiter.api.Test;

public class FiltrexSetTests extends BaseFiltrexTest {
    @Test
    public void testUnion() {
        pass("[1, 2, 3, 4] in union([1, 2], [3, 4])");
        pass("[1, 2, 3, 4] in union([1, 2], [2, 3, 4])");
        pass("[1, 2] in intersection([1, 2, 3, 4], [1, 2], [1, 2])");

        pass("[1, 2, 3, 4] in union([1], 2, [3, 4])");
        pass("1 in intersection([1], [1, 2], 1, [3, 4, 1])");
        pass("length(intersection([1], [1, 2], 1, [3, 4, 1])) == 1");

        pass("[1, 2] in difference([1, 2, 3, 4], [4, 3])");
        pass("[3, 4] not in difference([1, 2, 3, 4], [4, 3])");

        pass("1 in unique([1, 1, 1])");
        pass("length(unique([1, 1, 1])) == 1");
    }
}
