package io.harness.rule;

import static java.util.Arrays.asList;

import io.harness.category.element.CliFunctionalTests;
import io.harness.category.element.E2ETests;
import io.harness.category.element.FunctionalTests;
import io.harness.category.element.IntegrationTests;
import io.harness.category.element.StressTests;
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
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CategoryTimeoutRule extends Timeout {
  public static class RunMode {}

  public CategoryTimeoutRule() {
    super(Timeout.builder());
  }

  public static Class fetchCategoryElement(Category category) {
    List<Class> classes = asList(UnitTests.class, IntegrationTests.class, FunctionalTests.class,
        CliFunctionalTests.class, E2ETests.class, StressTests.class);

    Class element = null;
    for (Class clz : classes) {
      final boolean match = Arrays.stream(category.value()).anyMatch(clz ::isAssignableFrom);
      if (match) {
        if (element != null) {
          throw new RuntimeException("More than one category element");
        }
        element = clz;
      }
    }

    if (element == null) {
      throw new RuntimeException("Category element is not specified");
    }

    return element;
  }

  @Override
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

    final Class categoryElement = fetchCategoryElement(category);

    // There should not be categorized test to exceed execution of 10 minutes.
    long timeoutMS = TimeUnit.MINUTES.toMillis(10);

    // Start from the most time restricting group. Unit tests should take way less than
    // integration tests. If tests belongs to more than one group we would like to apply
    // the most restrictive pattern.
    if (categoryElement == UnitTests.class) {
      if (fast) {
        timeoutMS = 300;
      } else if (slow) {
        timeoutMS = TimeUnit.SECONDS.toMillis(2);
      }
    } else if (categoryElement == IntegrationTests.class) {
      if (fast) {
        timeoutMS = TimeUnit.SECONDS.toMillis(10);
      } else if (slow) {
        timeoutMS = TimeUnit.MINUTES.toMillis(5);
      }
    } else if (categoryElement == FunctionalTests.class) {
      if (fast) {
        timeoutMS = TimeUnit.MINUTES.toMillis(1);
      } else if (slow) {
        timeoutMS = TimeUnit.MINUTES.toMillis(15);
      }
    } else if (categoryElement == CliFunctionalTests.class) {
      if (fast) {
        timeoutMS = TimeUnit.MINUTES.toMillis(1);
      } else if (slow) {
        timeoutMS = TimeUnit.MINUTES.toMillis(15);
      }
    }

    return FailOnTimeout.builder()
        .withTimeout(timeoutMS, TimeUnit.MILLISECONDS)
        .withLookingForStuckThread(getLookingForStuckThread())
        .build(statement);
  }
}
