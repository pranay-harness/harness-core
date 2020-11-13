package io.harness.advisers.manualintervention;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.OrchestrationTestBase;
import io.harness.adviser.Advise;
import io.harness.adviser.AdvisingEvent;
import io.harness.adviser.AdvisingEvent.AdvisingEventBuilder;
import io.harness.adviser.advise.InterventionWaitAdvise;
import io.harness.ambiance.Ambiance;
import io.harness.category.element.UnitTests;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.exception.FailureType;
import io.harness.execution.NodeExecution;
import io.harness.pms.execution.Status;
import io.harness.plan.PlanNode;
import io.harness.pms.ambiance.Level;
import io.harness.pms.steps.StepType;
import io.harness.rule.Owner;
import io.harness.state.io.FailureInfo;
import io.harness.utils.AmbianceTestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.EnumSet;

public class ManualInterventionAdviserTest extends OrchestrationTestBase {
  public static final String NODE_EXECUTION_ID = generateUuid();
  public static final String NODE_SETUP_ID = generateUuid();
  public static final String NODE_NAME = generateUuid();
  public static final String NODE_IDENTIFIER = "DUMMY";
  public static final StepType DUMMY_STEP_TYPE = StepType.newBuilder().setType("DUMMY").build();

  @InjectMocks @Inject ManualInterventionAdviser manualInterventionAdviser;

  @Mock NodeExecutionService nodeExecutionService;

  private Ambiance ambiance;

  @Before
  public void setup() {
    ambiance = AmbianceTestUtils.buildAmbiance();
    ambiance.addLevel(Level.newBuilder()
                          .setSetupId(NODE_SETUP_ID)
                          .setRuntimeId(NODE_EXECUTION_ID)
                          .setIdentifier(NODE_IDENTIFIER)
                          .setStepType(DUMMY_STEP_TYPE)
                          .build());
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestOnAdviseEvent() {
    NodeExecution nodeExecution = NodeExecution.builder()
                                      .uuid(NODE_EXECUTION_ID)
                                      .ambiance(ambiance)
                                      .node(PlanNode.builder()
                                                .uuid(NODE_SETUP_ID)
                                                .name(NODE_NAME)
                                                .identifier("dummy")
                                                .stepType(io.harness.state.StepType.builder().type("DUMMY").build())
                                                .build())
                                      .startTs(System.currentTimeMillis())
                                      .status(Status.FAILED)
                                      .build();
    when(nodeExecutionService.get(ambiance.obtainCurrentRuntimeId())).thenReturn(nodeExecution);
    AdvisingEvent<ManualInterventionAdviserParameters> advisingEvent =
        AdvisingEvent.<ManualInterventionAdviserParameters>builder()
            .ambiance(ambiance)
            .toStatus(Status.FAILED)
            .adviserParameters(ManualInterventionAdviserParameters.builder().build())
            .build();
    Advise advise = manualInterventionAdviser.onAdviseEvent(advisingEvent);
    assertThat(advise).isInstanceOf(InterventionWaitAdvise.class);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestCanAdvise() {
    AdvisingEventBuilder<ManualInterventionAdviserParameters> advisingEventBuilder =
        AdvisingEvent.<ManualInterventionAdviserParameters>builder()
            .ambiance(ambiance)
            .toStatus(Status.FAILED)
            .fromStatus(Status.RUNNING)
            .adviserParameters(ManualInterventionAdviserParameters.builder()
                                   .applicableFailureTypes(EnumSet.of(FailureType.AUTHENTICATION))
                                   .build());

    AdvisingEvent<ManualInterventionAdviserParameters> authFailEvent =
        advisingEventBuilder
            .failureInfo(FailureInfo.builder()
                             .errorMessage("Auth Error")
                             .failureTypes(EnumSet.of(FailureType.AUTHENTICATION))
                             .build())
            .build();

    boolean canAdvise = manualInterventionAdviser.canAdvise(authFailEvent);
    assertThat(canAdvise).isTrue();

    AdvisingEvent<ManualInterventionAdviserParameters> appFailEvent =
        advisingEventBuilder
            .failureInfo(FailureInfo.builder()
                             .errorMessage("Application Error")
                             .failureTypes(EnumSet.of(FailureType.APPLICATION_ERROR))
                             .build())
            .build();
    canAdvise = manualInterventionAdviser.canAdvise(appFailEvent);
    assertThat(canAdvise).isFalse();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestCanAdviseWithFromStatus() {
    AdvisingEventBuilder<ManualInterventionAdviserParameters> advisingEventBuilder =
        AdvisingEvent.<ManualInterventionAdviserParameters>builder()
            .ambiance(ambiance)
            .toStatus(Status.FAILED)
            .fromStatus(Status.INTERVENTION_WAITING)
            .adviserParameters(ManualInterventionAdviserParameters.builder()
                                   .applicableFailureTypes(EnumSet.of(FailureType.AUTHENTICATION))
                                   .build());

    boolean canAdvise = manualInterventionAdviser.canAdvise(advisingEventBuilder.build());
    assertThat(canAdvise).isFalse();
  }
}