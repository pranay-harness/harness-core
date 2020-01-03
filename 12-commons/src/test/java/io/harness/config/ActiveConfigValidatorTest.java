package io.harness.config;

import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ActiveConfigValidatorTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testIsActive() {
    final WorkersConfiguration workersConfiguration = new WorkersConfiguration();
    assertThat(workersConfiguration.confirmWorkerIsActive(ActiveConfigValidatorTest.class)).isTrue();

    workersConfiguration.setActive(ImmutableMap.<String, Boolean>builder().put("io.harness", false).build());
    assertThat(workersConfiguration.confirmWorkerIsActive(ActiveConfigValidatorTest.class)).isFalse();

    workersConfiguration.setActive(
        ImmutableMap.<String, Boolean>builder().put("io.harness.config", true).put("io.harness", false).build());
    assertThat(workersConfiguration.confirmWorkerIsActive(ActiveConfigValidatorTest.class)).isTrue();

    workersConfiguration.setActive(ImmutableMap.<String, Boolean>builder()
                                       .put("io.harness.config.ActiveConfigValidatorTest", false)
                                       .put("io.harness.config", true)
                                       .put("io.harness", false)
                                       .build());
    assertThat(workersConfiguration.confirmWorkerIsActive(ActiveConfigValidatorTest.class)).isFalse();
  }
}
