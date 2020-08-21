package io.harness.cvng.core.services.impl;

import static io.harness.cvng.core.services.CVNextGenConstants.DATA_COLLECTION_DELAY;
import static io.harness.cvng.core.services.impl.DataCollectionTaskServiceImpl.MAX_RETRY_COUNT;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.NEMANJA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.inject.Inject;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.AppDynamicsDataCollectionInfo;
import io.harness.cvng.beans.DataCollectionInfo;
import io.harness.cvng.beans.DataCollectionTaskDTO;
import io.harness.cvng.beans.DataCollectionTaskDTO.DataCollectionTaskResult;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.ExecutionStatus;
import io.harness.cvng.beans.SplunkDataCollectionInfo;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.DataCollectionTask;
import io.harness.cvng.core.entities.DataCollectionTask.DataCollectionTaskKeys;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.DataCollectionTaskService;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.cvng.models.VerificationType;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public class DataCollectionTaskServiceImplTest extends CvNextGenTest {
  @Inject private DataCollectionTaskService dataCollectionTaskService;
  @Inject private HPersistence hPersistence;
  @Inject private CVConfigService cvConfigService;
  @Inject private MetricPackService metricPackService;
  @Mock private Clock clock;

  private String cvConfigId;
  private String accountId;
  private Instant fakeNow;
  private String dataCollectionWorkerId;
  private String orgIdentifier;
  private String projectIdentifier;

  @Before
  public void setupTests() throws IllegalAccessException {
    initMocks(this);
    accountId = generateUuid();
    cvConfigId = generateUuid();
    dataCollectionTaskService = spy(dataCollectionTaskService);
    clock = Clock.fixed(Instant.parse("2020-04-22T10:02:06Z"), ZoneOffset.UTC);
    FieldUtils.writeField(dataCollectionTaskService, "clock", clock, true);
    fakeNow = clock.instant();
    dataCollectionWorkerId = generateUuid();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testSave_dataCollectionTask() {
    DataCollectionTask dataCollectionTask = create();
    dataCollectionTaskService.save(dataCollectionTask);
    assertThat(dataCollectionTask.getStatus()).isEqualTo(ExecutionStatus.QUEUED);
    DataCollectionTask updatedDataCollectionTask = getDataCollectionTask(dataCollectionTask.getUuid());
    assertThat(updatedDataCollectionTask.getStatus()).isEqualTo(ExecutionStatus.QUEUED);
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
    dataCollectionTask.setValidAfter(Instant.now().plus(10, ChronoUnit.MINUTES).toEpochMilli());
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
    dataCollectionTask.setValidAfter(clock.instant().minus(10, ChronoUnit.MINUTES).toEpochMilli());
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
    dataCollectionTask.setStatus(ExecutionStatus.RUNNING);
    hPersistence.save(dataCollectionTask);
    Optional<DataCollectionTask> nextTask =
        dataCollectionTaskService.getNextTask(dataCollectionTask.getAccountId(), dataCollectionWorkerId);
    assertThat(nextTask.isPresent()).isFalse();
    dataCollectionTask.setStatus(ExecutionStatus.QUEUED);
    hPersistence.save(dataCollectionTask);
    nextTask = dataCollectionTaskService.getNextTask(dataCollectionTask.getAccountId(), dataCollectionWorkerId);
    assertThat(nextTask.isPresent()).isTrue();
    assertThat(nextTask.get().getUuid()).isEqualTo(dataCollectionTask.getUuid());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetNextTask_executionStatusUpdateOnGettingNextTask() {
    DataCollectionTask dataCollectionTask = createAndSave(ExecutionStatus.QUEUED);
    Optional<DataCollectionTask> nextTask =
        dataCollectionTaskService.getNextTask(dataCollectionTask.getAccountId(), dataCollectionWorkerId);
    assertThat(nextTask.get().getStatus()).isEqualTo(ExecutionStatus.RUNNING);
    DataCollectionTask reloadedDataCollectionTask = getDataCollectionTask(dataCollectionTask.getUuid());
    assertThat(reloadedDataCollectionTask.getStatus()).isEqualTo(ExecutionStatus.RUNNING);
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
    DataCollectionTask dataCollectionTask = create(ExecutionStatus.RUNNING);
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

    DataCollectionTask queuedDataCollectionTask = create(ExecutionStatus.QUEUED);
    DataCollectionTask runningDataCollectionTask = create(ExecutionStatus.RUNNING);
    runningDataCollectionTask.setEndTime(null);

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
  public void testGetNextTask_withExceededRetryCount() {
    DataCollectionTask dataCollectionTask = create();
    dataCollectionTask.setRetryCount(10);
    hPersistence.save(dataCollectionTask);
    Optional<DataCollectionTask> nextTask = dataCollectionTaskService.getNextTask(accountId, dataCollectionWorkerId);
    assertThat(nextTask.isPresent()).isFalse();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetValidUntil_dataCollectionTaskValidUntilIsBeingSetToOneMonth() {
    DataCollectionTask dataCollectionTask = create(ExecutionStatus.SUCCESS);
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
    DataCollectionTask dataCollectionTask = createAndSave(ExecutionStatus.SUCCESS);
    assertThat(dataCollectionTask.getUuid())
        .isEqualTo(dataCollectionTaskService.getDataCollectionTask(dataCollectionTask.getUuid()).getUuid());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testUpdateTaskStatus_taskStatusToSuccess() {
    DataCollectionTask dataCollectionTask = createAndSave(ExecutionStatus.QUEUED);
    DataCollectionTaskResult result = DataCollectionTaskDTO.DataCollectionTaskResult.builder()
                                          .status(ExecutionStatus.SUCCESS)
                                          .dataCollectionTaskId(dataCollectionTask.getUuid())
                                          .build();
    cvConfigService.update(getCVConfig());
    dataCollectionTaskService.updateTaskStatus(result);
    DataCollectionTask updated = dataCollectionTaskService.getDataCollectionTask(dataCollectionTask.getUuid());
    assertThat(updated.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(updated.getException()).isNull();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testUpdateTaskStatus_taskStatusSuccessShouldCreateNextTask() {
    DataCollectionTask dataCollectionTask = createAndSave(ExecutionStatus.QUEUED);
    DataCollectionTaskResult result = DataCollectionTaskResult.builder()
                                          .status(ExecutionStatus.SUCCESS)
                                          .dataCollectionTaskId(dataCollectionTask.getUuid())
                                          .build();
    cvConfigService.update(getCVConfig());
    dataCollectionTaskService.updateTaskStatus(result);
    DataCollectionTask nextTask = hPersistence.createQuery(DataCollectionTask.class)
                                      .filter(DataCollectionTaskKeys.accountId, accountId)
                                      .filter(DataCollectionTaskKeys.cvConfigId, cvConfigId)
                                      .filter(DataCollectionTaskKeys.status, ExecutionStatus.QUEUED)
                                      .order(DataCollectionTaskKeys.lastUpdatedAt)
                                      .get();

    assertThat(nextTask.getStatus()).isEqualTo(ExecutionStatus.QUEUED);
    assertThat(nextTask.getStartTime()).isEqualTo(dataCollectionTask.getEndTime());
    assertThat(nextTask.getEndTime()).isEqualTo(dataCollectionTask.getEndTime().plus(5, ChronoUnit.MINUTES));
    assertThat(nextTask.getDataCollectionWorkerId()).isEqualTo(cvConfigId);
    assertThat(nextTask.getValidAfter())
        .isEqualTo(dataCollectionTask.getEndTime().plus(5, ChronoUnit.MINUTES).toEpochMilli()
            + DATA_COLLECTION_DELAY.toMillis());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testUpdateTaskStatus_retryFailedTask() {
    Exception exception = new RuntimeException("exception msg");
    DataCollectionTask dataCollectionTask = createAndSave(ExecutionStatus.QUEUED);
    DataCollectionTaskResult result = DataCollectionTaskDTO.DataCollectionTaskResult.builder()
                                          .status(ExecutionStatus.FAILED)
                                          .stacktrace(ExceptionUtils.getStackTrace(exception))
                                          .dataCollectionTaskId(dataCollectionTask.getUuid())
                                          .exception(exception.getMessage())
                                          .build();
    dataCollectionTaskService.updateTaskStatus(result);
    DataCollectionTask updated = dataCollectionTaskService.getDataCollectionTask(dataCollectionTask.getUuid());
    assertThat(updated.getStatus()).isEqualTo(ExecutionStatus.QUEUED);
    assertThat(updated.getRetryCount()).isEqualTo(1);
    assertThat(updated.getException()).isEqualTo(exception.getMessage());
    assertThat(updated.getStacktrace()).isEqualTo(ExceptionUtils.getStackTrace(exception));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testUpdateTaskStatus_dontRetryIfRetryCountExceeds() {
    Exception exception = new RuntimeException("exception msg");
    DataCollectionTask dataCollectionTask = createAndSave(ExecutionStatus.QUEUED);
    DataCollectionTaskResult result = DataCollectionTaskDTO.DataCollectionTaskResult.builder()
                                          .status(ExecutionStatus.FAILED)
                                          .stacktrace(ExceptionUtils.getStackTrace(exception))
                                          .dataCollectionTaskId(dataCollectionTask.getUuid())
                                          .exception(exception.getMessage())
                                          .build();
    MAX_RETRY_COUNT = 2;
    int maxRetry = MAX_RETRY_COUNT;
    IntStream.range(0, maxRetry).forEach(index -> {
      dataCollectionTaskService.updateTaskStatus(result);
      DataCollectionTask updated = dataCollectionTaskService.getDataCollectionTask(dataCollectionTask.getUuid());
      assertThat(updated.getStatus()).isEqualTo(ExecutionStatus.QUEUED);
      assertThat(updated.getRetryCount()).isEqualTo(index + 1);
      assertThat(updated.getException()).isEqualTo(exception.getMessage());
      assertThat(updated.getStacktrace()).isEqualTo(ExceptionUtils.getStackTrace(exception));
    });
    dataCollectionTaskService.updateTaskStatus(result);
    DataCollectionTask updated = dataCollectionTaskService.getDataCollectionTask(dataCollectionTask.getUuid());
    assertThat(updated.getStatus()).isEqualTo(ExecutionStatus.FAILED);
    assertThat(updated.getRetryCount()).isEqualTo(maxRetry);
    assertThat(updated.getException()).isEqualTo(exception.getMessage());
    assertThat(updated.getStacktrace()).isEqualTo(ExceptionUtils.getStackTrace(exception));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testEnqueueFirstTask_forMetricsConfig() throws IllegalAccessException {
    String taskId = generateUuid();
    VerificationManagerService verificationManagerService = mock(VerificationManagerService.class);
    FieldUtils.writeField(dataCollectionTaskService, "verificationManagerService", verificationManagerService, true);

    when(verificationManagerService.createServiceGuardDataCollectionTask(
             eq(accountId), eq(cvConfigId), anyString(), anyString(), anyString(), eq(cvConfigId)))
        .thenReturn(taskId);
    AppDynamicsCVConfig cvConfig = getCVConfig();

    String taskIdFromApi = dataCollectionTaskService.enqueueFirstTask(cvConfig);
    DataCollectionTask savedTask =
        hPersistence.createQuery(DataCollectionTask.class).filter(DataCollectionTaskKeys.cvConfigId, cvConfigId).get();
    assertThat(savedTask.getStatus()).isEqualTo(ExecutionStatus.QUEUED);
    assertThat(savedTask.getDataCollectionInfo()).isInstanceOf(AppDynamicsDataCollectionInfo.class);
    assertThat(taskIdFromApi).isEqualTo(taskId);
    assertThat(savedTask.getEndTime()).isEqualTo(cvConfig.getFirstTimeDataCollectionTimeRange().getEndTime());
    assertThat(savedTask.getStartTime()).isEqualTo(cvConfig.getFirstTimeDataCollectionTimeRange().getStartTime());
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testDeleteDataCollectionTask() throws IllegalAccessException {
    String taskId = generateUuid();
    VerificationManagerService verificationManagerService = mock(VerificationManagerService.class);
    FieldUtils.writeField(dataCollectionTaskService, "verificationManagerService", verificationManagerService, true);
    dataCollectionTaskService.deleteDataCollectionTask(accountId, taskId);
    verify(verificationManagerService, times(1)).deleteDataCollectionTask(accountId, taskId);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testEnqueueFirstTask_forLogConfig() throws IllegalAccessException {
    String taskId = generateUuid();
    VerificationManagerService verificationManagerService = mock(VerificationManagerService.class);
    FieldUtils.writeField(dataCollectionTaskService, "verificationManagerService", verificationManagerService, true);

    when(verificationManagerService.createServiceGuardDataCollectionTask(
             eq(accountId), eq(cvConfigId), anyString(), anyString(), anyString(), eq(cvConfigId)))
        .thenReturn(taskId);
    SplunkCVConfig cvConfig = getSplunkCVConfig();

    String taskIdFromApi = dataCollectionTaskService.enqueueFirstTask(cvConfig);
    DataCollectionTask savedTask =
        hPersistence.createQuery(DataCollectionTask.class).filter(DataCollectionTaskKeys.cvConfigId, cvConfigId).get();
    assertThat(savedTask.getStatus()).isEqualTo(ExecutionStatus.QUEUED);
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
    assertThat(dataCollectionTasks.get(0).getStatus()).isEqualTo(ExecutionStatus.QUEUED);
    for (int i = 0; i < 9; i++) {
      assertThat(dataCollectionTasks.get(i + 1).getUuid()).isEqualTo(dataCollectionTasks.get(i).getNextTaskId());
    }
    assertThat(dataCollectionTasks.get(dataCollectionTasks.size() - 1).getNextTaskId()).isNull();
    for (int i = 1; i < 10; i++) {
      assertThat(dataCollectionTasks.get(i).getStatus()).isEqualTo(ExecutionStatus.WAITING);
    }
  }

  private AppDynamicsCVConfig getCVConfig() {
    AppDynamicsCVConfig cvConfig = new AppDynamicsCVConfig();
    cvConfig.setProjectIdentifier("projectIdentifier");
    cvConfig.setUuid(cvConfigId);
    cvConfig.setAccountId(accountId);
    cvConfig.setApplicationId(1234);
    cvConfig.setTierId(1234);
    cvConfig.setVerificationType(VerificationType.TIME_SERIES);
    cvConfig.setConnectorId(generateUuid());
    cvConfig.setServiceIdentifier("serviceIdentifier");
    cvConfig.setEnvIdentifier("envIdentifier");
    cvConfig.setGroupId(generateUuid());
    cvConfig.setApplicationName("applicationName");
    cvConfig.setTierName("tierName");
    cvConfig.setMetricPack(
        metricPackService.getMetricPacks(accountId, "projectId", DataSourceType.APP_DYNAMICS).get(0));
    return cvConfig;
  }

  private SplunkCVConfig getSplunkCVConfig() {
    SplunkCVConfig cvConfig = new SplunkCVConfig();
    cvConfig.setCreatedAt(clock.millis());
    cvConfig.setProjectIdentifier("projectIdentifier");
    cvConfig.setUuid(cvConfigId);
    cvConfig.setAccountId(accountId);
    cvConfig.setVerificationType(VerificationType.TIME_SERIES);
    cvConfig.setConnectorId(generateUuid());
    cvConfig.setServiceIdentifier("serviceIdentifier");
    cvConfig.setEnvIdentifier("envIdentifier");
    cvConfig.setGroupId(generateUuid());
    cvConfig.setQuery("excetpion");
    cvConfig.setServiceInstanceIdentifier("host");
    return cvConfig;
  }

  private DataCollectionTask getDataCollectionTask(String dataCollectionTaskId) {
    return hPersistence.get(DataCollectionTask.class, dataCollectionTaskId);
  }

  private DataCollectionTask create() {
    return create(ExecutionStatus.QUEUED);
  }

  private DataCollectionInfo createDataCollectionInfo() {
    return AppDynamicsDataCollectionInfo.builder().build();
  }

  private DataCollectionTask create(ExecutionStatus executionStatus) {
    return DataCollectionTask.builder()
        .cvConfigId(cvConfigId)
        .dataCollectionWorkerId(dataCollectionWorkerId)
        .accountId(accountId)
        .startTime(fakeNow.minus(5, ChronoUnit.MINUTES))
        .endTime(fakeNow)
        .status(executionStatus)
        .dataCollectionInfo(createDataCollectionInfo())
        .build();
  }

  private DataCollectionTask createAndSave(ExecutionStatus executionStatus) {
    DataCollectionTask dataCollectionTask = create(executionStatus);
    hPersistence.save(dataCollectionTask);
    return dataCollectionTask;
  }
}
