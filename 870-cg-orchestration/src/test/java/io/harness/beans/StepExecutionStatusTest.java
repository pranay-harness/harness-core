/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
