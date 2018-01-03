package software.wings.rules;

import org.junit.experimental.categories.Category;
import org.junit.internal.runners.statements.FailOnTimeout;
import org.junit.rules.Timeout;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import software.wings.category.speed.FastTests;
import software.wings.category.speed.SlowTests;
import software.wings.category.element.IntegrationTests;
import software.wings.category.element.UnitTests;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class CategoryTimeoutRule extends Timeout {
  public CategoryTimeoutRule() {
    super(Timeout.builder());
  }

  public Statement apply(Statement statement, Description description) {
    Category category = description.getAnnotation(Category.class);
    if (category == null) {
      return statement;
    }

    boolean fast = Arrays.stream(category.value()).anyMatch(cls -> FastTests.class.isAssignableFrom(cls));
    boolean slow = Arrays.stream(category.value()).anyMatch(cls -> SlowTests.class.isAssignableFrom(cls));
    if (fast && slow) {
      throw new RuntimeException("A test cannot be fast and slow at the same time");
    }

    boolean unit = Arrays.stream(category.value()).anyMatch(cls -> UnitTests.class.isAssignableFrom(cls));
    boolean integration = Arrays.stream(category.value()).anyMatch(cls -> IntegrationTests.class.isAssignableFrom(cls));

    if (!unit && !integration) {
      throw new RuntimeException("A test should belong to at least one type category");
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
