package io.harness.state.core.section;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.OrchestrationTest;
import io.harness.ambiance.Ambiance;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ResponseData;
import io.harness.execution.status.NodeExecutionStatus;
import io.harness.facilitator.modes.child.ChildExecutableResponse;
import io.harness.rule.Owner;
import io.harness.state.StepType;
import io.harness.state.io.StatusNotifyResponseData;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepTransput;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SectionStateTest extends OrchestrationTest {
  @Inject private SectionStep sectionState;

  private static final String CHILD_ID = generateUuid();
  private static final StepType STATE_TYPE = StepType.builder().type("SECTION").build();

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestObtainChildren() {
    Ambiance ambiance = Ambiance.builder().build();
    List<StepTransput> stepTransputList = new ArrayList<>();
    SectionStepParameters stateParameters = SectionStepParameters.builder().childNodeId(CHILD_ID).build();
    ChildExecutableResponse childExecutableResponse =
        sectionState.obtainChild(ambiance, stateParameters, stepTransputList);
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
            .put(CHILD_ID, StatusNotifyResponseData.builder().status(NodeExecutionStatus.FAILED).build())
            .build();
    StepResponse stepResponse = sectionState.handleChildResponse(ambiance, stateParameters, responseDataMap);
    assertThat(stepResponse.getStatus()).isEqualTo(NodeExecutionStatus.FAILED);
  }
}