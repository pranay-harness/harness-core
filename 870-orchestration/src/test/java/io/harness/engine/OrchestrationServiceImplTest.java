package io.harness.engine;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.execution.PlanExecution;
import io.harness.execution.PlanExecutionMetadata;
import io.harness.plan.Plan;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.steps.StepType;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Spy;

public class OrchestrationServiceImplTest extends OrchestrationTestBase {
  private static final String PLAN_EXECUTION_ID = generateUuid();
  private static final String PLAN_ID = generateUuid();
  private static final String DUMMY_NODE_1_ID = generateUuid();
  private static final String DUMMY_NODE_2_ID = generateUuid();
  private static final String DUMMY_NODE_3_ID = generateUuid();

  private static final String ACCOUNT_ID = generateUuid();
  private static final String APP_ID = generateUuid();

  @Spy @Inject private OrchestrationServiceImpl orchestrationService;

  private static final StepType DUMMY_STEP_TYPE = StepType.newBuilder().setType("DUMMY").build();

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestStartExecution() {
    Plan plan = Plan.builder()
                    .uuid(PLAN_ID)
                    .node(PlanNodeProto.newBuilder()
                              .setUuid(DUMMY_NODE_1_ID)
                              .setName("Dummy Node 1")
                              .setStepType(DUMMY_STEP_TYPE)
                              .setIdentifier("dummy1")
                              .build())
                    .node(PlanNodeProto.newBuilder()
                              .setUuid(DUMMY_NODE_2_ID)
                              .setName("Dummy Node 2")
                              .setStepType(DUMMY_STEP_TYPE)
                              .setIdentifier("dummy2")
                              .build())
                    .node(PlanNodeProto.newBuilder()
                              .setUuid(DUMMY_NODE_3_ID)
                              .setName("Dummy Node 3")
                              .setStepType(DUMMY_STEP_TYPE)
                              .setIdentifier("dummy3")
                              .build())
                    .startingNodeId(DUMMY_NODE_1_ID)
                    .build();
    Map<String, String> setupAbstractions = ImmutableMap.of("accountId", ACCOUNT_ID, "appId", APP_ID);
    ExecutionMetadata metadata = ExecutionMetadata.newBuilder().setExecutionUuid(PLAN_EXECUTION_ID).build();

    PlanExecution planExecution =
        orchestrationService.startExecution(plan, setupAbstractions, metadata, PlanExecutionMetadata.builder().build());

    assertThat(planExecution.getUuid()).isEqualTo(PLAN_EXECUTION_ID);
    assertThat(planExecution.getStatus()).isEqualTo(Status.RUNNING);
    ArgumentCaptor<Ambiance> ambianceCaptor = ArgumentCaptor.forClass(Ambiance.class);
    ArgumentCaptor<PlanNodeProto> nodeCaptor = ArgumentCaptor.forClass(PlanNodeProto.class);
    verify(orchestrationService, times(1)).submitToEngine(ambianceCaptor.capture(), nodeCaptor.capture());

    Ambiance ambiance = ambianceCaptor.getValue();
    assertThat(ambiance.getPlanExecutionId()).isEqualTo(PLAN_EXECUTION_ID);
    assertThat(ambiance.getPlanId()).isEqualTo(PLAN_ID);
    assertThat(ambiance.getSetupAbstractionsMap()).isEqualTo(setupAbstractions);

    PlanNodeProto planNodeProto = nodeCaptor.getValue();
    assertThat(planNodeProto.getUuid()).isEqualTo(DUMMY_NODE_1_ID);
  }
}