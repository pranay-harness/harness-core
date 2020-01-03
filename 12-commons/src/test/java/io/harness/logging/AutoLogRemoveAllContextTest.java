package io.harness.logging;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.MDC;

public class AutoLogRemoveAllContextTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldRemove() {
    try (AutoLogContext add1 = new AutoLogContext("foo", "value", OVERRIDE_ERROR)) {
      assertThat(MDC.get("foo")).isEqualTo("value");
      try (AutoLogRemoveAllContext remove = new AutoLogRemoveAllContext()) {
        assertThat(MDC.get("foo")).isNull();
        try (AutoLogContext add2 = new AutoLogContext("bar", "value", OVERRIDE_ERROR)) {
          assertThat(MDC.get("bar")).isEqualTo("value");
        }
      }
      assertThat(MDC.get("foo")).isEqualTo("value");
      assertThat(MDC.get("bar")).isNull();
    }
  }
}
