package io.harness.cvng.core.services.impl;

import static io.harness.cvng.beans.DataCollectionExecutionStatus.QUEUED;
import static io.harness.cvng.beans.DataCollectionExecutionStatus.SUCCESS;
import static io.harness.cvng.core.entities.DeploymentDataCollectionTask.MAX_RETRY_COUNT;
import static io.harness.cvng.core.services.CVNextGenConstants.DATA_COLLECTION_DELAY;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.NEMANJA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.AppDynamicsDataCollectionInfo;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataCollectionConnectorBundle;
import io.harness.cvng.beans.DataCollectionExecutionStatus;
import io.harness.cvng.beans.DataCollectionInfo;
import io.harness.cvng.beans.DataCollectionTaskDTO;
import io.harness.cvng.beans.DataCollectionTaskDTO.DataCollectionTaskResult;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.SplunkDataCollectionInfo;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.DataCollectionTask;
import io.harness.cvng.core.entities.DataCollectionTask.DataCollectionTaskKeys;
import io.harness.cvng.core.entities.DataCollectionTask.Type;
import io.harness.cvng.core.entities.DeploymentDataCollectionTask;
import io.harness.cvng.core.entities.ServiceGuardDataCollectionTask;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.DataCollectionTaskService;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.models.VerificationType;
import io.harness.cvng.verificationjob.beans.Sensitivity;
import io.harness.cvng.verificationjob.beans.TestVerificationJobDTO;
import io.harness.cvng.verificationjob.beans.VerificationJobDTO;
import io.harness.cvng.verificationjob.entities.TestVerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.ExecutionStatus;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class DataCollectionTaskServiceImplTest extends CvNextGenTest {
  @Inject private DataCollectionTaskService dataCollectionTaskService;
  @Inject private HPersistence hPersistence;
  @Inject private CVConfigService cvConfigService;
  @Inject private MetricPackService metricPackService;
  @Inject private VerificationTaskService verificationTaskService;
  @Mock private Clock clock;
  @Inject private VerificationJobService verificationJobService;
  @Inject private VerificationJobInstanceService verificationJobInstanceService;

  private String cvConfigId;
  private String accountId;
  private Instant fakeNow;
  private String dataCollectionWorkerId;
  private String verificationTaskId;

  @Before
  public void setupTests() throws IllegalAccessException {
    initMocks(this);
    accountId = generateUuid();
    CVConfig cvConfig = cvConfigService.save(createCVConfig());
    cvConfigId = cvConfig.getUuid();
    verificationTaskId = verificationTaskService.getServiceGuardVerificationTaskId(accountId, cvConfigId);
    dataCollectionTaskService = spy(dataCollectionTaskService);
    clock = Clock.fixed(Instant.parse("2020-04-22T10:02:06Z"), ZoneOffset.UTC);
    FieldUtils.writeField(dataCollectionTaskService, "clock", clock, true);
    FieldUtils.writeField(verificationJobInstanceService, "clock", clock, true);
    FieldUtils.writeField(
        dataCollectionTaskService, "verificationJobInstanceService", verificationJobInstanceService, true);
    fakeNow = clock.instant();
    dataCollectionWorkerId = cvConfigId;
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testSave_dataCollectionTask() {
    DataCollectionTask dataCollectionTask = create();
    dataCollectionTaskService.save(dataCollectionTask);
    assertThat(dataCollectionTask.getStatus()).isEqualTo(QUEUED);
    DataCollectionTask updatedDataCollectionTask = getDataCollectionTask(dataCollectionTask.getUuid());
    assertThat(updatedDataCollectionTask.getStatus()).isEqualTo(QUEUED);
    assertThat(updatedDataCollectionTask.getVerificationTaskId()).isEqualTo(dataCollectionTask.getVerificationTaskId());
    assertThat(updatedDataCollectionTask.shouldQueueAnalysis()).isTrue();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetNextTask_validAfterWhenNotSetOnGettingNextTask() {
    DataCollectionTask dataCollectionTask = create();
    dataCollectionTaskService.save(dataCollectionTask);
    Optional<DataCollectionTask> nextTask =
        dataCollectionTaskService.getNextTask(dataCollectionTask.getAccountId(), dataCollectionWorkerId);
    assertThat(nextTask.isPresent()).isTrue();
    assertThat(nextTask.get().getUuid()).isEqualTo(dataCollectionTask.getUuid());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetNextTaskDTO_dtoCreation() {
    DataCollectionTask dataCollectionTask = create();
    dataCollectionTaskService.save(dataCollectionTask);
    Optional<DataCollectionTaskDTO> nextTask =
        dataCollectionTaskService.getNextTaskDTO(dataCollectionTask.getAccountId(), dataCollectionWorkerId);
    assertThat(nextTask.isPresent()).isTrue();
    assertThat(nextTask.get().getUuid()).isEqualTo(dataCollectionTask.getUuid());
    assertThat(nextTask.get().getAccountId()).isEqualTo(dataCollectionTask.getAccountId());
    assertThat(nextTask.get().getDataCollectionInfo()).isEqualTo(dataCollectionTask.getDataCollectionInfo());
    assertThat(nextTask.get().getStartTime()).isEqualTo(dataCollectionTask.getStartTime());
    assertThat(nextTask.get().getEndTime()).isEqualTo(dataCollectionTask.getEndTime());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetNextTaskDTO_notPresent() {
    DataCollectionTask dataCollectionTask = create();
    dataCollectionTaskService.save(dataCollectionTask);
    Optional<DataCollectionTaskDTO> nextTask =
        dataCollectionTaskService.getNextTaskDTO(dataCollectionTask.getAccountId(), "invalid-worker-id");
    assertThat(nextTask.isPresent()).isFalse();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetNextTask_validAfterWhenSetToFutureOnGettingNextTask() {
    DataCollectionTask dataCollectionTask = create();
    dataCollectionTask.setValidAfter(Instant.now().plus(10, ChronoUnit.MINUTES));
    dataCollectionTaskService.save(dataCollectionTask);
    Optional<DataCollectionTask> nextTask =
        dataCollectionTaskService.getNextTask(dataCollectionTask.getAccountId(), dataCollectionWorkerId);
    assertThat(nextTask.isPresent()).isFalse();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetNextTask_validAfterWhenSetToPastOnGettingNextTask() {
    DataCollectionTask dataCollectionTask = create();
    dataCollectionTask.setValidAfter(clock.instant().minus(10, ChronoUnit.MINUTES));
    dataCollectionTaskService.save(dataCollectionTask);
    Optional<DataCollectionTask> nextTask =
        dataCollectionTaskService.getNextTask(dataCollectionTask.getAccountId(), dataCollectionWorkerId);
    assertThat(nextTask.isPresent()).isTrue();
    assertThat(nextTask.get().getUuid()).isEqualTo(dataCollectionTask.getUuid());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetNextTask_ExecutionStatusFilterOnGettingNextTask() {
    DataCollectionTask dataCollectionTask = create();
    dataCollectionTask.setStatus(DataCollectionExecutionStatus.RUNNING);
    hPersistence.save(dataCollectionTask);
    Optional<DataCollectionTask> nextTask =
        dataCollectionTaskService.getNextTask(dataCollectionTask.getAccountId(), dataCollectionWorkerId);
    assertThat(nextTask.isPresent()).isFalse();
    dataCollectionTask.setStatus(QUEUED);
    hPersistence.save(dataCollectionTask);
    nextTask = dataCollectionTaskService.getNextTask(dataCollectionTask.getAccountId(), dataCollectionWorkerId);
    assertThat(nextTask.isPresent()).isTrue();
    assertThat(nextTask.get().getUuid()).isEqualTo(dataCollectionTask.getUuid());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetNextTask_executionStatusUpdateOnGettingNextTask() {
    DataCollectionTask dataCollectionTask = createAndSave(QUEUED);
    Optional<DataCollectionTask> nextTask =
        dataCollectionTaskService.getNextTask(dataCollectionTask.getAccountId(), dataCollectionWorkerId);
    assertThat(nextTask.get().getStatus()).isEqualTo(DataCollectionExecutionStatus.RUNNING);
    DataCollectionTask reloadedDataCollectionTask = getDataCollectionTask(dataCollectionTask.getUuid());
    assertThat(reloadedDataCollectionTask.getStatus()).isEqualTo(DataCollectionExecutionStatus.RUNNING);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetNextTask_orderOnGettingNextTask() throws InterruptedException {
    List<DataCollectionTask> dataCollectionTasks = new ArrayList<>();
    long time = clock.millis();
    for (int i = 0; i < 5; i++) {
      DataCollectionTask dataCollectionTask = create();
      // dataCollectionTask.setLastUpdatedAt(time + i);
      // TODO: find a way to set lastUpdatedAt.
      Thread.sleep(1);
      dataCollectionTasks.add(dataCollectionTask);
      hPersistence.save(dataCollectionTask);
    }
    Thread.sleep(1);
    // dataCollectionTasks.get(0).setLastUpdatedAt(time + 6);
    hPersistence.save(dataCollectionTasks.get(0));
    for (int i = 1; i < 5; i++) {
      Optional<DataCollectionTask> nextTask = dataCollectionTaskService.getNextTask(accountId, dataCollectionWorkerId);
      assertThat(nextTask.isPresent()).isTrue();
      assertThat(nextTask.get().getUuid()).isEqualTo(dataCollectionTasks.get(i).getUuid());
    }
    Optional<DataCollectionTask> nextTask = dataCollectionTaskService.getNextTask(accountId, dataCollectionWorkerId);
    assertThat(nextTask.isPresent()).isTrue();
    assertThat(nextTask.get().getUuid()).isEqualTo(dataCollectionTasks.get(0).getUuid());
    assertThat(dataCollectionTaskService.getNextTask(accountId, dataCollectionWorkerId).isPresent()).isFalse();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetNextTask_taskPickedUpAgainAfterNotCompletedForMoreThanFiveMinutes() throws IllegalAccessException {
    DataCollectionTask dataCollectionTask = create(DataCollectionExecutionStatus.RUNNING);
    clock = Clock.fixed(Instant.now().plus(15, ChronoUnit.MINUTES), ZoneOffset.UTC);
    FieldUtils.writeField(dataCollectionTaskService, "clock", clock, true);
    hPersistence.save(dataCollectionTask);
    Optional<DataCollectionTask> nextTask = dataCollectionTaskService.getNextTask(accountId, dataCollectionWorkerId);
    assertThat(nextTask.isPresent()).isTrue();
    assertThat(nextTask.get().getRetryCount()).isEqualTo(1);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetNextTask_withBothQueuedAndRunningTask() throws IllegalAccessException {
    clock = Clock.fixed(Instant.now().plus(6, ChronoUnit.MINUTES), ZoneOffset.UTC);
    FieldUtils.writeField(dataCollectionTaskService, "clock", clock, true);

    DataCollectionTask queuedDataCollectionTask = create(QUEUED);
    DataCollectionTask runningDataCollectionTask = create(DataCollectionExecutionStatus.RUNNING);
    hPersistence.save(queuedDataCollectionTask);
    hPersistence.save(runningDataCollectionTask);

    Optional<DataCollectionTask> nextTask = dataCollectionTaskService.getNextTask(accountId, dataCollectionWorkerId);
    assertThat(nextTask.isPresent()).isTrue();
    nextTask = dataCollectionTaskService.getNextTask(accountId, dataCollectionWorkerId);
    assertThat(nextTask.isPresent()).isTrue();
    clock = Clock.fixed(Instant.now(), ZoneOffset.UTC);
    FieldUtils.writeField(dataCollectionTaskService, "clock", clock, true);
    nextTask = dataCollectionTaskService.getNextTask(accountId, dataCollectionWorkerId);
    assertThat(nextTask.isPresent()).isFalse();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetNextTask_withExceededRetryCountDeployment() {
    DataCollectionTask dataCollectionTask = create(QUEUED, Type.DEPLOYMENT);
    dataCollectionTask.setRetryCount(MAX_RETRY_COUNT + 1);
    hPersistence.save(dataCollectionTask);
    Optional<DataCollectionTask> nextTask = dataCollectionTaskService.getNextTask(accountId, dataCollectionWorkerId);
    assertThat(nextTask.isPresent()).isFalse();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetNextTask_withExceededRetryCountServiceGuard() {
    DataCollectionTask dataCollectionTask = create(QUEUED, Type.SERVICE_GUARD);
    dataCollectionTask.setRetryCount(MAX_RETRY_COUNT + 1);
    hPersistence.save(dataCollectionTask);
    Optional<DataCollectionTask> nextTask = dataCollectionTaskService.getNextTask(accountId, dataCollectionWorkerId);
    assertThat(nextTask.isPresent()).isTrue();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetValidUntil_dataCollectionTaskValidUntilIsBeingSetToOneMonth() {
    DataCollectionTask dataCollectionTask = create(DataCollectionExecutionStatus.SUCCESS);
    assertThat(dataCollectionTask.getValidUntil().getTime() > Instant.now().toEpochMilli()).isTrue();
    assertThat(dataCollectionTask.getValidUntil().getTime() >= Instant.now().plus(29, ChronoUnit.DAYS).toEpochMilli())
        .isTrue();
    assertThat(dataCollectionTask.getValidUntil().getTime() <= Instant.now().plus(31, ChronoUnit.DAYS).toEpochMilli())
        .isTrue();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetDataCollectionTask_byId() {
    DataCollectionTask dataCollectionTask = createAndSave(DataCollectionExecutionStatus.SUCCESS);
    assertThat(dataCollectionTask.getUuid())
        .isEqualTo(dataCollectionTaskService.getDataCollectionTask(dataCollectionTask.getUuid()).getUuid());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testUpdateTaskStatus_taskStatusToSuccess() {
    DataCollectionTask dataCollectionTask = createAndSave(QUEUED);
    DataCollectionTaskResult result = DataCollectionTaskDTO.DataCollectionTaskResult.builder()
                                          .status(DataCollectionExecutionStatus.SUCCESS)
                                          .dataCollectionTaskId(dataCollectionTask.getUuid())
                                          .build();
    cvConfigService.update(getCVConfig());
    dataCollectionTaskService.updateTaskStatus(result);
    DataCollectionTask updated = dataCollectionTaskService.getDataCollectionTask(dataCollectionTask.getUuid());
    assertThat(updated.getStatus()).isEqualTo(DataCollectionExecutionStatus.SUCCESS);
    assertThat(updated.getException()).isNull();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testUpdateTaskStatus_multipleCallsWithSameResult() {
    DataCollectionTask dataCollectionTask = createAndSave(QUEUED);
    DataCollectionTaskResult result = DataCollectionTaskDTO.DataCollectionTaskResult.builder()
                                          .status(DataCollectionExecutionStatus.SUCCESS)
                                          .dataCollectionTaskId(dataCollectionTask.getUuid())
                                          .build();
    cvConfigService.update(getCVConfig());
    dataCollectionTaskService.updateTaskStatus(result);
    assertThatThrownBy(() -> dataCollectionTaskService.updateTaskStatus(result))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testUpdateTaskStatus_taskStatusSuccessShouldCreateNextTask() {
    DataCollectionTask dataCollectionTask = createAndSave(QUEUED);
    DataCollectionTaskResult result = DataCollectionTaskResult.builder()
                                          .status(DataCollectionExecutionStatus.SUCCESS)
                                          .dataCollectionTaskId(dataCollectionTask.getUuid())
                                          .build();
    cvConfigService.update(getCVConfig());
    dataCollectionTaskService.updateTaskStatus(result);
    DataCollectionTask nextTask = hPersistence.createQuery(DataCollectionTask.class)
                                      .filter(DataCollectionTaskKeys.accountId, accountId)
                                      .filter(DataCollectionTaskKeys.verificationTaskId, verificationTaskId)
                                      .filter(DataCollectionTaskKeys.status, QUEUED)
                                      .order(DataCollectionTaskKeys.lastUpdatedAt)
                                      .get();

    assertThat(nextTask.getStatus()).isEqualTo(QUEUED);
    assertThat(nextTask.getStartTime()).isEqualTo(dataCollectionTask.getEndTime());
    assertThat(nextTask.getEndTime()).isEqualTo(dataCollectionTask.getEndTime().plus(5, ChronoUnit.MINUTES));
    assertThat(nextTask.getDataCollectionWorkerId()).isEqualTo(cvConfigId);
    assertThat(nextTask.getValidAfter())
        .isEqualTo(dataCollectionTask.getEndTime().plus(5, ChronoUnit.MINUTES).plus(DATA_COLLECTION_DELAY));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testUpdateTaskStatus_ifLastSuccessIsBefore2Hour() throws IllegalAccessException {
    DataCollectionTask dataCollectionTask = createAndSave(QUEUED);
    DataCollectionTaskResult result = DataCollectionTaskResult.builder()
                                          .status(DataCollectionExecutionStatus.SUCCESS)
                                          .dataCollectionTaskId(dataCollectionTask.getUuid())
                                          .build();
    cvConfigService.update(getCVConfig());
    Clock clock = Clock.fixed(this.clock.instant().plus(Duration.ofHours(3)), ZoneOffset.UTC);
    FieldUtils.writeField(dataCollectionTaskService, "clock", clock, true);
    dataCollectionTaskService.updateTaskStatus(result);
    DataCollectionTask nextTask = hPersistence.createQuery(DataCollectionTask.class)
                                      .filter(DataCollectionTaskKeys.accountId, accountId)
                                      .filter(DataCollectionTaskKeys.verificationTaskId, verificationTaskId)
                                      .filter(DataCollectionTaskKeys.status, QUEUED)
                                      .order(DataCollectionTaskKeys.lastUpdatedAt)
                                      .get();

    assertThat(nextTask.getStatus()).isEqualTo(QUEUED);
    Instant expectedStartTime = Instant.parse("2020-04-22T11:00:00Z");
    assertThat(nextTask.getStartTime()).isEqualTo(expectedStartTime);
    assertThat(nextTask.getEndTime()).isEqualTo(expectedStartTime.plus(5, ChronoUnit.MINUTES));
    assertThat(nextTask.getDataCollectionWorkerId()).isEqualTo(cvConfigId);
    assertThat(nextTask.getValidAfter())
        .isEqualTo(expectedStartTime.plus(5, ChronoUnit.MINUTES).plus(DATA_COLLECTION_DELAY));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testUpdateTaskStatus_retryFailedTask() {
    Exception exception = new RuntimeException("exception msg");
    DataCollectionTask dataCollectionTask = createAndSave(QUEUED);
    DataCollectionTaskResult result = DataCollectionTaskDTO.DataCollectionTaskResult.builder()
                                          .status(DataCollectionExecutionStatus.FAILED)
                                          .stacktrace(ExceptionUtils.getStackTrace(exception))
                                          .dataCollectionTaskId(dataCollectionTask.getUuid())
                                          .exception(exception.getMessage())
                                          .build();
    dataCollectionTaskService.updateTaskStatus(result);
    DataCollectionTask updated = dataCollectionTaskService.getDataCollectionTask(dataCollectionTask.getUuid());
    assertThat(updated.getStatus()).isEqualTo(QUEUED);
    assertThat(updated.getRetryCount()).isEqualTo(1);
    assertThat(updated.getException()).isEqualTo(exception.getMessage());
    assertThat(updated.getStacktrace()).isEqualTo(ExceptionUtils.getStackTrace(exception));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testUpdateTaskStatus_deploymentDataCollectionDontRetryIfRetryCountExceeds() {
    Exception exception = new RuntimeException("exception msg");
    VerificationJobInstance verificationJobInstance = createVerificationJobInstance();
    DataCollectionTask dataCollectionTask = createAndSave(QUEUED, Type.DEPLOYMENT);
    DataCollectionTaskResult result = DataCollectionTaskDTO.DataCollectionTaskResult.builder()
                                          .status(DataCollectionExecutionStatus.FAILED)
                                          .stacktrace(ExceptionUtils.getStackTrace(exception))
                                          .dataCollectionTaskId(dataCollectionTask.getUuid())
                                          .exception(exception.getMessage())
                                          .build();
    MAX_RETRY_COUNT = 2;
    int maxRetry = MAX_RETRY_COUNT;
    IntStream.range(0, maxRetry).forEach(index -> {
      dataCollectionTaskService.updateTaskStatus(result);
      DataCollectionTask updated = dataCollectionTaskService.getDataCollectionTask(dataCollectionTask.getUuid());
      assertThat(updated.getStatus()).isEqualTo(QUEUED);
      assertThat(updated.getRetryCount()).isEqualTo(index + 1);
      assertThat(updated.getException()).isEqualTo(exception.getMessage());
      assertThat(updated.getStacktrace()).isEqualTo(ExceptionUtils.getStackTrace(exception));
    });
    dataCollectionTaskService.updateTaskStatus(result);
    DataCollectionTask updated = dataCollectionTaskService.getDataCollectionTask(dataCollectionTask.getUuid());
    assertThat(updated.getStatus()).isEqualTo(DataCollectionExecutionStatus.FAILED);
    assertThat(updated.getRetryCount()).isEqualTo(maxRetry);
    assertThat(updated.getException()).isEqualTo(exception.getMessage());
    assertThat(updated.getStacktrace()).isEqualTo(ExceptionUtils.getStackTrace(exception));

    VerificationJobInstance jobInstanceWithProgressLog =
        verificationJobInstanceService.getVerificationJobInstance(verificationJobInstance.getUuid());
    assertThat(jobInstanceWithProgressLog.getProgressLogs()).hasSize(1);
    assertThat(jobInstanceWithProgressLog.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
    assertThat(jobInstanceWithProgressLog.getProgressLogs().get(0))
        .isEqualTo(VerificationJobInstance.DataCollectionProgressLog.builder()
                       .executionStatus(DataCollectionExecutionStatus.FAILED)
                       .verificationTaskId(verificationTaskId)
                       .startTime(dataCollectionTask.getStartTime())
                       .createdAt(clock.instant())
                       .endTime(dataCollectionTask.getEndTime())
                       .log("Data collection failed with exception: exception msg")
                       .isFinalState(false)
                       .build());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testUpdateTaskStatus_deploymentSuccessful() {
    VerificationJobInstance verificationJobInstance = createVerificationJobInstance();
    DataCollectionTask dataCollectionTask = createAndSave(QUEUED, Type.DEPLOYMENT);
    DataCollectionTaskResult result = DataCollectionTaskDTO.DataCollectionTaskResult.builder()
                                          .status(DataCollectionExecutionStatus.SUCCESS)
                                          .dataCollectionTaskId(dataCollectionTask.getUuid())
                                          .build();
    dataCollectionTaskService.updateTaskStatus(result);
    DataCollectionTask updated = dataCollectionTaskService.getDataCollectionTask(dataCollectionTask.getUuid());
    assertThat(updated.getStatus()).isEqualTo(SUCCESS);
    assertThat(updated.getRetryCount()).isEqualTo(0);
    assertThat(updated.getException()).isNull();
    assertThat(updated.getStacktrace()).isNull();
    VerificationJobInstance jobInstanceWithProgressLog =
        verificationJobInstanceService.getVerificationJobInstance(verificationJobInstance.getUuid());
    assertThat(jobInstanceWithProgressLog.getProgressLogs()).hasSize(1);
    assertThat(jobInstanceWithProgressLog.getExecutionStatus()).isEqualTo(ExecutionStatus.QUEUED);
    assertThat(jobInstanceWithProgressLog.getProgressLogs().get(0))
        .isEqualTo(VerificationJobInstance.DataCollectionProgressLog.builder()
                       .executionStatus(SUCCESS)
                       .verificationTaskId(verificationTaskId)
                       .startTime(dataCollectionTask.getStartTime())
                       .createdAt(clock.instant())
                       .endTime(dataCollectionTask.getEndTime())
                       .log("Data collection task successful")
                       .isFinalState(false)
                       .build());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testUpdateTaskStatus_serviceGuardCreateNewTaskIfRetryTaskIsTooOld() throws IllegalAccessException {
    Exception exception = new RuntimeException("exception msg");
    DataCollectionTask dataCollectionTask = createAndSave(QUEUED, Type.SERVICE_GUARD);
    DataCollectionTaskResult result = DataCollectionTaskDTO.DataCollectionTaskResult.builder()
                                          .status(DataCollectionExecutionStatus.FAILED)
                                          .stacktrace(ExceptionUtils.getStackTrace(exception))
                                          .dataCollectionTaskId(dataCollectionTask.getUuid())
                                          .exception(exception.getMessage())
                                          .build();
    dataCollectionTaskService.updateTaskStatus(result);
    Clock clock = Clock.fixed(this.clock.instant().plus(Duration.ofHours(3)), ZoneOffset.UTC);
    FieldUtils.writeField(dataCollectionTaskService, "clock", clock, true);
    dataCollectionTaskService.updateTaskStatus(result);
    DataCollectionTask updated = dataCollectionTaskService.getDataCollectionTask(dataCollectionTask.getUuid());
    assertThat(updated.getStatus()).isEqualTo(DataCollectionExecutionStatus.FAILED);
    assertThat(updated.getRetryCount()).isEqualTo(1);
    assertThat(updated.getException()).isEqualTo("exception msg");
    DataCollectionTask newTask =
        hPersistence.createQuery(DataCollectionTask.class).filter(DataCollectionTaskKeys.status, QUEUED).get();
    assertThat(newTask.getStatus()).isEqualTo(QUEUED);
    assertThat(newTask.getRetryCount()).isEqualTo(1);
    assertThat(newTask.getValidAfter()).isEqualTo(Instant.parse("2020-04-22T13:02:16Z"));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testEnqueueFirstTask_forMetricsConfig() throws IllegalAccessException {
    String taskId = generateUuid();
    VerificationManagerService verificationManagerService = mock(VerificationManagerService.class);
    FieldUtils.writeField(dataCollectionTaskService, "verificationManagerService", verificationManagerService, true);

    when(verificationManagerService.createDataCollectionTask(
             eq(accountId), eq("orgIdentifier"), eq("projectIdentifier"), any(DataCollectionConnectorBundle.class)))
        .thenReturn(taskId);
    AppDynamicsCVConfig cvConfig = getCVConfig();

    String taskIdFromApi = dataCollectionTaskService.enqueueFirstTask(cvConfig);
    DataCollectionTask savedTask = hPersistence.createQuery(DataCollectionTask.class)
                                       .filter(DataCollectionTaskKeys.verificationTaskId, verificationTaskId)
                                       .get();
    assertThat(savedTask.getStatus()).isEqualTo(QUEUED);
    assertThat(savedTask.getDataCollectionInfo()).isInstanceOf(AppDynamicsDataCollectionInfo.class);
    assertThat(taskIdFromApi).isEqualTo(taskId);
    assertThat(savedTask.getEndTime()).isEqualTo(cvConfig.getFirstTimeDataCollectionTimeRange().getEndTime());
    assertThat(savedTask.getStartTime()).isEqualTo(cvConfig.getFirstTimeDataCollectionTimeRange().getStartTime());
    assertThat(savedTask.getVerificationTaskId())
        .isEqualTo(verificationTaskService.getServiceGuardVerificationTaskId(accountId, cvConfigId));
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testDeleteDataCollectionTask() throws IllegalAccessException {
    String taskId = generateUuid();
    VerificationManagerService verificationManagerService = mock(VerificationManagerService.class);
    FieldUtils.writeField(dataCollectionTaskService, "verificationManagerService", verificationManagerService, true);
    dataCollectionTaskService.deletePerpetualTasks(accountId, taskId);
    verify(verificationManagerService, times(1)).deletePerpetualTask(accountId, taskId);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testEnqueueFirstTask_forLogConfig() throws IllegalAccessException {
    String taskId = generateUuid();
    VerificationManagerService verificationManagerService = mock(VerificationManagerService.class);
    FieldUtils.writeField(dataCollectionTaskService, "verificationManagerService", verificationManagerService, true);

    when(verificationManagerService.createDataCollectionTask(
             eq(accountId), eq("orgIdentifier"), eq("projectIdentifier"), any(DataCollectionConnectorBundle.class)))
        .thenReturn(taskId);
    SplunkCVConfig cvConfig = getSplunkCVConfig();

    String taskIdFromApi = dataCollectionTaskService.enqueueFirstTask(cvConfig);
    DataCollectionTask savedTask = hPersistence.createQuery(DataCollectionTask.class)
                                       .filter(DataCollectionTaskKeys.verificationTaskId, verificationTaskId)
                                       .get();
    assertThat(savedTask.getStatus()).isEqualTo(QUEUED);
    assertThat(savedTask.getDataCollectionInfo()).isInstanceOf(SplunkDataCollectionInfo.class);
    assertThat(taskIdFromApi).isEqualTo(taskId);
    assertThat(savedTask.getEndTime()).isEqualTo(cvConfig.getFirstTimeDataCollectionTimeRange().getEndTime());
    assertThat(savedTask.getStartTime()).isEqualTo(cvConfig.getFirstTimeDataCollectionTimeRange().getStartTime());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreateSeqTasks() {
    List<DataCollectionTask> dataCollectionTasks = new ArrayList<>();
    for (int minute = 0; minute < 10; minute++) {
      DataCollectionTask dataCollectionTask = create();
      dataCollectionTasks.add(dataCollectionTask);
    }
    dataCollectionTaskService.createSeqTasks(dataCollectionTasks);
    for (int i = 0; i < dataCollectionTasks.size(); i++) {
      dataCollectionTasks.set(i, dataCollectionTaskService.getDataCollectionTask(dataCollectionTasks.get(i).getUuid()));
    }
    assertThat(dataCollectionTasks.get(0).getStatus()).isEqualTo(QUEUED);
    for (int i = 0; i < 9; i++) {
      assertThat(dataCollectionTasks.get(i + 1).getUuid()).isEqualTo(dataCollectionTasks.get(i).getNextTaskId());
    }
    assertThat(dataCollectionTasks.get(dataCollectionTasks.size() - 1).getNextTaskId()).isNull();
    for (int i = 1; i < 10; i++) {
      assertThat(dataCollectionTasks.get(i).getStatus()).isEqualTo(DataCollectionExecutionStatus.WAITING);
    }
    assertThat(fakeNow).isEqualTo(dataCollectionTasks.get(0).getValidAfter());
  }

  private AppDynamicsCVConfig getCVConfig() {
    AppDynamicsCVConfig cvConfig = new AppDynamicsCVConfig();
    cvConfig.setProjectIdentifier("projectIdentifier");
    cvConfig.setUuid(cvConfigId);
    cvConfig.setAccountId(accountId);
    cvConfig.setApplicationName("cv-app");
    cvConfig.setTierName("docker-tier");
    cvConfig.setVerificationType(VerificationType.TIME_SERIES);
    cvConfig.setConnectorIdentifier(generateUuid());
    cvConfig.setServiceIdentifier("serviceIdentifier");
    cvConfig.setEnvIdentifier("envIdentifier");
    cvConfig.setIdentifier(generateUuid());
    cvConfig.setMonitoringSourceName(generateUuid());
    cvConfig.setApplicationName("applicationName");
    cvConfig.setTierName("tierName");
    cvConfig.setOrgIdentifier("orgIdentifier");
    cvConfig.setMetricPack(
        metricPackService.getMetricPacks(accountId, "org", "projectId", DataSourceType.APP_DYNAMICS).get(0));
    return cvConfig;
  }

  private SplunkCVConfig getSplunkCVConfig() {
    SplunkCVConfig cvConfig = new SplunkCVConfig();
    cvConfig.setCreatedAt(clock.millis());
    cvConfig.setProjectIdentifier("projectIdentifier");
    cvConfig.setUuid(cvConfigId);
    cvConfig.setAccountId(accountId);
    cvConfig.setVerificationType(VerificationType.TIME_SERIES);
    cvConfig.setConnectorIdentifier(generateUuid());
    cvConfig.setServiceIdentifier("serviceIdentifier");
    cvConfig.setEnvIdentifier("envIdentifier");
    cvConfig.setOrgIdentifier("orgIdentifier");
    cvConfig.setIdentifier(generateUuid());
    cvConfig.setMonitoringSourceName(generateUuid());
    cvConfig.setQuery("excetpion");
    cvConfig.setServiceInstanceIdentifier("host");
    return cvConfig;
  }

  private DataCollectionTask getDataCollectionTask(String dataCollectionTaskId) {
    return hPersistence.get(DataCollectionTask.class, dataCollectionTaskId);
  }

  private DataCollectionTask create() {
    return create(QUEUED);
  }

  private DataCollectionInfo createDataCollectionInfo() {
    return AppDynamicsDataCollectionInfo.builder().build();
  }

  private DataCollectionTask create(DataCollectionExecutionStatus executionStatus) {
    return create(executionStatus, Type.SERVICE_GUARD);
  }
  private DataCollectionTask create(DataCollectionExecutionStatus executionStatus, Type type) {
    if (type == Type.SERVICE_GUARD) {
      return ServiceGuardDataCollectionTask.builder()
          .verificationTaskId(verificationTaskId)
          .type(Type.SERVICE_GUARD)
          .dataCollectionWorkerId(cvConfigId)
          .accountId(accountId)
          .startTime(fakeNow.minus(Duration.ofMinutes(7)))
          .endTime(fakeNow.minus(Duration.ofMinutes(2)))
          .status(executionStatus)
          .dataCollectionInfo(createDataCollectionInfo())
          .build();
    } else {
      return DeploymentDataCollectionTask.builder()
          .verificationTaskId(verificationTaskId)
          .dataCollectionWorkerId(dataCollectionWorkerId)
          .type(Type.DEPLOYMENT)
          .accountId(accountId)
          .startTime(fakeNow.minus(Duration.ofMinutes(7)))
          .endTime(fakeNow.minus(Duration.ofMinutes(2)))
          .status(executionStatus)
          .dataCollectionInfo(createDataCollectionInfo())
          .build();
    }
  }

  private DataCollectionTask createAndSave(DataCollectionExecutionStatus executionStatus) {
    DataCollectionTask dataCollectionTask = create(executionStatus);
    hPersistence.save(dataCollectionTask);
    return dataCollectionTask;
  }

  private DataCollectionTask createAndSave(DataCollectionExecutionStatus executionStatus, Type type) {
    DataCollectionTask dataCollectionTask = create(executionStatus, type);
    hPersistence.save(dataCollectionTask);
    return dataCollectionTask;
  }

  private CVConfig createCVConfig() {
    SplunkCVConfig cvConfig = new SplunkCVConfig();
    fillCommon(cvConfig);
    cvConfig.setQuery("exception");
    cvConfig.setServiceInstanceIdentifier(generateUuid());
    return cvConfig;
  }

  private void fillCommon(CVConfig cvConfig) {
    cvConfig.setVerificationType(VerificationType.LOG);
    cvConfig.setAccountId(accountId);
    cvConfig.setConnectorIdentifier(generateUuid());
    cvConfig.setServiceIdentifier(generateUuid());
    cvConfig.setEnvIdentifier(generateUuid());
    cvConfig.setProjectIdentifier(generateUuid());
    cvConfig.setOrgIdentifier(generateUuid());
    cvConfig.setIdentifier(generateUuid());
    cvConfig.setMonitoringSourceName(generateUuid());
    cvConfig.setCategory(CVMonitoringCategory.PERFORMANCE);
    cvConfig.setProductName(generateUuid());
  }

  private VerificationJobInstance createVerificationJobInstance() {
    VerificationJobDTO verificationJobDTO = newVerificationJobDTO();
    verificationJobService.upsert(accountId, verificationJobDTO);
    VerificationJob verificationJob =
        verificationJobService.getVerificationJob(accountId, verificationJobDTO.getIdentifier());
    VerificationJobInstance verificationJobInstance =
        VerificationJobInstance.builder()
            .accountId(accountId)
            .executionStatus(ExecutionStatus.QUEUED)
            .verificationJobIdentifier(verificationJob.getIdentifier())
            .deploymentStartTime(Instant.ofEpochMilli(clock.millis()))
            .resolvedJob(verificationJob)
            .startTime(Instant.ofEpochMilli(clock.millis() + Duration.ofMinutes(2).toMillis()))
            .build();

    verificationJobInstance.setUuid(((TestVerificationJob) verificationJob).getBaselineVerificationJobInstanceId());

    verificationJobInstanceService.create(verificationJobInstance);
    verificationTaskId = verificationTaskService.create(accountId, cvConfigId, verificationJobInstance.getUuid());
    return verificationJobInstance;
  }
  private VerificationJobDTO newVerificationJobDTO() {
    TestVerificationJobDTO testVerificationJob = new TestVerificationJobDTO();
    testVerificationJob.setIdentifier(generateUuid());
    testVerificationJob.setJobName(generateUuid());
    testVerificationJob.setDataSources(Lists.newArrayList(DataSourceType.SPLUNK));
    testVerificationJob.setServiceIdentifier(generateUuid());
    testVerificationJob.setOrgIdentifier(generateUuid());
    testVerificationJob.setProjectIdentifier(generateUuid());
    testVerificationJob.setEnvIdentifier(generateUuid());
    testVerificationJob.setSensitivity(Sensitivity.MEDIUM.name());
    testVerificationJob.setDuration("15m");
    testVerificationJob.setBaselineVerificationJobInstanceId(generateUuid());
    return testVerificationJob;
  }
}
