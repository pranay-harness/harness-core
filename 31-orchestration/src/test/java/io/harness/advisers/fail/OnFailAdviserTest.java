package io.harness.advisers.fail;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.OrchestrationTestBase;
import io.harness.adviser.Advise;
import io.harness.adviser.AdvisingEvent;
import io.harness.adviser.AdvisingEvent.AdvisingEventBuilder;
import io.harness.adviser.advise.NextStepAdvise;
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
import io.harness.serializer.KryoSerializer;
import io.harness.state.io.FailureInfo;
import io.harness.utils.AmbianceTestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.EnumSet;

public class OnFailAdviserTest extends OrchestrationTestBase {
  public static final String NODE_EXECUTION_ID = generateUuid();
  public static final String NODE_SETUP_ID = generateUuid();
  public static final String NODE_NAME = generateUuid();
  public static final String NODE_IDENTIFIER = "DUMMY";
  public static final StepType DUMMY_STEP_TYPE = StepType.newBuilder().setType("DUMMY").build();

  @InjectMocks @Inject OnFailAdviser onFailAdviser;

  @Mock NodeExecutionService nodeExecutionService;
  @Inject KryoSerializer kryoSerializer;

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
  public void shouldTestValidStatus() {
    String nextNodeId = generateUuid();
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
    AdvisingEvent advisingEvent =
        AdvisingEvent.builder()
            .ambiance(ambiance)
            .toStatus(Status.FAILED)
            .adviserParameters(kryoSerializer.asBytes(OnFailAdviserParameters.builder().nextNodeId(nextNodeId).build()))
            .build();
    Advise advise = onFailAdviser.onAdviseEvent(advisingEvent);
    assertThat(advise).isInstanceOf(NextStepAdvise.class);
    NextStepAdvise nextStepAdvise = (NextStepAdvise) advise;
    assertThat(nextStepAdvise.getNextNodeId()).isEqualTo(nextNodeId);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestCanAdviseNextNull() {
    AdvisingEvent advisingEvent = AdvisingEvent.<OnFailAdviserParameters>builder()
                                      .ambiance(ambiance)
                                      .toStatus(Status.FAILED)
                                      .adviserParameters(kryoSerializer.asBytes(
                                          OnFailAdviserParameters.builder()
                                              .applicableFailureTypes(EnumSet.of(FailureType.AUTHENTICATION))
                                              .build()))
                                      .failureInfo(FailureInfo.builder()
                                                       .errorMessage("Auth Error")
                                                       .failureTypes(EnumSet.of(FailureType.AUTHENTICATION))
                                                       .build())
                                      .build();

    boolean canAdvise = onFailAdviser.canAdvise(advisingEvent);
    assertThat(canAdvise).isFalse();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestCanAdvise() {
    AdvisingEventBuilder advisingEventBuilder =
        AdvisingEvent.builder()
            .ambiance(ambiance)
            .toStatus(Status.FAILED)
            .adviserParameters(
                kryoSerializer.asBytes(OnFailAdviserParameters.builder()
                                           .nextNodeId(generateUuid())
                                           .applicableFailureTypes(EnumSet.of(FailureType.AUTHENTICATION))
                                           .build()));

    AdvisingEvent authFailEvent = advisingEventBuilder
                                      .failureInfo(FailureInfo.builder()
                                                       .errorMessage("Auth Error")
                                                       .failureTypes(EnumSet.of(FailureType.AUTHENTICATION))
                                                       .build())
                                      .build();

    boolean canAdvise = onFailAdviser.canAdvise(authFailEvent);
    assertThat(canAdvise).isTrue();

    AdvisingEvent appFailEvent = advisingEventBuilder
                                     .failureInfo(FailureInfo.builder()
                                                      .errorMessage("Application Error")
                                                      .failureTypes(EnumSet.of(FailureType.APPLICATION_ERROR))
                                                      .build())
                                     .build();
    canAdvise = onFailAdviser.canAdvise(appFailEvent);
    assertThat(canAdvise).isFalse();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestCanAdviseWithNoFailureInfo() {
    AdvisingEventBuilder advisingEventBuilder =
        AdvisingEvent.<OnFailAdviserParameters>builder()
            .ambiance(ambiance)
            .toStatus(Status.FAILED)
            .adviserParameters(
                kryoSerializer.asBytes(OnFailAdviserParameters.builder()
                                           .nextNodeId(generateUuid())
                                           .applicableFailureTypes(EnumSet.of(FailureType.AUTHENTICATION))
                                           .build()));

    boolean canAdvise = onFailAdviser.canAdvise(advisingEventBuilder.build());
    assertThat(canAdvise).isTrue();
  }
}