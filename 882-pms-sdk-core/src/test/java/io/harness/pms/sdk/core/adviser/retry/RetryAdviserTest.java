package io.harness.pms.sdk.core.adviser.retry;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.advisers.AdviseType;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.advisers.RetryAdvise;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.commons.RepairActionCode;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.AmbianceTestUtils;
import io.harness.pms.sdk.core.PmsSdkCoreTestBase;
import io.harness.pms.sdk.core.adviser.AdvisingEvent;
import io.harness.pms.sdk.core.adviser.AdvisingEvent.AdvisingEventBuilder;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.harness.serializer.ProtoUtils;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.EnumSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class RetryAdviserTest extends PmsSdkCoreTestBase {
  public static final String DUMMY_NODE_ID = generateUuid();
  public static final String NODE_EXECUTION_ID = generateUuid();
  public static final String NODE_SETUP_ID = generateUuid();
  public static final String NODE_NAME = generateUuid();
  public static final String NODE_IDENTIFIER = "DUMMY";
  public static final StepType DUMMY_STEP_TYPE = StepType.newBuilder().setType("DUMMY").build();

  @InjectMocks @Inject RetryAdviser retryAdviser;

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
    NodeExecutionProto nodeExecutionProto =
        NodeExecutionProto.newBuilder()
            .setUuid(NODE_EXECUTION_ID)
            .setAmbiance(ambiance)
            .setNode(PlanNodeProto.newBuilder()
                         .setUuid(NODE_SETUP_ID)
                         .setName(NODE_NAME)
                         .setIdentifier("dummy")
                         .setStepType(StepType.newBuilder().setType("DUMMY").build())
                         .build())
            .setStartTs(ProtoUtils.unixMillisToTimestamp(System.currentTimeMillis()))
            .setStatus(Status.FAILED)
            .build();
    AdvisingEvent advisingEvent = AdvisingEvent.builder()
                                      .nodeExecution(nodeExecutionProto)
                                      .toStatus(Status.FAILED)
                                      .adviserParameters(kryoSerializer.asBytes(getRetryParamsWithIgnore()))
                                      .build();
    AdviserResponse adviserResponse = retryAdviser.onAdviseEvent(advisingEvent);

    assertThat(adviserResponse.getType()).isEqualTo(AdviseType.RETRY);
    assertThat(adviserResponse.getRetryAdvise()).isNotNull();
    RetryAdvise retryAdvise = adviserResponse.getRetryAdvise();
    assertThat(retryAdvise.getRetryNodeExecutionId()).isEqualTo(NODE_EXECUTION_ID);
    assertThat(retryAdvise.getWaitInterval()).isEqualTo(2);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestLastWaitInterval() {
    NodeExecutionProto nodeExecutionProto =
        NodeExecutionProto.newBuilder()
            .setUuid(NODE_EXECUTION_ID)
            .setAmbiance(ambiance)
            .setNode(PlanNodeProto.newBuilder()
                         .setUuid(NODE_SETUP_ID)
                         .setName(NODE_NAME)
                         .setIdentifier("dummy")
                         .setStepType(StepType.newBuilder().setType("DUMMY").build())
                         .build())
            .setStartTs(ProtoUtils.unixMillisToTimestamp(System.currentTimeMillis()))
            .setStatus(Status.FAILED)
            .addAllRetryIds(Arrays.asList(generateUuid(), generateUuid(), generateUuid(), generateUuid()))
            .build();
    AdvisingEvent advisingEvent = AdvisingEvent.<io.harness.pms.sdk.core.adviser.retry.RetryAdviserParameters>builder()
                                      .nodeExecution(nodeExecutionProto)
                                      .toStatus(Status.FAILED)
                                      .adviserParameters(kryoSerializer.asBytes(getRetryParamsWithIgnore()))
                                      .build();
    AdviserResponse adviserResponse = retryAdviser.onAdviseEvent(advisingEvent);

    assertThat(adviserResponse.getType()).isEqualTo(AdviseType.RETRY);
    assertThat(adviserResponse.getRetryAdvise()).isNotNull();
    RetryAdvise retryAdvise = adviserResponse.getRetryAdvise();
    assertThat(retryAdvise.getRetryNodeExecutionId()).isEqualTo(NODE_EXECUTION_ID);
    assertThat(retryAdvise.getWaitInterval()).isEqualTo(5);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestAfterRetryStatus() {
    NodeExecutionProto nodeExecutionProto =
        NodeExecutionProto.newBuilder()
            .setUuid(NODE_EXECUTION_ID)
            .setAmbiance(ambiance)
            .setNode(PlanNodeProto.newBuilder()
                         .setUuid(NODE_SETUP_ID)
                         .setName(NODE_NAME)
                         .setIdentifier("dummy")
                         .setStepType(StepType.newBuilder().setType("DUMMY").build())
                         .build())
            .setStartTs(ProtoUtils.unixMillisToTimestamp(System.currentTimeMillis()))
            .setStatus(Status.FAILED)
            .addAllRetryIds(
                Arrays.asList(generateUuid(), generateUuid(), generateUuid(), generateUuid(), generateUuid()))
            .build();
    AdvisingEvent advisingEvent = AdvisingEvent.builder()
                                      .nodeExecution(nodeExecutionProto)
                                      .toStatus(Status.FAILED)
                                      .adviserParameters(kryoSerializer.asBytes(getRetryParamsWithIgnore()))
                                      .build();
    AdviserResponse adviserResponse = retryAdviser.onAdviseEvent(advisingEvent);

    assertThat(adviserResponse.getType()).isEqualTo(AdviseType.IGNORE_FAILURE);
    assertThat(adviserResponse.getIgnoreFailureAdvise()).isNotNull();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestCanAdvise() {
    AdvisingEventBuilder advisingEventBuilder =
        AdvisingEvent.builder()
            .toStatus(Status.FAILED)
            .adviserParameters(kryoSerializer.asBytes(getRetryParamsWithIgnore()));

    NodeExecutionProto nodeExecutionAuthFail =
        NodeExecutionProto.newBuilder()
            .setAmbiance(ambiance)
            .setFailureInfo(FailureInfo.newBuilder()
                                .setErrorMessage("Auth Error")
                                .addAllFailureTypes(EnumSet.of(FailureType.AUTHENTICATION_FAILURE))
                                .build())
            .build();
    AdvisingEvent authFailEvent = advisingEventBuilder.nodeExecution(nodeExecutionAuthFail).build();
    boolean canAdvise = retryAdviser.canAdvise(authFailEvent);
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
    canAdvise = retryAdviser.canAdvise(appFailEvent);
    assertThat(canAdvise).isFalse();
  }

  private static io.harness.pms.sdk.core.adviser.retry.RetryAdviserParameters getRetryParamsWithIgnore() {
    return RetryAdviserParameters.builder()
        .retryCount(5)
        .waitIntervalList(ImmutableList.of(2, 5))
        .repairActionCodeAfterRetry(RepairActionCode.IGNORE)
        .nextNodeId(DUMMY_NODE_ID)
        .applicableFailureTypes(EnumSet.of(FailureType.AUTHENTICATION_FAILURE))
        .build();
  }
}
