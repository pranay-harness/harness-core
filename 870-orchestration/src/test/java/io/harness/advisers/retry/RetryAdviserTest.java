package io.harness.advisers.retry;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.AmbianceUtils;
import io.harness.OrchestrationTestBase;
import io.harness.adviser.Advise;
import io.harness.adviser.AdvisingEvent;
import io.harness.adviser.AdvisingEvent.AdvisingEventBuilder;
import io.harness.adviser.advise.NextStepAdvise;
import io.harness.adviser.advise.RetryAdvise;
import io.harness.category.element.UnitTests;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.interrupts.RepairActionCode;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.ambiance.Level;
import io.harness.pms.execution.Status;
import io.harness.pms.execution.failure.FailureInfo;
import io.harness.pms.execution.failure.FailureType;
import io.harness.pms.plan.PlanNodeProto;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.steps.StepType;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.harness.utils.AmbianceTestUtils;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.EnumSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class RetryAdviserTest extends OrchestrationTestBase {
  public static final String DUMMY_NODE_ID = generateUuid();
  public static final String NODE_EXECUTION_ID = generateUuid();
  public static final String NODE_SETUP_ID = generateUuid();
  public static final String NODE_NAME = generateUuid();
  public static final String NODE_IDENTIFIER = "DUMMY";
  public static final StepType DUMMY_STEP_TYPE = StepType.newBuilder().setType("DUMMY").build();

  @InjectMocks @Inject RetryAdviser retryAdviser;

  @Mock NodeExecutionService nodeExecutionService;
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
    NodeExecution nodeExecution = NodeExecution.builder()
                                      .uuid(NODE_EXECUTION_ID)
                                      .ambiance(ambiance)
                                      .node(PlanNodeProto.newBuilder()
                                                .setUuid(NODE_SETUP_ID)
                                                .setName(NODE_NAME)
                                                .setIdentifier("dummy")
                                                .setStepType(StepType.newBuilder().setType("DUMMY").build())
                                                .build())
                                      .startTs(System.currentTimeMillis())
                                      .status(Status.FAILED)
                                      .build();
    when(nodeExecutionService.get(AmbianceUtils.obtainCurrentRuntimeId(ambiance))).thenReturn(nodeExecution);
    AdvisingEvent advisingEvent = AdvisingEvent.builder()
                                      .ambiance(ambiance)
                                      .toStatus(Status.FAILED)
                                      .adviserParameters(kryoSerializer.asBytes(getRetryParamsWithIgnore()))
                                      .build();
    Advise advise = retryAdviser.onAdviseEvent(advisingEvent);
    assertThat(advise).isInstanceOf(RetryAdvise.class);
    RetryAdvise retryAdvise = (RetryAdvise) advise;
    assertThat(retryAdvise.getWaitInterval()).isEqualTo(2);
    assertThat(retryAdvise.getRetryNodeExecutionId()).isEqualTo(NODE_EXECUTION_ID);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestLastWaitInterval() {
    NodeExecution nodeExecution =
        NodeExecution.builder()
            .uuid(NODE_EXECUTION_ID)
            .ambiance(ambiance)
            .node(PlanNodeProto.newBuilder()
                      .setUuid(NODE_SETUP_ID)
                      .setName(NODE_NAME)
                      .setIdentifier("dummy")
                      .setStepType(StepType.newBuilder().setType("DUMMY").build())
                      .build())
            .startTs(System.currentTimeMillis())
            .status(Status.FAILED)
            .retryIds(Arrays.asList(generateUuid(), generateUuid(), generateUuid(), generateUuid()))
            .build();
    when(nodeExecutionService.get(AmbianceUtils.obtainCurrentRuntimeId(ambiance))).thenReturn(nodeExecution);
    AdvisingEvent advisingEvent = AdvisingEvent.<RetryAdviserParameters>builder()
                                      .ambiance(ambiance)
                                      .toStatus(Status.FAILED)
                                      .adviserParameters(kryoSerializer.asBytes(getRetryParamsWithIgnore()))
                                      .build();
    Advise advise = retryAdviser.onAdviseEvent(advisingEvent);
    assertThat(advise).isInstanceOf(RetryAdvise.class);
    RetryAdvise retryAdvise = (RetryAdvise) advise;
    assertThat(retryAdvise.getWaitInterval()).isEqualTo(5);
    assertThat(retryAdvise.getRetryNodeExecutionId()).isEqualTo(NODE_EXECUTION_ID);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestAfterRetryStatus() {
    NodeExecution nodeExecution =
        NodeExecution.builder()
            .uuid(NODE_EXECUTION_ID)
            .ambiance(ambiance)
            .node(PlanNodeProto.newBuilder()
                      .setUuid(NODE_SETUP_ID)
                      .setName(NODE_NAME)
                      .setIdentifier("dummy")
                      .setStepType(StepType.newBuilder().setType("DUMMY").build())
                      .build())
            .startTs(System.currentTimeMillis())
            .status(Status.FAILED)
            .retryIds(Arrays.asList(generateUuid(), generateUuid(), generateUuid(), generateUuid(), generateUuid()))
            .build();
    when(nodeExecutionService.get(AmbianceUtils.obtainCurrentRuntimeId(ambiance))).thenReturn(nodeExecution);
    AdvisingEvent advisingEvent = AdvisingEvent.builder()
                                      .ambiance(ambiance)
                                      .toStatus(Status.FAILED)
                                      .adviserParameters(kryoSerializer.asBytes(getRetryParamsWithIgnore()))
                                      .build();
    Advise advise = retryAdviser.onAdviseEvent(advisingEvent);
    assertThat(advise).isInstanceOf(NextStepAdvise.class);
    NextStepAdvise nextStepAdvise = (NextStepAdvise) advise;
    assertThat(nextStepAdvise.getNextNodeId()).isEqualTo(DUMMY_NODE_ID);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestCanAdvise() {
    AdvisingEventBuilder advisingEventBuilder =
        AdvisingEvent.builder()
            .ambiance(ambiance)
            .toStatus(Status.FAILED)
            .adviserParameters(kryoSerializer.asBytes(getRetryParamsWithIgnore()));

    AdvisingEvent authFailEvent =
        advisingEventBuilder
            .failureInfo(FailureInfo.newBuilder()
                             .setErrorMessage("Auth Error")
                             .addAllFailureTypes(EnumSet.of(FailureType.AUTHENTICATION_FAILURE))
                             .build())
            .build();
    boolean canAdvise = retryAdviser.canAdvise(authFailEvent);
    assertThat(canAdvise).isTrue();

    AdvisingEvent appFailEvent = advisingEventBuilder
                                     .failureInfo(FailureInfo.newBuilder()
                                                      .setErrorMessage("Application Error")
                                                      .addAllFailureTypes(EnumSet.of(FailureType.APPLICATION_FAILURE))
                                                      .build())
                                     .build();
    canAdvise = retryAdviser.canAdvise(appFailEvent);
    assertThat(canAdvise).isFalse();
  }

  private static RetryAdviserParameters getRetryParamsWithIgnore() {
    return RetryAdviserParameters.builder()
        .retryCount(5)
        .waitIntervalList(ImmutableList.of(2, 5))
        .repairActionCodeAfterRetry(RepairActionCode.IGNORE)
        .nextNodeId(DUMMY_NODE_ID)
        .applicableFailureTypes(EnumSet.of(FailureType.AUTHENTICATION_FAILURE))
        .build();
  }
}
