package io.harness.engine;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.execution.status.Status.SUCCEEDED;
import static io.harness.rule.OwnerRule.ALEXEI;
import static io.harness.utils.steps.TestAsyncStep.ASYNC_STEP_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import io.harness.facilitator.DefaultFacilitatorParams;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.maintenance.MaintenanceGuard;
import io.harness.plan.Plan;
import io.harness.plan.PlanNode;
import io.harness.plan.input.InputArgs;
import io.harness.registries.adviser.AdviserRegistry;
import io.harness.registries.state.StepRegistry;
import io.harness.rule.Owner;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepTransput;
import io.harness.testlib.RealMongo;
import io.harness.utils.steps.TestAsyncStep;
import io.harness.utils.steps.TestStepParameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Duration;
import java.util.List;

public class ExecutionEngineTest extends OrchestrationTest {
  @Inject private AdviserRegistry adviserRegistry;
  @Inject private StepRegistry stepRegistry;
  @Inject private ExecutionEngine executionEngine;
  @Inject private EngineTestHelper engineTestHelper;

  private static final AdviserType TEST_ADVISER_TYPE =
      AdviserType.builder().type("TEST_HTTP_RESPONSE_CODE_SWITCH").build();
  private static final StepType TEST_STEP_TYPE = StepType.builder().type("TEST_STEP_PLAN").build();
  private static final StepType DUMMY_STEP_TYPE = StepType.builder().type("DUMMY").build();

  @Before
  public void setUp() {
    adviserRegistry.register(TEST_ADVISER_TYPE, TestHttpResponseCodeSwitchAdviser.class);
    stepRegistry.register(TEST_STEP_TYPE, TestSyncStep.class);
    stepRegistry.register(ASYNC_STEP_TYPE, TestAsyncStep.class);
  }

  @Test
  @RealMongo
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldStartOneNodeExecution() {
    String testNodeId = generateUuid();
    Plan oneNodePlan =
        Plan.builder()
            .uuid(generateUuid())
            .node(PlanNode.builder()
                      .name("Test Node")
                      .uuid(testNodeId)
                      .identifier("test1")
                      .stepType(TEST_STEP_TYPE)
                      .facilitatorObtainment(FacilitatorObtainment.builder()
                                                 .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                                 .build())
                      .build())
            .startingNodeId(testNodeId)
            .build();

    EmbeddedUser user = new EmbeddedUser(generateUuid(), ALEXEI, ALEXEI);

    PlanExecution response = executionEngine.startExecution(oneNodePlan, prepareInputArgs(), user);

    engineTestHelper.waitForPlanCompletion(response.getUuid());
    response = engineTestHelper.getPlanExecutionStatus(response.getUuid());

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(SUCCEEDED);
  }

  @Test
  @RealMongo
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldStartSyncExecution() {
    String testStartNodeId = generateUuid();
    String testSecondNodeId = generateUuid();
    Plan oneNodePlan =
        Plan.builder()
            .uuid(generateUuid())
            .node(PlanNode.builder()
                      .name("Test Node")
                      .uuid(testStartNodeId)
                      .identifier("test1")
                      .stepType(TEST_STEP_TYPE)
                      .adviserObtainment(
                          AdviserObtainment.builder()
                              .type(OnSuccessAdviser.ADVISER_TYPE)
                              .parameters(OnSuccessAdviserParameters.builder().nextNodeId(testSecondNodeId).build())
                              .build())
                      .facilitatorObtainment(FacilitatorObtainment.builder()
                                                 .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                                 .build())
                      .build())
            .node(PlanNode.builder()
                      .name("Test Node 2")
                      .uuid(testSecondNodeId)
                      .identifier("test2")
                      .stepType(DUMMY_STEP_TYPE)
                      .facilitatorObtainment(FacilitatorObtainment.builder()
                                                 .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                                 .build())
                      .build())
            .startingNodeId(testStartNodeId)
            .build();

    EmbeddedUser user = new EmbeddedUser(generateUuid(), ALEXEI, ALEXEI);

    PlanExecution response = executionEngine.startExecution(oneNodePlan, prepareInputArgs(), user);

    engineTestHelper.waitForPlanCompletion(response.getUuid());
    response = engineTestHelper.getPlanExecutionStatus(response.getUuid());

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(SUCCEEDED);
  }

  @Test
  @RealMongo
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldStartAsyncExecution() {
    String testStartNodeId = generateUuid();
    String testWaitNodeId = generateUuid();
    Plan oneNodePlan =
        Plan.builder()
            .uuid(generateUuid())
            .node(PlanNode.builder()
                      .name("Test Node")
                      .uuid(testStartNodeId)
                      .identifier("test1")
                      .stepType(TEST_STEP_TYPE)
                      .adviserObtainment(
                          AdviserObtainment.builder()
                              .type(OnSuccessAdviser.ADVISER_TYPE)
                              .parameters(OnSuccessAdviserParameters.builder().nextNodeId(testWaitNodeId).build())
                              .build())
                      .facilitatorObtainment(FacilitatorObtainment.builder()
                                                 .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                                 .build())
                      .build())
            .node(PlanNode.builder()
                      .uuid(testWaitNodeId)
                      .name("Finish Node")
                      .identifier("finish")
                      .stepType(ASYNC_STEP_TYPE)
                      .stepParameters(TestStepParameters.builder().param("Param").build())
                      .facilitatorObtainment(
                          FacilitatorObtainment.builder()
                              .type(FacilitatorType.builder().type(FacilitatorType.ASYNC).build())
                              .parameters(
                                  DefaultFacilitatorParams.builder().waitDurationSeconds(Duration.ofSeconds(2)).build())
                              .build())
                      .build())
            .startingNodeId(testStartNodeId)
            .build();

    EmbeddedUser user = new EmbeddedUser(generateUuid(), ALEXEI, ALEXEI);

    try (MaintenanceGuard guard = new MaintenanceGuard(false)) {
      PlanExecution response = executionEngine.startExecution(oneNodePlan, prepareInputArgs(), user);

      engineTestHelper.waitForPlanCompletion(response.getUuid());
      response = engineTestHelper.getPlanExecutionStatus(response.getUuid());

      assertThat(response).isNotNull();
      assertThat(response.getStatus()).isEqualTo(SUCCEEDED);
    }
  }

  @Test
  @RealMongo
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldThrowInvalidRequestException() {
    final String exceptionStartMessage = "No node found with Id";
    Plan oneNodePlan = Plan.builder().uuid(generateUuid()).startingNodeId(generateUuid()).build();

    EmbeddedUser user = new EmbeddedUser(generateUuid(), ALEXEI, ALEXEI);

    assertThatThrownBy(() -> executionEngine.startExecution(oneNodePlan, prepareInputArgs(), user))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageStartingWith(exceptionStartMessage);
  }

  private static InputArgs prepareInputArgs() {
    return InputArgs.builder()
        .put("accountId", "kmpySmUISimoRrJL6NL73w")
        .put("appId", "XEsfW6D_RJm1IaGpDidD3g")
        .build();
  }

  private static class TestHttpResponseCodeSwitchAdviser implements Adviser {
    @Override
    public Advise onAdviseEvent(AdvisingEvent advisingEvent) {
      return null;
    }
  }

  private static class TestSyncStep implements Step, SyncExecutable {
    @Override
    public StepResponse executeSync(
        Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs, PassThroughData passThroughData) {
      return StepResponse.builder().status(SUCCEEDED).build();
    }
  }
}
