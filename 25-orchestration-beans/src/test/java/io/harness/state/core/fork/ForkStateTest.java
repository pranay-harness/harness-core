package io.harness.state.core.fork;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.OrchestrationBeansTest;
import io.harness.ambiance.Ambiance;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ResponseData;
import io.harness.facilitate.modes.children.ChildrenExecutableResponse;
import io.harness.facilitate.modes.children.ChildrenExecutableResponse.Child;
import io.harness.rule.Owner;
import io.harness.state.StateType;
import io.harness.state.execution.status.NodeExecutionStatus;
import io.harness.state.io.StateResponse;
import io.harness.state.io.StateTransput;
import io.harness.state.io.StatusNotifyResponseData;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ForkStateTest extends OrchestrationBeansTest {
  @Inject private ForkState forkState;

  private static final String FIRST_CHILD_ID = generateUuid();
  private static final String SECOND_CHILD_ID = generateUuid();
  private static final StateType STATE_TYPE = StateType.builder().type("FORK").build();

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestGetStateType() {
    assertThat(forkState.getStateType()).isEqualTo(STATE_TYPE);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestObtainChildren() {
    Ambiance ambiance = Ambiance.builder().build();
    List<StateTransput> stateTransputList = new ArrayList<>();
    ForkStateParameters stateParameters =
        ForkStateParameters.builder().parallelNodeId(FIRST_CHILD_ID).parallelNodeId(SECOND_CHILD_ID).build();
    ChildrenExecutableResponse childrenExecutableResponse =
        forkState.obtainChildren(ambiance, stateParameters, stateTransputList);
    assertThat(childrenExecutableResponse).isNotNull();
    assertThat(childrenExecutableResponse.getChildren()).hasSize(2);
    List<String> childIds =
        childrenExecutableResponse.getChildren().stream().map(Child::getChildNodeId).collect(Collectors.toList());
    assertThat(childIds).hasSize(2);
    assertThat(childIds).containsExactlyInAnyOrder(FIRST_CHILD_ID, SECOND_CHILD_ID);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestHandleAsyncResponse() {
    Ambiance ambiance = Ambiance.builder().build();
    ForkStateParameters stateParameters =
        ForkStateParameters.builder().parallelNodeId(FIRST_CHILD_ID).parallelNodeId(SECOND_CHILD_ID).build();

    Map<String, ResponseData> responseDataMap =
        ImmutableMap.<String, ResponseData>builder()
            .put(FIRST_CHILD_ID, StatusNotifyResponseData.builder().status(NodeExecutionStatus.SUCCEEDED).build())
            .put(SECOND_CHILD_ID, StatusNotifyResponseData.builder().status(NodeExecutionStatus.FAILED).build())
            .build();
    StateResponse stateResponse = forkState.handleAsyncResponse(ambiance, stateParameters, responseDataMap);
    assertThat(stateResponse.getStatus()).isEqualTo(NodeExecutionStatus.FAILED);
  }
}