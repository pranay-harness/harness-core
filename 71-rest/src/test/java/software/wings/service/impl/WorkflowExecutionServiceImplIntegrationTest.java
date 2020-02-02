package software.wings.service.impl;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;

import com.google.inject.Inject;

import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.integration.BaseIntegrationTest;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;

import java.util.Arrays;
import java.util.List;

public class WorkflowExecutionServiceImplIntegrationTest extends BaseIntegrationTest {
  @Inject WorkflowExecutionServiceImpl workflowExecutionService;

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(DeprecatedIntegrationTests.class)
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
  public void shouldReturnEmptyWhenStateExecutionInstancesForPhasesNotExists() {
    List<StateExecutionInstance> stateExecutionInstancesForPhases =
        workflowExecutionService.getStateExecutionInstancesForPhases("non-existent-execution-id");

    assertThat(stateExecutionInstancesForPhases).isEmpty();
  }
}