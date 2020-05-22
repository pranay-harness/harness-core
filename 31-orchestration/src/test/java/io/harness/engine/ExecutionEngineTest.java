package io.harness.engine;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ALEXEI;
import static io.harness.utils.TestAsyncStep.ASYNC_STATE_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.OrchestrationTest;
import io.harness.adviser.Advise;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserObtainment;
import io.harness.adviser.AdviserType;
import io.harness.adviser.AdvisingEvent;
import io.harness.adviser.impl.success.OnSuccessAdviser;
import io.harness.adviser.impl.success.OnSuccessAdviserParameters;
import io.harness.ambiance.Ambiance;
import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.PlanExecution;
import io.harness.execution.PlanExecution.PlanExecutionKeys;
import io.harness.execution.status.ExecutionInstanceStatus;
import io.harness.execution.status.NodeExecutionStatus;
import io.harness.facilitator.DefaultFacilitatorParams;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.maintenance.MaintenanceGuard;
import io.harness.persistence.HPersistence;
import io.harness.plan.ExecutionNode;
import io.harness.plan.Plan;
import io.harness.registries.adviser.AdviserRegistry;
import io.harness.registries.state.StepRegistry;
import io.harness.rule.Owner;
import io.harness.state.StateType;
import io.harness.state.Step;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepTransput;
import io.harness.testlib.RealMongo;
import io.harness.utils.TestAsyncStep;
import io.harness.utils.TestStepParameters;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ExecutionEngineTest extends OrchestrationTest {
  @Inject private AdviserRegistry adviserRegistry;
  @Inject private StepRegistry stepRegistry;
  @Inject private HPersistence hPersistence;
  @Inject private ExecutionEngine executionEngine;

  private static final AdviserType TEST_ADVISER_TYPE =
      AdviserType.builder().type("TEST_HTTP_RESPONSE_CODE_SWITCH").build();
  private static final StateType TEST_STATE_TYPE = StateType.builder().type("TEST_STATE_PLAN").build();
  private static final StateType DUMMY_STATE_TYPE = StateType.builder().type("DUMMY").build();

  @Before
  public void setUp() {
    adviserRegistry.register(TEST_ADVISER_TYPE, TestHttpResponseCodeSwitchAdviser.class);
    stepRegistry.register(TEST_STATE_TYPE, TestSyncStep.class);
    stepRegistry.register(ASYNC_STATE_TYPE, TestAsyncStep.class);
  }

  @Test
  @RealMongo
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldStartSyncExecution() {
    String testNodeId = generateUuid();
    Plan oneNodePlan =
        Plan.builder()
            .uuid(generateUuid())
            .node(ExecutionNode.builder()
                      .name("Test Node")
                      .uuid(testNodeId)
                      .identifier("test1")
                      .stateType(TEST_STATE_TYPE)
                      .facilitatorObtainment(FacilitatorObtainment.builder()
                                                 .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                                 .build())
                      .build())
            .startingNodeId(testNodeId)
            .setupAbstractions(ImmutableMap.<String, String>builder()
                                   .put("accountId", "kmpySmUISimoRrJL6NL73w")
                                   .put("appId", "XEsfW6D_RJm1IaGpDidD3g")
                                   .build())
            .build();

    EmbeddedUser user = new EmbeddedUser(generateUuid(), ALEXEI, ALEXEI);

    PlanExecution response = executionEngine.startExecution(oneNodePlan, user);

    waitForPlanCompletion(response.getUuid());
    response = getPlanExecutionStatus(response.getUuid());

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(ExecutionInstanceStatus.SUCCEEDED);
  }

  @Test
  @RealMongo
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldStartAsyncExecution() {
    String testStartNodeId = generateUuid();
    String testSecondNodeId = generateUuid();
    Plan oneNodePlan =
        Plan.builder()
            .uuid(generateUuid())
            .node(ExecutionNode.builder()
                      .name("Test Node")
                      .uuid(testStartNodeId)
                      .identifier("test1")
                      .stateType(TEST_STATE_TYPE)
                      .adviserObtainment(
                          AdviserObtainment.builder()
                              .type(OnSuccessAdviser.ADVISER_TYPE)
                              .parameters(OnSuccessAdviserParameters.builder().nextNodeId(testSecondNodeId).build())
                              .build())
                      .facilitatorObtainment(FacilitatorObtainment.builder()
                                                 .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                                 .build())
                      .build())
            .node(ExecutionNode.builder()
                      .name("Test Node 2")
                      .uuid(testSecondNodeId)
                      .identifier("test2")
                      .stateType(DUMMY_STATE_TYPE)
                      .facilitatorObtainment(FacilitatorObtainment.builder()
                                                 .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                                 .build())
                      .build())
            .startingNodeId(testStartNodeId)
            .setupAbstractions(ImmutableMap.<String, String>builder()
                                   .put("accountId", "kmpySmUISimoRrJL6NL73w")
                                   .put("appId", "XEsfW6D_RJm1IaGpDidD3g")
                                   .build())
            .build();

    EmbeddedUser user = new EmbeddedUser(generateUuid(), ALEXEI, ALEXEI);

    PlanExecution response = executionEngine.startExecution(oneNodePlan, user);

    waitForPlanCompletion(response.getUuid());
    response = getPlanExecutionStatus(response.getUuid());

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(ExecutionInstanceStatus.SUCCEEDED);
  }

  @Test
  @RealMongo
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestShit() {
    String testStartNodeId = generateUuid();
    String testWaitNodeId = generateUuid();
    Plan oneNodePlan =
        Plan.builder()
            .uuid(generateUuid())
            .node(ExecutionNode.builder()
                      .name("Test Node")
                      .uuid(testStartNodeId)
                      .identifier("test1")
                      .stateType(TEST_STATE_TYPE)
                      .adviserObtainment(
                          AdviserObtainment.builder()
                              .type(OnSuccessAdviser.ADVISER_TYPE)
                              .parameters(OnSuccessAdviserParameters.builder().nextNodeId(testWaitNodeId).build())
                              .build())
                      .facilitatorObtainment(FacilitatorObtainment.builder()
                                                 .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                                 .build())
                      .build())
            .node(ExecutionNode.builder()
                      .uuid(testWaitNodeId)
                      .name("Finish Node")
                      .identifier("finish")
                      .stateType(ASYNC_STATE_TYPE)
                      .stepParameters(TestStepParameters.builder().param("Param").build())
                      .facilitatorObtainment(
                          FacilitatorObtainment.builder()
                              .type(FacilitatorType.builder().type(FacilitatorType.ASYNC).build())
                              .parameters(
                                  DefaultFacilitatorParams.builder().waitDurationSeconds(Duration.ofSeconds(2)).build())
                              .build())
                      .build())
            .startingNodeId(testStartNodeId)
            .setupAbstractions(ImmutableMap.<String, String>builder()
                                   .put("accountId", "kmpySmUISimoRrJL6NL73w")
                                   .put("appId", "XEsfW6D_RJm1IaGpDidD3g")
                                   .build())
            .build();

    EmbeddedUser user = new EmbeddedUser(generateUuid(), ALEXEI, ALEXEI);

    try (MaintenanceGuard guard = new MaintenanceGuard(false)) {
      PlanExecution response = executionEngine.startExecution(oneNodePlan, user);

      waitForPlanCompletion(response.getUuid());
      response = getPlanExecutionStatus(response.getUuid());

      assertThat(response).isNotNull();
      assertThat(response.getStatus()).isEqualTo(ExecutionInstanceStatus.SUCCEEDED);
    }
  }

  @Test
  @RealMongo
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldThrowInvalidRequestException() {
    final String exceptionStartMessage = "No node found with Id";
    Plan oneNodePlan = Plan.builder()
                           .uuid(generateUuid())
                           .startingNodeId(generateUuid())
                           .setupAbstractions(ImmutableMap.<String, String>builder()
                                                  .put("accountId", "kmpySmUISimoRrJL6NL73w")
                                                  .put("appId", "XEsfW6D_RJm1IaGpDidD3g")
                                                  .build())
                           .build();

    EmbeddedUser user = new EmbeddedUser(generateUuid(), ALEXEI, ALEXEI);

    assertThatThrownBy(() -> executionEngine.startExecution(oneNodePlan, user))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageStartingWith(exceptionStartMessage);
  }

  private void waitForPlanCompletion(String uuid) {
    final String finalStatusEnding = "ED";
    Awaitility.await().atMost(15, TimeUnit.MINUTES).pollInterval(5, TimeUnit.SECONDS).until(() -> {
      final PlanExecution planExecution = getPlanExecutionStatus(uuid);
      return planExecution != null && planExecution.getStatus().name().endsWith(finalStatusEnding);
    });
  }

  private PlanExecution getPlanExecutionStatus(String uuid) {
    return hPersistence.createQuery(PlanExecution.class)
        .filter(PlanExecutionKeys.uuid, uuid)
        .project(PlanExecutionKeys.status, true)
        .get();
  }

  private static class TestHttpResponseCodeSwitchAdviser implements Adviser {
    @Override
    public AdviserType getType() {
      return TEST_ADVISER_TYPE;
    }

    @Override
    public Advise onAdviseEvent(AdvisingEvent advisingEvent) {
      return null;
    }
  }

  private static class TestSyncStep implements Step, SyncExecutable {
    @Override
    public StepResponse executeSync(
        Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs, PassThroughData passThroughData) {
      return StepResponse.builder().status(NodeExecutionStatus.SUCCEEDED).build();
    }

    @Override
    public StateType getType() {
      return TEST_STATE_TYPE;
    }
  }
}
