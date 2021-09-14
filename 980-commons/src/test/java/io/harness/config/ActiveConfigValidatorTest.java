/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.config;

import static io.harness.rule.OwnerRule.GEORGE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
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
