/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.execution.utils;

import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class LevelUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testIsStageLevel() {
    assertThat(
        LevelUtils.isStageLevel(
            Level.newBuilder().setStepType(StepType.newBuilder().setStepCategory(StepCategory.STAGE).build()).build()))
        .isTrue();
    for (StepCategory category : StepCategory.values()) {
      if (category == StepCategory.STAGE || category == StepCategory.UNRECOGNIZED) {
        continue;
      }
      assertThat(LevelUtils.isStageLevel(
                     Level.newBuilder().setStepType(StepType.newBuilder().setStepCategory(category).build()).build()))
          .isFalse();
    }
  }
}
