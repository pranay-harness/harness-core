package io.harness.engine;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.pms.contracts.execution.Status.SUCCEEDED;
import static io.harness.rule.OwnerRule.ALEXEI;
import static io.harness.rule.OwnerRule.GARVIT;
import static io.harness.rule.OwnerRule.PRASHANT;
import static io.harness.utils.steps.TestAsyncStep.ASYNC_STEP_TYPE;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.OrchestrationTestBase;
import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.PlanExecution;
import io.harness.maintenance.MaintenanceGuard;
import io.harness.plan.Plan;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.pipeline.ExecutionTriggerInfo;
import io.harness.pms.pipeline.TriggerType;
import io.harness.pms.sdk.core.adviser.Adviser;
import io.harness.pms.sdk.core.adviser.AdvisingEvent;
import io.harness.pms.sdk.core.adviser.success.OnSuccessAdviser;
import io.harness.pms.sdk.core.adviser.success.OnSuccessAdviserParameters;
import io.harness.pms.sdk.core.facilitator.DefaultFacilitatorParams;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.registries.AdviserRegistry;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.pms.sdk.core.steps.executables.SyncExecutable;
import io.harness.pms.sdk.core.steps.io.EmptyStepParameters;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.harness.utils.steps.TestAsyncStep;
import io.harness.utils.steps.TestStepParameters;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.protobuf.ByteString;
import java.time.Duration;
import java.util.Map;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Ignore("Will enable after setting up listeners")
public class OrchestrationEngineTest extends OrchestrationTestBase {
  @Inject private Injector injector;
  @Inject private AdviserRegistry adviserRegistry;
  @Inject private StepRegistry stepRegistry;
  @Inject private OrchestrationService orchestrationService;
  @Inject private EngineTestHelper engineTestHelper;
  @Inject private KryoSerializer kryoSerializer;

  private static final AdviserType TEST_ADVISER_TYPE =
      AdviserType.newBuilder().setType("TEST_HTTP_RESPONSE_CODE_SWITCH").build();
  private static final StepType TEST_STEP_TYPE = StepType.newBuilder().setType("TEST_STEP_PLAN").build();

  private static final EmbeddedUser embeddedUser =
      EmbeddedUser.builder().email(PRASHANT).name(PRASHANT).uuid(generateUuid()).build();
  private static final ExecutionTriggerInfo triggerInfo =
      ExecutionTriggerInfo.builder().triggerType(TriggerType.MANUAL).triggeredBy(embeddedUser).build();

