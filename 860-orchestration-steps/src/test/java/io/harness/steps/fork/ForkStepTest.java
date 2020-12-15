package io.harness.steps.fork;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.OrchestrationStepsTestBase;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildrenExecutableResponse;
import io.harness.pms.contracts.execution.ChildrenExecutableResponse.Child;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponseNotifyData;
import io.harness.rule.Owner;
import io.harness.tasks.ResponseData;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ForkStepTest extends OrchestrationStepsTestBase {
  @Inject private ForkStep forkStep;

  private static final String FIRST_CHILD_ID = generateUuid();
  private static final String SECOND_CHILD_ID = generateUuid();

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestObtainChildren() {
    Ambiance ambiance = Ambiance.newBuilder().build();
    StepInputPackage inputPackage = StepInputPackage.builder().build();
    ForkStepParameters stateParameters =
        ForkStepParameters.builder().parallelNodeId(FIRST_CHILD_ID).parallelNodeId(SECOND_CHILD_ID).build();
    ChildrenExecutableResponse childrenExecutableResponse =
        forkStep.obtainChildren(ambiance, stateParameters, inputPackage);
    assertThat(childrenExecutableResponse).isNotNull();
    assertThat(childrenExecutableResponse.getChildrenList()).hasSize(2);
    List<String> childIds =
        childrenExecutableResponse.getChildrenList().stream().map(Child::getChildNodeId).collect(Collectors.toList());
    assertThat(childIds).hasSize(2);
    assertThat(childIds).containsExactlyInAnyOrder(FIRST_CHILD_ID, SECOND_CHILD_ID);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestHandleChildrenResponse() {
    Ambiance ambiance = Ambiance.newBuilder().build();
    ForkStepParameters stateParameters =
        ForkStepParameters.builder().parallelNodeId(FIRST_CHILD_ID).parallelNodeId(SECOND_CHILD_ID).build();

    Map<String, ResponseData> responseDataMap =
        ImmutableMap.<String, ResponseData>builder()
            .put(FIRST_CHILD_ID, StepResponseNotifyData.builder().status(Status.SUCCEEDED).build())
            .put(SECOND_CHILD_ID, StepResponseNotifyData.builder().status(Status.FAILED).build())
            .build();
    StepResponse stepResponse = forkStep.handleChildrenResponse(ambiance, stateParameters, responseDataMap);
    assertThat(stepResponse.getStatus()).isEqualTo(Status.FAILED);
  }
}
