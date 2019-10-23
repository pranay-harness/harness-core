package io.harness.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class WorkersConfigurationTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void testConfirmWorkerIsActive() {
    final WorkersConfiguration workersConfiguration = new WorkersConfiguration();
    assertThat(workersConfiguration.confirmWorkerIsActive(WorkersConfigurationTest.class)).isTrue();

    workersConfiguration.setActive(ImmutableMap.<String, Boolean>builder().put("io.harness", false).build());
    assertThat(workersConfiguration.confirmWorkerIsActive(WorkersConfigurationTest.class)).isFalse();

    workersConfiguration.setActive(
        ImmutableMap.<String, Boolean>builder().put("io.harness.config", true).put("io.harness", false).build());
    assertThat(workersConfiguration.confirmWorkerIsActive(WorkersConfigurationTest.class)).isTrue();

    workersConfiguration.setActive(ImmutableMap.<String, Boolean>builder()
                                       .put("io.harness.config.WorkersConfigurationTest", false)
                                       .put("io.harness.config", true)
                                       .put("io.harness", false)
                                       .build());
    assertThat(workersConfiguration.confirmWorkerIsActive(WorkersConfigurationTest.class)).isFalse();
  }
}
