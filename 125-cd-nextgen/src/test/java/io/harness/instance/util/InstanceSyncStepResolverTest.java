/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.instance.util;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.IVAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.instance.util.InstanceSyncStepResolver;
import io.harness.cdng.pipeline.steps.NGSectionStep;
import io.harness.pms.contracts.steps.StepType;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDP)
public class InstanceSyncStepResolverTest extends CategoryTest {
  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testShouldRunInstanceSync() {
    List<Boolean> validationList = new ArrayList<>();
    InstanceSyncStepResolver.INSTANCE_SYN_STEP_TYPES.forEach(stepType -> {
      boolean shouldRunInstanceSync =
          InstanceSyncStepResolver.shouldRunInstanceSync(StepType.newBuilder().setType(stepType).build());
      validationList.add(shouldRunInstanceSync);
    });

    assertThat(validationList.size()).isEqualTo(InstanceSyncStepResolver.INSTANCE_SYN_STEP_TYPES.size());
    assertThat(validationList.stream().allMatch(el -> el)).isTrue();
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testShouldRunInstanceSyncWithNotInstanceSyncStep() {
    boolean shouldRunInstanceSync = InstanceSyncStepResolver.shouldRunInstanceSync(NGSectionStep.STEP_TYPE);

    assertThat(shouldRunInstanceSync).isFalse();
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testShouldRunInstanceSyncWithStepTypeEmpty() {
    boolean shouldRunInstanceSync = InstanceSyncStepResolver.shouldRunInstanceSync(StepType.newBuilder().build());

    assertThat(shouldRunInstanceSync).isFalse();
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testModifyInstanceSyncStepTypesSet() {
    assertThatThrownBy(() -> InstanceSyncStepResolver.INSTANCE_SYN_STEP_TYPES.add(NGSectionStep.STEP_TYPE.getType()))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
