package io.harness.states;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static software.wings.beans.TaskType.CI_LE_STATUS;

import io.harness.beans.DelegateTaskRequest;
import io.harness.callback.DelegateCallbackToken;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.HDelegateTask;
import io.harness.delegate.task.SimpleHDelegateTask;
import io.harness.delegate.task.stepstatus.StepStatusTaskParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.executionplan.CIExecutionTest;
import io.harness.grpc.DelegateServiceGrpcClient;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.Duration;
import java.util.HashMap;
import java.util.function.Supplier;

public class CIDelegateTaskExecutorTest extends CIExecutionTest {
  private static final String TASK_ID = "123456";
  private static final String ACCOUNT_ID = "accountId";
  @Mock private DelegateServiceGrpcClient delegateServiceGrpcClient;
  @Mock private Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;
  @InjectMocks private CIDelegateTaskExecutor ciDelegateTaskExecutor;

  private final StepStatusTaskParameters parameters = StepStatusTaskParameters.builder().build();
  private final DelegateTaskRequest expectedDelegateTaskRequest = DelegateTaskRequest.builder()
                                                                      .parked(true)
                                                                      .accountId(ACCOUNT_ID)
                                                                      .taskType(CI_LE_STATUS.name())
                                                                      .taskParameters(parameters)
                                                                      .executionTimeout(Duration.ofHours(12))
                                                                      .taskSetupAbstractions(new HashMap<>())
                                                                      .build();

  private final DelegateTaskRequest expectedDelegateTaskRequestWithEmptyParams =
      DelegateTaskRequest.builder()
          .parked(true)
          .accountId(ACCOUNT_ID)
          .taskType(CI_LE_STATUS.name())
          .taskParameters(null)
          .executionTimeout(Duration.ofHours(12))
          .taskSetupAbstractions(new HashMap<>())
          .build();

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldQueueTaskAndReturnTaskId() {
    HDelegateTask task = SimpleHDelegateTask.builder()
                             .accountId(ACCOUNT_ID)
                             .data(TaskData.builder()
                                       .async(true)
                                       .parked(true)
                                       .taskType(CI_LE_STATUS.name())
                                       .parameters(new Object[] {parameters})
                                       .timeout(10 * 60L)
                                       .build())
                             .setupAbstractions(new HashMap<>())
                             .build();

    when(delegateCallbackTokenSupplier.get()).thenReturn(DelegateCallbackToken.newBuilder().build());
    when(delegateServiceGrpcClient.submitAsyncTask(eq(expectedDelegateTaskRequest), any())).thenReturn(TASK_ID);

    String taskId = ciDelegateTaskExecutor.queueTask(new HashMap<>(), task);
    assertThat(taskId).isEqualTo(TASK_ID);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldQueueTaskWithoutTaskParams() {
    HDelegateTask task = SimpleHDelegateTask.builder()
                             .accountId(ACCOUNT_ID)
                             .data(TaskData.builder()
                                       .async(true)
                                       .parked(true)
                                       .taskType(CI_LE_STATUS.name())
                                       .parameters(null)
                                       .timeout(10 * 60L)
                                       .build())
                             .setupAbstractions(new HashMap<>())
                             .build();

    when(delegateCallbackTokenSupplier.get()).thenReturn(DelegateCallbackToken.newBuilder().build());
    when(delegateServiceGrpcClient.submitAsyncTask(eq(expectedDelegateTaskRequestWithEmptyParams), any()))
        .thenReturn(TASK_ID);

    String taskId = ciDelegateTaskExecutor.queueTask(new HashMap<>(), task);
    assertThat(taskId).isEqualTo(TASK_ID);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldThrowOnNonTaskParamsRequestForQueueTask() {
    HDelegateTask task = SimpleHDelegateTask.builder()
                             .accountId(ACCOUNT_ID)
                             .data(TaskData.builder()
                                       .async(true)
                                       .parked(true)
                                       .taskType(CI_LE_STATUS.name())
                                       .parameters(new Object[] {"Wrong type"})
                                       .timeout(10 * 60L)
                                       .build())
                             .setupAbstractions(new HashMap<>())
                             .build();

    when(delegateCallbackTokenSupplier.get()).thenReturn(DelegateCallbackToken.newBuilder().build());
    when(delegateServiceGrpcClient.submitAsyncTask(eq(expectedDelegateTaskRequestWithEmptyParams), any()))
        .thenReturn(TASK_ID);

    assertThatThrownBy(() -> ciDelegateTaskExecutor.queueTask(new HashMap<>(), task))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Task Execution not supported for type");
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldReturnFalseOnAbortTask() {
    assertThat(ciDelegateTaskExecutor.abortTask(new HashMap<>(), TASK_ID)).isFalse();
  }
}