  @Before
  public void setUp() {
    adviserRegistry.register(TEST_ADVISER_TYPE, injector.getInstance(TestHttpResponseCodeSwitchAdviser.class));
    stepRegistry.register(TEST_STEP_TYPE, injector.getInstance(TestSyncStep.class));
    stepRegistry.register(ASYNC_STEP_TYPE, injector.getInstance(TestAsyncStep.class));
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldStartOneNodeExecution() {
    String testNodeId = generateUuid();
    Plan oneNodePlan =
        Plan.builder()
            .node(PlanNode.builder()
                      .name("Test Node")
                      .uuid(testNodeId)
                      .identifier("test1")
                      .stepType(TEST_STEP_TYPE)
                      .facilitatorObtainment(
                          FacilitatorObtainment.newBuilder()
                              .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                              .build())
                      .build())
            .startingNodeId(testNodeId)
            .build();

    PlanExecution response = orchestrationService.startExecution(oneNodePlan, prepareInputArgs(), triggerInfo);

    engineTestHelper.waitForPlanCompletion(response.getUuid());
    response = engineTestHelper.getPlanExecutionStatus(response.getUuid());

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(SUCCEEDED);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldStartSyncExecution() {
    String testStartNodeId = generateUuid();
    Plan oneNodePlan =
        Plan.builder()
            .node(PlanNode.builder()
                      .name("Test Node")
                      .uuid(testStartNodeId)
                      .identifier("test1")
                      .stepType(TEST_STEP_TYPE)
                      .facilitatorObtainment(
                          FacilitatorObtainment.newBuilder()
                              .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                              .build())
                      .build())
            .startingNodeId(testStartNodeId)
            .build();

    PlanExecution response = orchestrationService.startExecution(oneNodePlan, prepareInputArgs(), triggerInfo);

    engineTestHelper.waitForPlanCompletion(response.getUuid());
    response = engineTestHelper.getPlanExecutionStatus(response.getUuid());

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(SUCCEEDED);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldStartAsyncExecution() {
    String testStartNodeId = generateUuid();
    String testWaitNodeId = generateUuid();
    Plan oneNodePlan =
        Plan.builder()
            .node(PlanNode.builder()
                      .name("Test Node")
                      .uuid(testStartNodeId)
                      .identifier("test1")
                      .stepType(TEST_STEP_TYPE)
                      .adviserObtainment(
                          AdviserObtainment.newBuilder()
                              .setType(OnSuccessAdviser.ADVISER_TYPE)
                              .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                                  OnSuccessAdviserParameters.builder().nextNodeId(testWaitNodeId).build())))
                              .build())
                      .facilitatorObtainment(
                          FacilitatorObtainment.newBuilder()
                              .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                              .build())
                      .build())
            .node(
                PlanNode.builder()
                    .uuid(testWaitNodeId)
                    .name("Finish Node")
                    .identifier("finish")
                    .stepType(ASYNC_STEP_TYPE)
                    .stepParameters(TestStepParameters.builder().param("Param").build())
                    .facilitatorObtainment(
                        FacilitatorObtainment.newBuilder()
                            .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.ASYNC).build())
                            .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                                DefaultFacilitatorParams.builder().waitDurationSeconds(Duration.ofSeconds(2)).build())))
                            .build())
                    .build())
            .startingNodeId(testStartNodeId)
            .build();

    try (MaintenanceGuard guard = new MaintenanceGuard(false)) {
      PlanExecution response = orchestrationService.startExecution(oneNodePlan, prepareInputArgs(), triggerInfo);

      engineTestHelper.waitForPlanCompletion(response.getUuid());
      response = engineTestHelper.getPlanExecutionStatus(response.getUuid());

      assertThat(response).isNotNull();
      assertThat(response.getStatus()).isEqualTo(SUCCEEDED);
    }
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldThrowInvalidRequestException() {
    final String exceptionStartMessage = "No node found with Id";
    Plan oneNodePlan = Plan.builder().startingNodeId(generateUuid()).build();

    assertThatThrownBy(() -> orchestrationService.startExecution(oneNodePlan, prepareInputArgs(), triggerInfo))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageStartingWith(exceptionStartMessage);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldRerunExecution() {
    String testNodeId = generateUuid();
    Plan oneNodePlan =
        Plan.builder()
            .node(PlanNode.builder()
                      .name("Test Rerun Node")
                      .uuid(testNodeId)
                      .identifier("test1")
                      .stepType(TEST_STEP_TYPE)
                      .facilitatorObtainment(
                          FacilitatorObtainment.newBuilder()
                              .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                              .build())
                      .build())
            .startingNodeId(testNodeId)
            .build();

    PlanExecution planExecution = orchestrationService.startExecution(oneNodePlan, prepareInputArgs(), triggerInfo);
    engineTestHelper.waitForPlanCompletion(planExecution.getUuid());
    planExecution = engineTestHelper.getPlanExecutionStatus(planExecution.getUuid());

    assertThat(planExecution).isNotNull();
    assertThat(planExecution.getStatus()).isEqualTo(SUCCEEDED);

    PlanExecution newPlanExecution = orchestrationService.rerunExecution(planExecution.getUuid(), prepareInputArgs());
    engineTestHelper.waitForPlanCompletion(newPlanExecution.getUuid());
    newPlanExecution = engineTestHelper.getPlanExecutionStatus(newPlanExecution.getUuid());

    assertThat(newPlanExecution).isNotNull();
    assertThat(newPlanExecution.getStatus()).isEqualTo(SUCCEEDED);
  }

  private static Map<String, String> prepareInputArgs() {
    return ImmutableMap.of("accountId", "kmpySmUISimoRrJL6NL73w", "appId", "XEsfW6D_RJm1IaGpDidD3g", "userId",
        embeddedUser.getUuid(), "userName", embeddedUser.getName(), "userEmail", embeddedUser.getEmail());
  }

  private static class TestHttpResponseCodeSwitchAdviser implements Adviser {
    @Override
    public AdviserResponse onAdviseEvent(AdvisingEvent advisingEvent) {
      return null;
    }

    @Override
    public boolean canAdvise(AdvisingEvent advisingEvent) {
      return false;
    }
  }

  private static class TestSyncStep implements SyncExecutable<EmptyStepParameters> {
    @Override
    public Class<EmptyStepParameters> getStepParametersClass() {
      return EmptyStepParameters.class;
    }

    @Override
    public StepResponse executeSync(Ambiance ambiance, EmptyStepParameters stepParameters,
        StepInputPackage inputPackage, PassThroughData passThroughData) {
      return StepResponse.builder().status(SUCCEEDED).build();
    }
  }
}
