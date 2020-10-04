package io.harness.steps.section;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.OrchestrationStepsTestBase;
import io.harness.ambiance.Ambiance;
import io.harness.category.element.UnitTests;
import io.harness.execution.status.Status;
import io.harness.facilitator.modes.child.ChildExecutableResponse;
import io.harness.rule.Owner;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponseNotifyData;
import io.harness.tasks.ResponseData;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Map;

public class SectionStepTest extends OrchestrationStepsTestBase {
  @Inject private SectionStep sectionState;

  private static final String CHILD_ID = generateUuid();

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestObtainChildren() {
    Ambiance ambiance = Ambiance.builder().build();
    StepInputPackage inputPackage = StepInputPackage.builder().build();
    SectionStepParameters stateParameters = SectionStepParameters.builder().childNodeId(CHILD_ID).build();
    ChildExecutableResponse childExecutableResponse = sectionState.obtainChild(ambiance, stateParameters, inputPackage);
    assertThat(childExecutableResponse).isNotNull();
    assertThat(childExecutableResponse.getChildNodeId()).isEqualTo(CHILD_ID);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestHandleChildResponse() {
    Ambiance ambiance = Ambiance.builder().build();
    SectionStepParameters stateParameters = SectionStepParameters.builder().childNodeId(CHILD_ID).build();

    Map<String, ResponseData> responseDataMap =
        ImmutableMap.<String, ResponseData>builder()
            .put(CHILD_ID, StepResponseNotifyData.builder().status(Status.FAILED).build())
            .build();
    StepResponse stepResponse = sectionState.handleChildResponse(ambiance, stateParameters, responseDataMap);
    assertThat(stepResponse.getStatus()).isEqualTo(Status.FAILED);
  }
}