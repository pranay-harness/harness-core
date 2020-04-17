package io.harness.time;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.logging.AutoLogContext;
import io.harness.rule.Owner;
import io.harness.threading.ExecutorModule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TimeModuleTest extends CategoryTest {
  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldPropagateLogContextInTimelimiterCall() throws Exception {
    List<Module> modules = new ArrayList<>();
    ExecutorModule.getInstance().setExecutorService(Executors.newSingleThreadExecutor());
    modules.addAll(TimeModule.getInstance().cumulativeDependencies());
    Injector injector = Guice.createInjector(modules);
    TimeLimiter timeLimiter = injector.getInstance(TimeLimiter.class);
    try (AutoLogContext context = new TestLogContext("foo", "bar", AutoLogContext.OverrideBehavior.OVERRIDE_ERROR)) {
      timeLimiter.callWithTimeout(() -> {
        assertThat(MDC.get("foo")).isEqualTo("bar");
        return null;
      }, 1, TimeUnit.MINUTES, false);
    }
  }

  private static class TestLogContext extends AutoLogContext {
    TestLogContext(String key, String value, OverrideBehavior behavior) {
      super(key, value, behavior);
    }
  }
}
