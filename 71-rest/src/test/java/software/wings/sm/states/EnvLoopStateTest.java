package software.wings.sm.states;

import static io.harness.rule.OwnerRule.POOJA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.utils.WingsTestConstants.APP_ID;

import com.google.common.collect.ImmutableList;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.StateTypeDescriptor;
import software.wings.sm.states.ForkState.ForkStateExecutionData;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class EnvLoopStateTest extends WingsBaseTest {
  @Mock private ExecutionContextImpl context;
  @Mock private WorkflowService workflowService;
  @InjectMocks private EnvLoopState envLoopState = new EnvLoopState("ENV_LOOP_STATE");

  @Before
  public void setUp() throws Exception {
    envLoopState.setLoopedVarName("infra1");
    envLoopState.setLoopedValues(ImmutableList.of("infraVal1", "infraVal2"));
    envLoopState.setPipelineId("PIPELINE_ID");
    envLoopState.setPipelineStageElementId("PSE_ID");
    envLoopState.setPipelineStageParallelIndex(0);
    envLoopState.setStageName("STAGE_1");
    envLoopState.setWorkflowId("WORKFLOW_ID");
    Map<String, String> workflowVariables = new HashMap<>();
    workflowVariables.put("infra1", "infraVal1,infralVal2");
    envLoopState.setWorkflowVariables(workflowVariables);

    when(context.getAppId()).thenReturn(APP_ID);
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void shouldExecute() {
    StateExecutionInstance stateExecutionInstance = aStateExecutionInstance().uuid("UUID").build();
    when(context.getStateExecutionInstance()).thenReturn(stateExecutionInstance);

    Map<String, StateTypeDescriptor> stencilMap = new HashMap<>();
    stencilMap.put(StateType.ENV_STATE.getType(), StateType.ENV_LOOP_STATE);
    when(workflowService.stencilMap(anyString())).thenReturn(stencilMap);
    ExecutionResponse executionResponse = envLoopState.execute(context);
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.isAsync()).isTrue();
    assertThat(executionResponse.getCorrelationIds().size()).isEqualTo(2);
    assertThat(executionResponse.getStateExecutionData()).isInstanceOf(ForkStateExecutionData.class);
    ForkStateExecutionData forkStateExecutionData = (ForkStateExecutionData) executionResponse.getStateExecutionData();
    assertThat(forkStateExecutionData.getElements().size()).isEqualTo(2);
    assertThat(forkStateExecutionData.getForkStateNames().size()).isEqualTo(2);
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void shouldGetTimeout() {
    Integer timeoutMillis = envLoopState.getTimeoutMillis();
    assertThat(timeoutMillis).isEqualTo(EnvState.ENV_STATE_TIMEOUT_MILLIS);
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void shouldGetSetTimeout() {
    envLoopState.setTimeoutMillis((int) TimeUnit.HOURS.toMillis(1));
    Integer timeoutMillis = envLoopState.getTimeoutMillis();
    assertThat(timeoutMillis).isEqualTo((int) TimeUnit.HOURS.toMillis(1));
  }
}