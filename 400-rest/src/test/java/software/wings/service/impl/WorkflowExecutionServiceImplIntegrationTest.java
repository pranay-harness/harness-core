/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.rule.Owner;

import software.wings.integration.IntegrationTestBase;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.List;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.CDC)
public class WorkflowExecutionServiceImplIntegrationTest extends IntegrationTestBase {
  @Inject WorkflowExecutionServiceImpl workflowExecutionService;

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void shouldGetStateExecutionInstancesForPhases() {
    StateExecutionInstance stateExecutionInstance1 = null, stateExecutionInstance2 = null,
                           stateExecutionInstance3 = null;
    try {
      stateExecutionInstance1 =
          aStateExecutionInstance().executionUuid("executionId1").stateType(StateType.PHASE.name()).build();
      stateExecutionInstance2 =
          aStateExecutionInstance().executionUuid("executionId1").stateType(StateType.PHASE_STEP.name()).build();
      stateExecutionInstance3 =
          aStateExecutionInstance().executionUuid("executionId2").stateType(StateType.PHASE.name()).build();
      wingsPersistence.save(Arrays.asList(stateExecutionInstance1, stateExecutionInstance2, stateExecutionInstance3));

      List<StateExecutionInstance> stateExecutionInstancesForPhases =
          workflowExecutionService.getStateExecutionInstancesForPhases("executionId1");

      assertThat(stateExecutionInstancesForPhases).hasSize(1);
    } finally {
      wingsPersistence.delete(stateExecutionInstance1);
      wingsPersistence.delete(stateExecutionInstance2);
      wingsPersistence.delete(stateExecutionInstance3);
    }
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void shouldReturnEmptyWhenStateExecutionInstancesForPhasesNotExists() {
    List<StateExecutionInstance> stateExecutionInstancesForPhases =
        workflowExecutionService.getStateExecutionInstancesForPhases("non-existent-execution-id");

    assertThat(stateExecutionInstancesForPhases).isEmpty();
  }
}
