/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.workflow;

import static io.harness.annotations.dev.HarnessModule._870_CG_ORCHESTRATION;
import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.VED;

import static software.wings.beans.workflow.StepSkipStrategy.Scope.ALL_STEPS;
import static software.wings.beans.workflow.StepSkipStrategy.Scope.SPECIFIC_STEPS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDC)
@TargetModule(_870_CG_ORCHESTRATION)
public class StepSkipStrategyTest extends WingsBaseTest {
  private static final String ASSERTION_EXPRESSION = "true";

  @Test
  @Owner(developers = VED)
  @Category(UnitTests.class)
  public void testForValidateStepSkipStrategies() {
    String stepId = "stepId";

    StepSkipStrategy stepSkipStrategy = new StepSkipStrategy(ALL_STEPS, Collections.emptyList(), ASSERTION_EXPRESSION);
    StepSkipStrategy stepSkipStrategy1 =
        new StepSkipStrategy(SPECIFIC_STEPS, Collections.singletonList(stepId), ASSERTION_EXPRESSION);

    // ALL SKIP ALREADY EXIST, CAN'T ADD A SPECIFIC SKIP
    List<StepSkipStrategy> stepSkipStrategyList = new ArrayList<>();
    stepSkipStrategyList.add(stepSkipStrategy);
    stepSkipStrategyList.add(stepSkipStrategy1);

    assertThatThrownBy(() -> StepSkipStrategy.validateStepSkipStrategies(stepSkipStrategyList))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Cannot add a skip condition as a skip all condition already exists");

    // SPECIFIC SKIP ALREADY EXISTS FOR THE SAME STEP-ID
    List<StepSkipStrategy> stepSkipStrategyList1 = new ArrayList<>();
    stepSkipStrategyList1.add(stepSkipStrategy1);
    stepSkipStrategyList1.add(stepSkipStrategy1);

    assertThatThrownBy(() -> StepSkipStrategy.validateStepSkipStrategies(stepSkipStrategyList1))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Multiple skip conditions for the same step");

    // SPECIFIC SKIP ALREADY EXISTS, CAN'T ADD ALL SKIP
    List<StepSkipStrategy> stepSkipStrategyList2 = new ArrayList<>();
    stepSkipStrategyList2.add(stepSkipStrategy1);
    stepSkipStrategyList2.add(stepSkipStrategy);

    assertThatThrownBy(() -> StepSkipStrategy.validateStepSkipStrategies(stepSkipStrategyList2))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Cannot skip all steps as a skip condition already exists");
  }

  @Test
  @Owner(developers = VED)
  @Category(UnitTests.class)
  public void testForContainsStepId() {
    String stepId = "StepId";
    String stepId1 = "StepId1";
    List<String> stepIds = new ArrayList<>();
    stepIds.add(stepId);
    stepIds.add(stepId1);

    StepSkipStrategy stepSkipStrategy = new StepSkipStrategy(ALL_STEPS, Collections.emptyList(), ASSERTION_EXPRESSION);
    StepSkipStrategy stepSkipStrategy1 = new StepSkipStrategy(SPECIFIC_STEPS, stepIds, ASSERTION_EXPRESSION);

    assertThat(stepSkipStrategy.containsStepId(stepId)).isTrue();
    assertThat(stepSkipStrategy1.containsStepId("StepId2")).isFalse();
    assertThat(stepSkipStrategy1.containsStepId("StepId1")).isTrue();
  }
}
