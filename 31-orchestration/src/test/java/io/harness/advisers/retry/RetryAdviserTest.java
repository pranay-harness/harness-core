package io.harness.advisers.retry;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import io.harness.OrchestrationTest;
import io.harness.adviser.Advise;
import io.harness.adviser.AdvisingEvent;
import io.harness.adviser.advise.NextStepAdvise;
import io.harness.adviser.advise.RetryAdvise;
import io.harness.ambiance.Ambiance;
import io.harness.ambiance.Level;
import io.harness.category.element.UnitTests;
import io.harness.engine.AmbianceHelper;
import io.harness.execution.NodeExecution;
import io.harness.execution.status.Status;
import io.harness.interrupts.RepairActionCode;
import io.harness.plan.PlanNode;
import io.harness.rule.Owner;
import io.harness.state.core.dummy.DummyStep;
import io.harness.utils.AmbianceTestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;

public class RetryAdviserTest extends OrchestrationTest {
  public static final String DUMMY_NODE_ID = generateUuid();
  public static final String NODE_EXECUTION_ID = generateUuid();
  public static final String NODE_SETUP_ID = generateUuid();
  public static final String NODE_NAME = generateUuid();
  public static final String NODE_IDENTIFIER = "DUMMY";

  @InjectMocks @Inject RetryAdviser retryAdviser;

  @Mock AmbianceHelper ambianceHelper;

  private Ambiance ambiance;

  @Before
  public void setup() {
    ambiance = AmbianceTestUtils.buildAmbiance();
    ambiance.addLevel(Level.builder()
                          .setupId(NODE_SETUP_ID)
                          .runtimeId(NODE_EXECUTION_ID)
                          .identifier(NODE_IDENTIFIER)
                          .stepType(DummyStep.STEP_TYPE)
                          .build());
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestInvalidStatus() {
    AdvisingEvent advisingEvent =
        AdvisingEvent.builder().ambiance(AmbianceTestUtils.buildAmbiance()).status(Status.SUCCEEDED).build();
    Advise advise = retryAdviser.onAdviseEvent(advisingEvent);
    assertThat(advise).isNull();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestValidStatus() {
    NodeExecution nodeExecution = NodeExecution.builder()
                                      .uuid(NODE_EXECUTION_ID)
                                      .planExecutionId(ambiance.getPlanExecutionId())
                                      .levels(ambiance.getLevels())
                                      .node(PlanNode.builder()
                                                .uuid(NODE_SETUP_ID)
                                                .name(NODE_NAME)
                                                .identifier("dummy")
                                                .stepType(DummyStep.STEP_TYPE)
                                                .build())
                                      .startTs(System.currentTimeMillis())
                                      .status(Status.FAILED)
                                      .build();
    when(ambianceHelper.obtainNodeExecution(ambiance)).thenReturn(nodeExecution);
    AdvisingEvent advisingEvent = AdvisingEvent.builder()
                                      .ambiance(ambiance)
                                      .status(Status.FAILED)
                                      .adviserParameters(getRetryParamsWithIgnore())
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
            .planExecutionId(ambiance.getPlanExecutionId())
            .levels(ambiance.getLevels())
            .node(PlanNode.builder()
                      .uuid(NODE_SETUP_ID)
                      .name(NODE_NAME)
                      .identifier("dummy")
                      .stepType(DummyStep.STEP_TYPE)
                      .build())
            .startTs(System.currentTimeMillis())
            .status(Status.FAILED)
            .retryIds(Arrays.asList(generateUuid(), generateUuid(), generateUuid(), generateUuid()))
            .build();
    when(ambianceHelper.obtainNodeExecution(ambiance)).thenReturn(nodeExecution);
    AdvisingEvent advisingEvent = AdvisingEvent.builder()
                                      .ambiance(ambiance)
                                      .status(Status.FAILED)
                                      .adviserParameters(getRetryParamsWithIgnore())
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
            .planExecutionId(ambiance.getPlanExecutionId())
            .levels(ambiance.getLevels())
            .node(PlanNode.builder()
                      .uuid(NODE_SETUP_ID)
                      .name(NODE_NAME)
                      .identifier("dummy")
                      .stepType(DummyStep.STEP_TYPE)
                      .build())
            .startTs(System.currentTimeMillis())
            .status(Status.FAILED)
            .retryIds(Arrays.asList(generateUuid(), generateUuid(), generateUuid(), generateUuid(), generateUuid()))
            .build();
    when(ambianceHelper.obtainNodeExecution(ambiance)).thenReturn(nodeExecution);
    AdvisingEvent advisingEvent = AdvisingEvent.builder()
                                      .ambiance(ambiance)
                                      .status(Status.FAILED)
                                      .adviserParameters(getRetryParamsWithIgnore())
                                      .build();
    Advise advise = retryAdviser.onAdviseEvent(advisingEvent);
    assertThat(advise).isInstanceOf(NextStepAdvise.class);
    NextStepAdvise nextStepAdvise = (NextStepAdvise) advise;
    assertThat(nextStepAdvise.getNextNodeId()).isEqualTo(DUMMY_NODE_ID);
  }

  private static RetryAdviserParameters getRetryParamsWithIgnore() {
    return RetryAdviserParameters.builder()
        .retryCount(5)
        .waitIntervalList(ImmutableList.of(2, 5))
        .repairActionCodeAfterRetry(RepairActionCode.IGNORE)
        .nextNodeId(DUMMY_NODE_ID)
        .build();
  }
}