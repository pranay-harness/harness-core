package io.harness.state.core.section;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.OrchestrationBeansTest;
import io.harness.ambiance.Ambiance;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ResponseData;
import io.harness.execution.status.NodeExecutionStatus;
import io.harness.facilitator.modes.child.ChildExecutableResponse;
import io.harness.rule.Owner;
import io.harness.state.StateType;
import io.harness.state.io.StateResponse;
import io.harness.state.io.StateTransput;
import io.harness.state.io.StatusNotifyResponseData;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SectionStateTest extends OrchestrationBeansTest {
  @Inject private SectionState sectionState;

  private static final String CHILD_ID = generateUuid();
  private static final StateType STATE_TYPE = StateType.builder().type("SECTION").build();

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestGetStateType() {
    assertThat(sectionState.getType()).isEqualTo(STATE_TYPE);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestObtainChildren() {
    Ambiance ambiance = Ambiance.builder().build();
    List<StateTransput> stateTransputList = new ArrayList<>();
    SectionStateParameters stateParameters = SectionStateParameters.builder().childNodeId(CHILD_ID).build();
    ChildExecutableResponse childExecutableResponse =
        sectionState.obtainChild(ambiance, stateParameters, stateTransputList);
    assertThat(childExecutableResponse).isNotNull();
    assertThat(childExecutableResponse.getChildNodeId()).isEqualTo(CHILD_ID);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestHandleAsyncResponse() {
    Ambiance ambiance = Ambiance.builder().build();
    SectionStateParameters stateParameters = SectionStateParameters.builder().childNodeId(CHILD_ID).build();

    Map<String, ResponseData> responseDataMap =
        ImmutableMap.<String, ResponseData>builder()
            .put(CHILD_ID, StatusNotifyResponseData.builder().status(NodeExecutionStatus.FAILED).build())
            .build();
    StateResponse stateResponse = sectionState.handleAsyncResponse(ambiance, stateParameters, responseDataMap);
    assertThat(stateResponse.getStatus()).isEqualTo(NodeExecutionStatus.FAILED);
  }
}