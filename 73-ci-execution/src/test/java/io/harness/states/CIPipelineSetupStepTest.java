package io.harness.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.HARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.beans.CIPipeline;
import io.harness.beans.CIPipelineSetupParameters;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.category.element.UnitTests;
import io.harness.ci.beans.entities.BuildNumber;
import io.harness.engine.outputs.ExecutionSweepingOutputService;
import io.harness.execution.status.Status;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTest;
import io.harness.facilitator.modes.child.ChildExecutableResponse;
import io.harness.rule.Owner;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponseNotifyData;
import io.harness.tasks.ResponseData;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;

public class CIPipelineSetupStepTest extends CIExecutionTest {
  @Inject private CIPipelineSetupStep ciPipelineSetupStep;
  @Inject private CIExecutionPlanTestHelper ciExecutionPlanTestHelper;
  @Mock ExecutionSweepingOutputService executionSweepingOutputResolver;
  private CIPipeline ciPipeline;

  private static final String CHILD_ID = generateUuid();

  @Before
  public void setUp() {
    ciPipeline = ciExecutionPlanTestHelper.getCIPipeline();
    on(ciPipelineSetupStep).set("executionSweepingOutputResolver", executionSweepingOutputResolver);
  }
  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldObtainChild() {
    Ambiance ambiance = Ambiance.builder().build();
    when(executionSweepingOutputResolver.consume(any(), any(), any(), any())).thenReturn("namespace");
    Map<String, String> fieldToExecutionNodeIdMap = new HashMap<>();
    fieldToExecutionNodeIdMap.put("stages", CHILD_ID);
    BuildNumber buildNumber = BuildNumber.builder().buildNumber(1L).build();
    CIPipelineSetupParameters stateParameters =
        CIPipelineSetupParameters.builder()
            .ciPipeline(ciPipeline)
            .ciExecutionArgs(CIExecutionArgs.builder().buildNumber(buildNumber).build())
            .fieldToExecutionNodeIdMap(fieldToExecutionNodeIdMap)
            .build();
    ChildExecutableResponse childExecutableResponse =
        ciPipelineSetupStep.obtainChild(ambiance, stateParameters, StepInputPackage.builder().build());
    assertThat(childExecutableResponse).isNotNull();
    assertThat(childExecutableResponse.getChildNodeId()).isEqualTo(CHILD_ID);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldHandleChildResponse() {
    Ambiance ambiance = Ambiance.builder().build();
    Map<String, String> fieldToExecutionNodeIdMap = new HashMap<>();
    fieldToExecutionNodeIdMap.put("stages", CHILD_ID);
    CIPipelineSetupParameters stateParameters = CIPipelineSetupParameters.builder()
                                                    .ciPipeline(ciPipeline)
                                                    .fieldToExecutionNodeIdMap(fieldToExecutionNodeIdMap)
                                                    .build();

    Map<String, ResponseData> responseDataMap =
        ImmutableMap.<String, ResponseData>builder()
            .put(CHILD_ID, StepResponseNotifyData.builder().status(Status.FAILED).build())
            .build();
    StepResponse stepResponse = ciPipelineSetupStep.handleChildResponse(ambiance, stateParameters, responseDataMap);
    assertThat(stepResponse.getStatus()).isEqualTo(Status.FAILED);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldExecuteSync() {
    Ambiance ambiance = Ambiance.builder().build();
    Map<String, String> fieldToExecutionNodeIdMap = new HashMap<>();
    fieldToExecutionNodeIdMap.put("stages", CHILD_ID);
    CIPipelineSetupParameters stateParameters = CIPipelineSetupParameters.builder()
                                                    .ciPipeline(ciPipeline)
                                                    .fieldToExecutionNodeIdMap(fieldToExecutionNodeIdMap)
                                                    .build();
    assertThat(ciPipelineSetupStep.executeSync(ambiance, stateParameters, StepInputPackage.builder().build(), null)
                   .getStatus())
        .isEqualTo(Status.SUCCEEDED);
  }
}