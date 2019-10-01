package io.harness.logging;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_NESTS;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.MDC;

public class AutoLogContextTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void shouldPreserveSameValue() {
    String key = "foo";

    try (AutoLogContext level1 = new AutoLogContext("foo", "value", OVERRIDE_ERROR)) {
      assertThat(MDC.get(key)).isEqualTo("value");
      try (AutoLogContext level2 = new AutoLogContext("foo", "value", OVERRIDE_ERROR)) {
        assertThat(MDC.get(key)).isEqualTo("value");
      }
      assertThat(MDC.get(key)).isEqualTo("value");
    }
  }

  @Test
  @Category(UnitTests.class)
  public void shouldPreserveSameValueOnOverride() {
    String key = "foo";

    try (AutoLogContext level1 = new AutoLogContext("foo", "value", OVERRIDE_ERROR)) {
      assertThat(MDC.get(key)).isEqualTo("value");
      try (AutoLogContext level2 = new AutoLogContext("foo", "bar", OVERRIDE_ERROR)) {
        assertThat(MDC.get(key)).isEqualTo("value");
      }
      assertThat(MDC.get(key)).isEqualTo("value");
    }
  }

  @Test
  @Category(UnitTests.class)
  public void shouldNestDifferentValueOnOverride() {
    String key = "foo";

    try (AutoLogContext level1 = new AutoLogContext("foo", "value", OVERRIDE_ERROR)) {
      assertThat(MDC.get(key)).isEqualTo("value");
      try (AutoLogContext level2 = new AutoLogContext("foo", "bar", OVERRIDE_NESTS)) {
        assertThat(MDC.get(key)).isEqualTo("bar");
      }
      assertThat(MDC.get(key)).isEqualTo("value");
    }
  }
}
