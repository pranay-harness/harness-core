package io.harness.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.beans.stages.IntegrationStage;
import io.harness.beans.stages.IntegrationStageStepParameters;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ResponseData;
import io.harness.execution.status.Status;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTest;
import io.harness.facilitator.modes.child.ChildExecutableResponse;
import io.harness.resolver.sweepingoutput.ExecutionSweepingOutputService;
import io.harness.rule.Owner;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponseNotifyData;
import io.harness.state.io.StepTransput;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IntegrationStageStepTest extends CIExecutionTest {
  @Inject private IntegrationStageStep integrationStageStep;
  @Inject private CIExecutionPlanTestHelper ciExecutionPlanTestHelper;
  private IntegrationStage integrationStage;
  @Mock private ExecutionSweepingOutputService executionSweepingOutputResolver;

  private static final String CHILD_ID = generateUuid();

  @Before
  public void setUp() {
    integrationStage = ciExecutionPlanTestHelper.getIntegrationStage();
    on(integrationStageStep).set("executionSweepingOutputResolver", executionSweepingOutputResolver);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldObtainChild() {
    when(executionSweepingOutputResolver.consume(any(), any(), any(), any())).thenReturn("namespace");
    Ambiance ambiance = Ambiance.builder().build();
    List<StepTransput> stepTransputList = new ArrayList<>();
    Map<String, String> fieldToExecutionNodeIdMap = new HashMap<>();
    fieldToExecutionNodeIdMap.put("execution", CHILD_ID);
    IntegrationStageStepParameters stateParameters = IntegrationStageStepParameters.builder()
                                                         .integrationStage(integrationStage)
                                                         .podName("podname")
                                                         .fieldToExecutionNodeIdMap(fieldToExecutionNodeIdMap)
                                                         .build();
    ChildExecutableResponse childExecutableResponse =
        integrationStageStep.obtainChild(ambiance, stateParameters, stepTransputList);
    assertThat(childExecutableResponse).isNotNull();
    assertThat(childExecutableResponse.getChildNodeId()).isEqualTo(CHILD_ID);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void handleChildResponse() {
    Ambiance ambiance = Ambiance.builder().build();
    Map<String, String> fieldToExecutionNodeIdMap = new HashMap<>();
    fieldToExecutionNodeIdMap.put("execution", CHILD_ID);
    IntegrationStageStepParameters stateParameters = IntegrationStageStepParameters.builder()
                                                         .integrationStage(integrationStage)
                                                         .fieldToExecutionNodeIdMap(fieldToExecutionNodeIdMap)
                                                         .build();

    Map<String, ResponseData> responseDataMap =
        ImmutableMap.<String, ResponseData>builder()
            .put(CHILD_ID, StepResponseNotifyData.builder().status(Status.FAILED).build())
            .build();
    StepResponse stepResponse = integrationStageStep.handleChildResponse(ambiance, stateParameters, responseDataMap);
    assertThat(stepResponse.getStatus()).isEqualTo(Status.FAILED);
  }
}