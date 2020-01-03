package io.harness.govern;

import static io.harness.rule.OwnerRule.GEORGE;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.MockableTestMixin;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;

public class SwitchTest extends CategoryTest implements MockableTestMixin {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void unhandled() throws IllegalAccessException {
    Logger mockLogger = mock(Logger.class);
    setStaticFieldValue(Switch.class, "logger", mockLogger);

    int a = 5;
    Switch.unhandled(a);

    verify(mockLogger, times(1))
        .error(matches("Unhandled switch value \\{\\}: \\{\\}\n\\{\\}"), matches("java.lang.Integer"), anyInt(),
            any(String.class));
  }
}
