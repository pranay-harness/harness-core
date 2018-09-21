package io.harness.rule;

import io.harness.category.element.IntegrationTests;
import io.harness.category.element.UnitTests;
import io.harness.category.speed.FastTests;
import io.harness.category.speed.SlowTests;
import io.harness.exception.CategoryConfigException;
import io.harness.exception.ImpossibleException;
import org.junit.experimental.categories.Category;
import org.junit.internal.runners.statements.FailOnTimeout;
import org.junit.rules.Timeout;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class CategoryTimeoutRule extends Timeout {
  public static class RunMode {}

  public CategoryTimeoutRule() {
    super(Timeout.builder());
  }

  public Statement apply(Statement statement, Description description) {
    Category category = description.getAnnotation(Category.class);
    if (category == null) {
      return statement;
    }

    boolean isDebug =
        ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("-agentlib:jdwp") >= 0;

    // Do not timeout when someone is debugging
    if (isDebug) {
      // This provides proof that in running mode we did not wrongfully detect a debugging mode
      if (Arrays.stream(category.value()).anyMatch(RunMode.class ::isAssignableFrom)) {
        throw new ImpossibleException("You should not be debugging the running test");
      }

      return statement;
    }

    boolean fast = Arrays.stream(category.value()).anyMatch(FastTests.class ::isAssignableFrom);
    boolean slow = Arrays.stream(category.value()).anyMatch(SlowTests.class ::isAssignableFrom);
    if (fast && slow) {
      throw new CategoryConfigException("A test cannot be fast and slow at the same time");
    }

    boolean unit = Arrays.stream(category.value()).anyMatch(UnitTests.class ::isAssignableFrom);
    boolean integration = Arrays.stream(category.value()).anyMatch(IntegrationTests.class ::isAssignableFrom);

    if (!unit && !integration) {
      throw new CategoryConfigException("A test should belong to at least one type category");
    }

    // There should not be categorized test to exceed execution of 10 minutes.
    long timeoutMS = TimeUnit.MINUTES.toMillis(10);

    // Start from the most time restricting group. Unit tests should take way less than
    // integration tests. If tests belongs to more than one group we would like to apply
    // the most restrictive pattern.
    if (unit) {
      if (fast) {
        timeoutMS = 300;
      } else if (slow) {
        timeoutMS = TimeUnit.SECONDS.toMillis(2);
      } else {
        timeoutMS = TimeUnit.SECONDS.toMillis(1);
      }
    } else if (integration) {
      if (fast) {
        timeoutMS = TimeUnit.SECONDS.toMillis(10);
      } else if (slow) {
        timeoutMS = TimeUnit.MINUTES.toMillis(5);
      } else {
        timeoutMS = TimeUnit.MINUTES.toMillis(1);
      }
    }

    return FailOnTimeout.builder()
        .withTimeout(timeoutMS, TimeUnit.MILLISECONDS)
        .withLookingForStuckThread(getLookingForStuckThread())
        .build(statement);
  }
}
