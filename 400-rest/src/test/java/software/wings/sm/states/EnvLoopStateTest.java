/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.POOJA;

import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.utils.WingsTestConstants.APP_ID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateExecutionInstanceHelper;
import software.wings.sm.StateType;
import software.wings.sm.StateTypeDescriptor;
import software.wings.sm.states.ForkState.ForkStateExecutionData;

import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class EnvLoopStateTest extends WingsBaseTest {
  private static final String DISABLE_ASSERTION = "${expr}";
  @Mock private ExecutionContextImpl context;
  @Mock private WorkflowService workflowService;
  @Mock private StateExecutionInstanceHelper stateExecutionInstanceHelper;
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
    envLoopState.setDisableAssertion(DISABLE_ASSERTION);
    Map<String, String> workflowVariables = new HashMap<>();
    workflowVariables.put("infra1", "infraVal1,infralVal2");
    envLoopState.setWorkflowVariables(workflowVariables);
    when(context.renderExpression(DISABLE_ASSERTION)).thenReturn("expr");
    when(context.getAppId()).thenReturn(APP_ID);
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void shouldExecute() {
    StateExecutionInstance stateExecutionInstance = aStateExecutionInstance().uuid("UUID").build();
    when(context.getStateExecutionInstance()).thenReturn(stateExecutionInstance);
    when(stateExecutionInstanceHelper.clone(stateExecutionInstance)).thenReturn(stateExecutionInstance);
    Map<String, StateTypeDescriptor> stencilMap = new HashMap<>();
    stencilMap.put(StateType.ENV_STATE.getType(), StateType.ENV_LOOP_STATE);
    when(workflowService.stencilMap(anyString())).thenReturn(stencilMap);
    ExecutionResponse executionResponse = envLoopState.execute(context);
    verify(context).renderExpression(DISABLE_ASSERTION);
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
