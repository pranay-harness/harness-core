/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans;

import static io.harness.rule.OwnerRule.POOJA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class StepExecutionStatusTest extends CategoryTest {
  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void getStatusCategory() {
    assertThat(ExecutionStatus.getStatusCategory(ExecutionStatus.SUCCESS)).isEqualTo(ExecutionStatusCategory.SUCCEEDED);
    assertThat(ExecutionStatus.getStatusCategory(ExecutionStatus.SKIPPED)).isEqualTo(ExecutionStatusCategory.SUCCEEDED);
    assertThat(ExecutionStatus.getStatusCategory(ExecutionStatus.RUNNING)).isEqualTo(ExecutionStatusCategory.ACTIVE);
    assertThat(ExecutionStatus.getStatusCategory(ExecutionStatus.FAILED)).isEqualTo(ExecutionStatusCategory.ERROR);
    assertThat(ExecutionStatus.getStatusCategory(ExecutionStatus.ERROR)).isEqualTo(ExecutionStatusCategory.ERROR);
    assertThat(ExecutionStatus.getStatusCategory(ExecutionStatus.EXPIRED)).isEqualTo(ExecutionStatusCategory.ERROR);
    assertThat(ExecutionStatus.getStatusCategory(ExecutionStatus.REJECTED)).isEqualTo(ExecutionStatusCategory.ERROR);
    assertThat(ExecutionStatus.getStatusCategory(ExecutionStatus.PAUSED)).isEqualTo(ExecutionStatusCategory.ACTIVE);
    assertThat(ExecutionStatus.getStatusCategory(ExecutionStatus.PAUSING)).isEqualTo(ExecutionStatusCategory.ACTIVE);
    assertThat(ExecutionStatus.getStatusCategory(ExecutionStatus.QUEUED)).isEqualTo(ExecutionStatusCategory.ACTIVE);
    assertThat(ExecutionStatus.getStatusCategory(ExecutionStatus.NEW)).isEqualTo(ExecutionStatusCategory.ACTIVE);
  }
}
