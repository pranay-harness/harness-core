package io.harness.engine;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.pms.contracts.execution.Status.SUCCEEDED;
import static io.harness.pms.contracts.plan.TriggerType.MANUAL;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.plan.TriggeredBy;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.adviser.Adviser;
import io.harness.pms.sdk.core.adviser.AdvisingEvent;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.steps.executables.SyncExecutable;
import io.harness.pms.sdk.core.steps.io.EmptyStepParameters;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class OrchestrationEngineTest extends OrchestrationTestBase {
  @Mock @Named("EngineExecutorService") ExecutorService executorService;
  @Inject @InjectMocks private OrchestrationEngine orchestrationEngine;

  private static final StepType TEST_STEP_TYPE = StepType.newBuilder().setType("TEST_STEP_PLAN").build();

  private static final TriggeredBy triggeredBy =
      TriggeredBy.newBuilder().putExtraInfo("email", PRASHANT).setIdentifier(PRASHANT).setUuid(generateUuid()).build();
  private static final ExecutionTriggerInfo triggerInfo =
      ExecutionTriggerInfo.newBuilder().setTriggerType(MANUAL).setTriggeredBy(triggeredBy).build();

  private static final ExecutionMetadata metadata = ExecutionMetadata.newBuilder()
                                                        .setExecutionUuid(generateUuid())
                                                        .setRunSequence(0)
                                                        .setTriggerInfo(triggerInfo)
                                                        .build();

  @Before
  public void setUp() {}

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestTriggerExecution() {
    String planExecutionId = generateUuid();
    Ambiance ambiance =
        Ambiance.newBuilder().setPlanExecutionId(planExecutionId).putAllSetupAbstractions(prepareInputArgs()).build();
    PlanNodeProto planNode =
        PlanNodeProto.newBuilder()
            .setName("Test Node")
            .setUuid(generateUuid())
            .setIdentifier("test")
            .setStepType(TEST_STEP_TYPE)
            .addFacilitatorObtainments(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                    .build())
            .build();
    orchestrationEngine.triggerExecution(ambiance, planNode);
    verify(executorService).submit(any(ExecutionEngineDispatcher.class));
  }

  private static Map<String, String> prepareInputArgs() {
    return ImmutableMap.of("accountId", "kmpySmUISimoRrJL6NL73w", "appId", "XEsfW6D_RJm1IaGpDidD3g", "userId",
        triggeredBy.getUuid(), "userName", triggeredBy.getIdentifier(), "userEmail",
        triggeredBy.getExtraInfoOrThrow("email"));
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
