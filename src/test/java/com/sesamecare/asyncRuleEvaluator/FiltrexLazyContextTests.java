package com.sesamecare.asyncRuleEvaluator;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.hamcrest.Matchers;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;

public class FiltrexLazyContextTests extends BaseFiltrexTest {
    class User {
        public int id;
        public String name;
        public int callCount = 0;
        public int valueMapCalls = 0;

        public Value getHttpBin() {
            try {
                var content = new URL("https://httpbin.org/anything?calls=" + callCount).openStream().readAllBytes();
                var parsed = new Gson().fromJson(new String(content), JsonElement.class);
                return new Value(Map.of(
                        "callCount", new Value(parsed.getAsJsonObject().get("args").getAsJsonObject().get("calls").getAsString())
                ));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        public Value getValueMap() {
            valueMapCalls += 1;
            return new Value(Map.of(
                    "id", new Value(id),
                    "name", new Value(name),
                    "callCount", new Value(args -> new Value(this.callCount++), false),
                    "originalCallCount", new Value(args -> new Value(this.callCount++), true),
                    "httpBin", new Value(args -> this.getHttpBin(), true)
            ));
        }
    }

    @Test
    public void testLazyMap() {
        var user = new User();
        user.id = 1;
        user.name = "John Doe";
        var context = Map.of(
                "user", new Value(args -> user.getValueMap(), true)
        );
        pass("user.id == 1", context);
        pass("user.name == \"John Doe\"", context);
        pass("user.originalCallCount == 0", context);
        pass("user.originalCallCount == 0", context);
        pass("user.callCount == 1", context);
        pass("user.callCount == 2", context);
        assertThat(user.valueMapCalls, Matchers.equalTo(1));

        pass("user.httpBin.callCount == 3", context);
        pass("user.callCount == 3", context);
        pass("user.httpBin.callCount == 3", context);
    }
}
