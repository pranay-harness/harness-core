package io.harness.pms.sdk.core.adviser.fail;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.advisers.AdviseType;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.advisers.NextStepAdvise;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.AmbianceTestUtils;
import io.harness.pms.sdk.core.PmsSdkCoreTestBase;
import io.harness.pms.sdk.core.adviser.AdvisingEvent;
import io.harness.pms.sdk.core.adviser.AdvisingEvent.AdvisingEventBuilder;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import java.util.EnumSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class OnFailAdviserTest extends PmsSdkCoreTestBase {
  public static final String NODE_EXECUTION_ID = generateUuid();
  public static final String NODE_SETUP_ID = generateUuid();
  public static final String NODE_IDENTIFIER = "DUMMY";
  public static final StepType DUMMY_STEP_TYPE = StepType.newBuilder().setType("DUMMY").build();

  @InjectMocks @Inject OnFailAdviser onFailAdviser;

  @Inject KryoSerializer kryoSerializer;

  private Ambiance ambiance;

  @Before
  public void setup() {
    ambiance = AmbianceTestUtils.buildAmbiance();
    ambiance = ambiance.toBuilder()
                   .addLevels(Level.newBuilder()
                                  .setSetupId(NODE_SETUP_ID)
                                  .setRuntimeId(NODE_EXECUTION_ID)
                                  .setIdentifier(NODE_IDENTIFIER)
                                  .setStepType(DUMMY_STEP_TYPE)
                                  .build())
                   .build();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestValidStatus() {
    NodeExecutionProto nodeExecutionProto = NodeExecutionProto.newBuilder().setAmbiance(ambiance).build();
    String nextNodeId = generateUuid();
    AdvisingEvent advisingEvent =
        AdvisingEvent.builder()
            .nodeExecution(nodeExecutionProto)
            .toStatus(Status.FAILED)
            .adviserParameters(kryoSerializer.asBytes(
                io.harness.pms.sdk.core.adviser.fail.OnFailAdviserParameters.builder().nextNodeId(nextNodeId).build()))
            .build();
    AdviserResponse adviserResponse = onFailAdviser.onAdviseEvent(advisingEvent);
    assertThat(adviserResponse.getType()).isEqualTo(AdviseType.NEXT_STEP);
    assertThat(adviserResponse.getNextStepAdvise()).isNotNull();
    NextStepAdvise nextStepAdvise = adviserResponse.getNextStepAdvise();
    assertThat(nextStepAdvise.getNextNodeId()).isEqualTo(nextNodeId);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestCanAdviseNextNull() {
    NodeExecutionProto nodeExecutionProto =
        NodeExecutionProto.newBuilder()
            .setAmbiance(ambiance)
            .setFailureInfo(FailureInfo.newBuilder()
                                .setErrorMessage("Auth Error")
                                .addAllFailureTypes(EnumSet.of(FailureType.AUTHENTICATION_FAILURE))
                                .build())
            .build();
    AdvisingEvent advisingEvent = AdvisingEvent.<io.harness.pms.sdk.core.adviser.fail.OnFailAdviserParameters>builder()
                                      .nodeExecution(nodeExecutionProto)
                                      .toStatus(Status.FAILED)
                                      .adviserParameters(kryoSerializer.asBytes(
                                          io.harness.pms.sdk.core.adviser.fail.OnFailAdviserParameters.builder()
                                              .applicableFailureTypes(EnumSet.of(FailureType.AUTHENTICATION_FAILURE))
                                              .build()))
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
            .toStatus(Status.FAILED)
            .adviserParameters(
                kryoSerializer.asBytes(io.harness.pms.sdk.core.adviser.fail.OnFailAdviserParameters.builder()
                                           .nextNodeId(generateUuid())
                                           .applicableFailureTypes(EnumSet.of(FailureType.AUTHENTICATION_FAILURE))
                                           .build()));

    NodeExecutionProto nodeExecutionAuthFail =
        NodeExecutionProto.newBuilder()
            .setAmbiance(ambiance)
            .setFailureInfo(FailureInfo.newBuilder()
                                .setErrorMessage("Auth Error")
                                .addAllFailureTypes(EnumSet.of(FailureType.AUTHENTICATION_FAILURE))
                                .build())
            .build();
    AdvisingEvent authFailEvent = advisingEventBuilder.nodeExecution(nodeExecutionAuthFail).build();

    boolean canAdvise = onFailAdviser.canAdvise(authFailEvent);
    assertThat(canAdvise).isTrue();

    NodeExecutionProto nodeExecutionAppFail =
        NodeExecutionProto.newBuilder()
            .setAmbiance(ambiance)
            .setFailureInfo(FailureInfo.newBuilder()
                                .setErrorMessage("Application Error")
                                .addAllFailureTypes(EnumSet.of(FailureType.APPLICATION_FAILURE))
                                .build())
            .build();
    AdvisingEvent appFailEvent = advisingEventBuilder.nodeExecution(nodeExecutionAppFail).build();
    canAdvise = onFailAdviser.canAdvise(appFailEvent);
    assertThat(canAdvise).isFalse();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestCanAdviseWithNoFailureInfo() {
    NodeExecutionProto nodeExecutionProto = NodeExecutionProto.newBuilder().setAmbiance(ambiance).build();
    AdvisingEventBuilder advisingEventBuilder =
        AdvisingEvent.<io.harness.pms.sdk.core.adviser.fail.OnFailAdviserParameters>builder()
            .nodeExecution(nodeExecutionProto)
            .toStatus(Status.FAILED)
            .adviserParameters(
                kryoSerializer.asBytes(OnFailAdviserParameters.builder()
                                           .nextNodeId(generateUuid())
                                           .applicableFailureTypes(EnumSet.of(FailureType.AUTHENTICATION_FAILURE))
                                           .build()));

    boolean canAdvise = onFailAdviser.canAdvise(advisingEventBuilder.build());
    assertThat(canAdvise).isTrue();
  }
}
