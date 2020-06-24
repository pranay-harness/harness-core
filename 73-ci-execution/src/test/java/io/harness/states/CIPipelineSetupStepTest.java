package io.harness.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.HARSH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.beans.CIPipeline;
import io.harness.beans.CIPipelineSetupParameters;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ResponseData;
import io.harness.execution.status.Status;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTest;
import io.harness.facilitator.modes.child.ChildExecutableResponse;
import io.harness.rule.Owner;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponseNotifyData;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.HashMap;
import java.util.Map;

public class CIPipelineSetupStepTest extends CIExecutionTest {
  @Inject private CIPipelineSetupStep ciPipelineSetupStep;
  @Inject private CIExecutionPlanTestHelper ciExecutionPlanTestHelper;
  private CIPipeline ciPipeline;

  private static final String CHILD_ID = generateUuid();

  @Before
  public void setUp() {
    ciPipeline = ciExecutionPlanTestHelper.getCIPipeline();
  }
  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldObtainChild() {
    Ambiance ambiance = Ambiance.builder().build();
    Map<String, String> fieldToExecutionNodeIdMap = new HashMap<>();
    fieldToExecutionNodeIdMap.put("stages", CHILD_ID);
    CIPipelineSetupParameters stateParameters = CIPipelineSetupParameters.builder()
                                                    .ciPipeline(ciPipeline)
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