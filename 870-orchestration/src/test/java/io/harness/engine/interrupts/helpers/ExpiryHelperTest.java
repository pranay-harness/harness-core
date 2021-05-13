package io.harness.engine.interrupts.helpers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.logging.UnitStatus.EXPIRED;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.OrchestrationTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.OrchestrationEngine;
import io.harness.execution.NodeExecution;
import io.harness.interrupts.Interrupt;
import io.harness.logging.UnitProgress;
import io.harness.logging.UnitStatus;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.TaskExecutableResponse;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.pms.contracts.steps.io.StepResponseProto;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PIPELINE)
public class ExpiryHelperTest extends OrchestrationTestBase {
  @Mock InterruptHelper interruptHelper;
  @Mock OrchestrationEngine engine;
  @Inject @InjectMocks ExpiryHelper expiryHelper;

  private NodeExecution nodeExecution;
  private Interrupt interrupt;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @After
  public void verifyMocks() {
    Mockito.verifyNoMoreInteractions(engine);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldExpireNodeExecutionInstance() {
    NodeExecution nodeExecution =
        NodeExecution.builder()
            .uuid(generateUuid())
            .ambiance(Ambiance.newBuilder().setPlanExecutionId(generateUuid()).build())
            .status(Status.RUNNING)
            .mode(ExecutionMode.TASK)
            .executableResponse(ExecutableResponse.newBuilder()
                                    .setTask(TaskExecutableResponse.newBuilder()
                                                 .setTaskId(generateUuid())
                                                 .setTaskCategory(TaskCategory.UNKNOWN_CATEGORY)
                                                 .build())
                                    .build())
            .startTs(123L)
            .build();

    Interrupt interrupt = Interrupt.builder()
                              .uuid(generateUuid())
                              .planExecutionId(nodeExecution.getAmbiance().getPlanExecutionId())
                              .type(InterruptType.MARK_EXPIRED)
                              .build();
    when(interruptHelper.discontinueTaskIfRequired(nodeExecution)).thenReturn(true);
    String interruptId = expiryHelper.expireMarkedInstance(nodeExecution, interrupt);

    verify(engine).handleStepResponse(nodeExecution.getUuid(),
        StepResponseProto.newBuilder()
            .setStatus(Status.EXPIRED)
            .setFailureInfo(FailureInfo.newBuilder()
                                .setErrorMessage("Step timed out before completion")
                                .addFailureTypes(FailureType.TIMEOUT_FAILURE)
                                .build())
            .build());
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestEvaluateUnitProgress() {
    NodeExecution nodeExecution =
        NodeExecution.builder()
            .uuid(generateUuid())
            .ambiance(Ambiance.newBuilder().setPlanExecutionId(generateUuid()).build())
            .status(Status.RUNNING)
            .mode(ExecutionMode.TASK)
            .executableResponse(ExecutableResponse.newBuilder()
                                    .setTask(TaskExecutableResponse.newBuilder()
                                                 .setTaskId(generateUuid())
                                                 .setTaskCategory(TaskCategory.UNKNOWN_CATEGORY)
                                                 .build())
                                    .build())
            .unitProgress(UnitProgress.newBuilder()
                              .setUnitName("Fetch Files")
                              .setStatus(UnitStatus.SUCCESS)
                              .setEndTime(System.currentTimeMillis())
                              .build())
            .unitProgress(UnitProgress.newBuilder().setUnitName("Apply").setStatus(UnitStatus.RUNNING).build())
            .startTs(123L)
            .build();

    List<UnitProgress> unitProgressList = InterruptHelper.evaluateUnitProgresses(nodeExecution, EXPIRED);

    assertThat(unitProgressList).hasSize(2);
    assertThat(unitProgressList.stream().map(UnitProgress::getStatus))
        .containsExactly(UnitStatus.SUCCESS, UnitStatus.EXPIRED);
    assertThat(unitProgressList.stream().map(UnitProgress::getEndTime)).doesNotContain((Long) null);
  }
}
