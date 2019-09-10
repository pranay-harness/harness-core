package software.wings.verification;

import static org.apache.cxf.ws.addressing.ContextUtils.generateUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.IntegrationTests;
import io.harness.waiter.WaitNotifyEngine;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import software.wings.integration.BaseIntegrationTest;
import software.wings.service.impl.analysis.DataCollectionInfoV2;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.impl.splunk.SplunkDataCollectionInfoV2;
import software.wings.service.intfc.verification.CVTaskService;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class CVTaskServiceTest extends BaseIntegrationTest {
  @Inject CVTaskService cvTaskService;
  private String stateExecutionId;
  private String accountId;
  @Mock WaitNotifyEngine waitNotifyEngine;

  @Before
  public void setupTests() throws IllegalAccessException {
    initMocks(this);
    accountId = generateUUID();
    stateExecutionId = generateUUID();
    cvTaskService = spy(cvTaskService);
    FieldUtils.writeField(cvTaskService, "waitNotifyEngine", waitNotifyEngine, true);
  }

  @Test
  @Category(IntegrationTests.class)
  public void testEnqueueTask() {
    String cvConfigId = generateUUID();
    long endMS = System.currentTimeMillis();
    long startMS = endMS - TimeUnit.MINUTES.toMillis(10);
    CVTask cvTask = cvTaskService.enqueueTask(accountId, cvConfigId, startMS, endMS);
    assertThat(cvTask.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  @Test
  @Category(IntegrationTests.class)
  public void testSaveCVTask() {
    CVTask cvTask = createCVTask();
    cvTaskService.saveCVTask(cvTask);
    assertEquals(ExecutionStatus.QUEUED, cvTask.getStatus());
    CVTask updatedCVTask = getCVTask(cvTask.getUuid());
    assertEquals(ExecutionStatus.QUEUED, updatedCVTask.getStatus());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testValidAfterWhenNotSetOnGettingNextTask() {
    CVTask cvTask = createCVTask();
    cvTaskService.saveCVTask(cvTask);
    Optional<CVTask> nextTask = cvTaskService.getNextTask(cvTask.getAccountId());
    assertTrue(nextTask.isPresent());
    assertEquals(cvTask.getUuid(), nextTask.get().getUuid());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testValidAfterWhenSetToFutureOnGettingNextTask() {
    CVTask cvTask = createCVTask();
    cvTask.setValidAfter(Instant.now().plus(10, ChronoUnit.MINUTES).toEpochMilli());
    cvTaskService.saveCVTask(cvTask);
    Optional<CVTask> nextTask = cvTaskService.getNextTask(cvTask.getAccountId());
    assertFalse(nextTask.isPresent());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testValidAfterWhenSetToPastOnGettingNextTask() {
    CVTask cvTask = createCVTask();
    cvTask.setValidAfter(Instant.now().minus(10, ChronoUnit.MINUTES).toEpochMilli());
    cvTaskService.saveCVTask(cvTask);
    Optional<CVTask> nextTask = cvTaskService.getNextTask(cvTask.getAccountId());
    assertTrue(nextTask.isPresent());
    assertEquals(cvTask.getUuid(), nextTask.get().getUuid());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testExecutionStatusFilterOnGettingNextTask() {
    CVTask cvTask = createCVTask();
    cvTask.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(cvTask);
    Optional<CVTask> nextTask = cvTaskService.getNextTask(cvTask.getAccountId());
    assertFalse(nextTask.isPresent());
    cvTask.setStatus(ExecutionStatus.QUEUED);
    wingsPersistence.save(cvTask);
    nextTask = cvTaskService.getNextTask(cvTask.getAccountId());
    assertTrue(nextTask.isPresent());
    assertEquals(cvTask.getUuid(), nextTask.get().getUuid());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testExecutionStatusUpdateOnGettingNextTask() {
    CVTask cvTask = createAndSaveCVTask();
    Optional<CVTask> nextTask = cvTaskService.getNextTask(cvTask.getAccountId());
    assertEquals(ExecutionStatus.RUNNING, nextTask.get().getStatus());
    CVTask reloadedCVTask = getCVTask(cvTask.getUuid());
    assertEquals(ExecutionStatus.RUNNING, reloadedCVTask.getStatus());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testOrderOnGettingNextTask() {
    List<CVTask> cvTasks = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      cvTasks.add(createAndSaveCVTask());
    }
    cvTasks.get(0).setCorrelationId(generateUUID()); // update first
    wingsPersistence.save(cvTasks.get(0));
    for (int i = 1; i < 10; i++) {
      Optional<CVTask> nextTask = cvTaskService.getNextTask(accountId);
      assertTrue(nextTask.isPresent());
      assertEquals(cvTasks.get(i).getUuid(), nextTask.get().getUuid());
    }
    Optional<CVTask> nextTask = cvTaskService.getNextTask(accountId);
    assertTrue(nextTask.isPresent());
    assertEquals(cvTasks.get(0).getUuid(), nextTask.get().getUuid());
    assertFalse(cvTaskService.getNextTask(accountId).isPresent());
  }
  @Test
  @Category(IntegrationTests.class)
  public void testIfCVTaskValidUntilIsBeingSetToOneMonth() {
    CVTask cvTask = createAndSaveCVTask();
    assertTrue(cvTask.getValidUntil().getTime() > Instant.now().toEpochMilli());
    assertThat(Math.abs(cvTask.getValidUntil().getTime()
                   - OffsetDateTime.now().plus(1, ChronoUnit.MONTHS).toInstant().toEpochMilli())
        < TimeUnit.DAYS.toMillis(3));
  }

  @Test
  @Category(IntegrationTests.class)
  public void testGetCVTaskById() {
    CVTask cvTask = createAndSaveCVTask();
    assertEquals(cvTask.getUuid(), cvTaskService.getCVTask(cvTask.getUuid()).getUuid());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testEnqueueSeqTasks() {
    long startTime = Instant.now().toEpochMilli();
    List<CVTask> cvTasks = new ArrayList<>();
    for (int minute = 0; minute < 10; minute++) {
      long startTimeMSForCurrentMinute = startTime + Duration.ofMinutes(minute).toMillis();
      CVTask cvTask = CVTask.builder()
                          .accountId(generateUUID())
                          .stateExecutionId(generateUUID())
                          .correlationId(generateUUID())
                          .validAfter(startTimeMSForCurrentMinute)
                          .status(ExecutionStatus.WAITING)
                          .build();
      cvTasks.add(cvTask);
    }
    cvTaskService.enqueueSequentialTasks(cvTasks);
    for (int i = 0; i < cvTasks.size(); i++) {
      cvTasks.set(i, cvTaskService.getCVTask(cvTasks.get(i).getUuid()));
    }
    assertEquals(ExecutionStatus.QUEUED, cvTasks.get(0).getStatus());
    for (int i = 0; i < 9; i++) {
      assertEquals(cvTasks.get(i).getNextTaskId(), cvTasks.get(i + 1).getUuid());
    }
    assertNull(cvTasks.get(cvTasks.size() - 1).getNextTaskId());
    for (int i = 1; i < 10; i++) {
      assertEquals(ExecutionStatus.WAITING, cvTasks.get(i).getStatus());
    }
  }

  @Test
  @Category(IntegrationTests.class)
  public void testUpdateTaskStatusWhenTaskResultIsSuccessful() {
    CVTask cvTask = createAndSaveCVTask(ExecutionStatus.RUNNING);
    CVTask nextTask = createAndSaveCVTask(ExecutionStatus.WAITING);
    cvTask.setNextTaskId(nextTask.getUuid());
    cvTaskService.saveCVTask(cvTask);
    DataCollectionTaskResult dataCollectionTaskResult =
        DataCollectionTaskResult.builder().status(DataCollectionTaskStatus.SUCCESS).build();
    cvTaskService.updateTaskStatus(cvTask.getUuid(), dataCollectionTaskResult);
    assertEquals(ExecutionStatus.SUCCESS, cvTaskService.getCVTask(cvTask.getUuid()).getStatus());
    assertEquals(ExecutionStatus.QUEUED, cvTaskService.getCVTask(nextTask.getUuid()).getStatus());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testUpdateTaskStatusWhenTaskHasFailed() {
    CVTask cvTask = createAndSaveCVTask(ExecutionStatus.RUNNING);
    CVTask nextTask = createAndSaveCVTask(ExecutionStatus.WAITING);
    cvTask.setNextTaskId(nextTask.getUuid());
    cvTaskService.saveCVTask(cvTask);
    DataCollectionTaskResult dataCollectionTaskResult = DataCollectionTaskResult.builder()
                                                            .status(DataCollectionTaskStatus.FAILURE)
                                                            .errorMessage("Error from unit test")
                                                            .build();
    cvTaskService.updateTaskStatus(cvTask.getUuid(), dataCollectionTaskResult);
    CVTask updatedTask1 = cvTaskService.getCVTask(cvTask.getUuid());
    assertEquals(ExecutionStatus.FAILED, updatedTask1.getStatus());
    assertEquals("Error from unit test", updatedTask1.getException());
    CVTask updatedTask2 = cvTaskService.getCVTask(nextTask.getUuid());
    assertEquals(ExecutionStatus.FAILED, updatedTask2.getStatus());
    assertEquals("Previous task failed", updatedTask2.getException());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testUpdateNotifyErrorWhenTaskHasFailed() {
    CVTask cvTask = createAndSaveCVTask(ExecutionStatus.RUNNING);
    String correlationId = generateUUID();
    cvTask.setCorrelationId(correlationId);
    cvTaskService.saveCVTask(cvTask);
    DataCollectionTaskResult dataCollectionTaskResult = DataCollectionTaskResult.builder()
                                                            .status(DataCollectionTaskStatus.FAILURE)
                                                            .errorMessage("Error from unit test")
                                                            .build();
    cvTaskService.updateTaskStatus(cvTask.getUuid(), dataCollectionTaskResult);
    ArgumentCaptor<VerificationDataAnalysisResponse> responseArgumentCaptor =
        ArgumentCaptor.forClass(VerificationDataAnalysisResponse.class);
    verify(waitNotifyEngine).notify(eq(correlationId), responseArgumentCaptor.capture());
    VerificationStateAnalysisExecutionData stateExecutionData =
        responseArgumentCaptor.getValue().getStateExecutionData();
    assertEquals("Error from unit test", stateExecutionData.getErrorMsg());
    assertEquals(ExecutionStatus.ERROR, stateExecutionData.getStatus());
    assertEquals(correlationId, stateExecutionData.getCorrelationId());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testExpireLongRunningTasksIfTaskIsNotExpired() {
    CVTask cvTask = createAndSaveCVTask(ExecutionStatus.RUNNING);
    cvTaskService.expireLongRunningTasks(cvTask.getAccountId());
    assertEquals(ExecutionStatus.RUNNING, cvTaskService.getCVTask(cvTask.getUuid()).getStatus());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testExpireLongRunningTasksIfTaskIsExpired() throws IllegalAccessException {
    CVTask cvTask = createAndSaveCVTask(ExecutionStatus.RUNNING);
    Clock clock = mock(Clock.class);
    when(clock.instant()).thenReturn(Instant.now().plus(16, ChronoUnit.MINUTES));
    FieldUtils.writeField(cvTaskService, "clock", clock, true);
    cvTaskService.expireLongRunningTasks(cvTask.getAccountId());
    CVTask updatedTask = cvTaskService.getCVTask(cvTask.getUuid());
    assertEquals(ExecutionStatus.FAILED, updatedTask.getStatus());
    assertEquals("Task timed out", updatedTask.getException());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testDependentTaskStatusUpdateIfTaskExpired() throws IllegalAccessException {
    CVTask cvTask = createAndSaveCVTask(ExecutionStatus.RUNNING);
    CVTask nextTask = createAndSaveCVTask(ExecutionStatus.WAITING);
    cvTask.setNextTaskId(nextTask.getUuid());
    cvTaskService.saveCVTask(cvTask);
    Clock clock = mock(Clock.class);
    when(clock.instant()).thenReturn(Instant.now().plus(16, ChronoUnit.MINUTES));
    FieldUtils.writeField(cvTaskService, "clock", clock, true);
    cvTaskService.expireLongRunningTasks(cvTask.getAccountId());
    CVTask updatedNextTask = cvTaskService.getCVTask(nextTask.getUuid());
    assertEquals(ExecutionStatus.FAILED, updatedNextTask.getStatus());
    assertEquals("Previous task timed out", updatedNextTask.getException());
  }

  private CVTask getCVTask(String cvTaskId) {
    return wingsPersistence.get(CVTask.class, cvTaskId);
  }

  private CVTask createCVTask() {
    return createCVTask(ExecutionStatus.QUEUED);
  }
  private CVTask createCVTask(ExecutionStatus executionStatus) {
    return CVTask.builder()
        .stateExecutionId(stateExecutionId)
        .correlationId(generateUUID())
        .accountId(accountId)
        .status(executionStatus)
        .validAfter(System.currentTimeMillis())
        .dataCollectionInfo(createDataCollectionInfo())
        .build();
  }

  private DataCollectionInfoV2 createDataCollectionInfo() {
    return SplunkDataCollectionInfoV2.builder()
        .serviceId(generateUUID())
        .workflowId(generateUUID())
        .startTime(Instant.now().minus(10, ChronoUnit.MINUTES))
        .endTime(Instant.now())
        .build();
  }

  private CVTask createAndSaveCVTask() {
    return createAndSaveCVTask(ExecutionStatus.QUEUED);
  }

  private CVTask createAndSaveCVTask(ExecutionStatus executionStatus) {
    CVTask cvTask = createCVTask(executionStatus);
    wingsPersistence.save(cvTask);
    return cvTask;
  }
}
