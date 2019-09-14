package io.harness.logging;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.MDC;

public class AutoLogContextTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void testAutoLogContext() throws Exception {
    String key = "foo";

    try (AutoLogContext level1 = new AutoLogContext("foo", "value")) {
      assertThat(MDC.get(key)).isEqualTo("value");
      try (AutoLogContext level2 = new AutoLogContext("foo", "value")) {
        assertThat(MDC.get(key)).isEqualTo("value");
      }
      assertThat(MDC.get(key)).isEqualTo("value");
    }
  }
}
