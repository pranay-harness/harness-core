package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.cvng.CVNextGenBaseTest;
import io.harness.cvng.beans.AppDynamicsDataCollectionInfo;
import io.harness.cvng.beans.DataCollectionInfo;
import io.harness.cvng.beans.DataCollectionTaskDTO;
import io.harness.cvng.beans.DataCollectionTaskDTO.DataCollectionTaskResult;
import io.harness.cvng.beans.ExecutionStatus;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.DataCollectionTask;
import io.harness.cvng.core.entities.DataCollectionTask.DataCollectionTaskKeys;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.services.api.DataCollectionTaskService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DataCollectionTaskServiceImplTest extends CVNextGenBaseTest {
  @Inject private DataCollectionTaskService dataCollectionTaskService;
  @Inject private HPersistence hPersistence;
  private String cvConfigId;
  private String accountId;

  @Before
  public void setupTests() {
    initMocks(this);
    accountId = generateUuid();
    cvConfigId = generateUuid();
    dataCollectionTaskService = spy(dataCollectionTaskService);
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
        dataCollectionTaskService.getNextTask(dataCollectionTask.getAccountId(), cvConfigId);
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
        dataCollectionTaskService.getNextTaskDTO(dataCollectionTask.getAccountId(), cvConfigId);
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
        dataCollectionTaskService.getNextTaskDTO(dataCollectionTask.getAccountId(), "invalid-cv-config");
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
        dataCollectionTaskService.getNextTask(dataCollectionTask.getAccountId(), cvConfigId);
    assertThat(nextTask.isPresent()).isFalse();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetNextTask_validAfterWhenSetToPastOnGettingNextTask() {
    DataCollectionTask dataCollectionTask = create();
    dataCollectionTask.setValidAfter(Instant.now().minus(10, ChronoUnit.MINUTES).toEpochMilli());
    dataCollectionTaskService.save(dataCollectionTask);
    Optional<DataCollectionTask> nextTask =
        dataCollectionTaskService.getNextTask(dataCollectionTask.getAccountId(), cvConfigId);
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
        dataCollectionTaskService.getNextTask(dataCollectionTask.getAccountId(), cvConfigId);
    assertThat(nextTask.isPresent()).isFalse();
    dataCollectionTask.setStatus(ExecutionStatus.QUEUED);
    hPersistence.save(dataCollectionTask);
    nextTask = dataCollectionTaskService.getNextTask(dataCollectionTask.getAccountId(), cvConfigId);
    assertThat(nextTask.isPresent()).isTrue();
    assertThat(nextTask.get().getUuid()).isEqualTo(dataCollectionTask.getUuid());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetNextTask_executionStatusUpdateOnGettingNextTask() {
    DataCollectionTask dataCollectionTask = createAndSave(ExecutionStatus.QUEUED);
    Optional<DataCollectionTask> nextTask =
        dataCollectionTaskService.getNextTask(dataCollectionTask.getAccountId(), cvConfigId);
    assertThat(nextTask.get().getStatus()).isEqualTo(ExecutionStatus.RUNNING);
    DataCollectionTask reloadedDataCollectionTask = getDataCollectionTask(dataCollectionTask.getUuid());
    assertThat(reloadedDataCollectionTask.getStatus()).isEqualTo(ExecutionStatus.RUNNING);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetNextTask_orderOnGettingNextTask() {
    List<DataCollectionTask> dataCollectionTasks = new ArrayList<>();
    long time = System.currentTimeMillis();
    for (int i = 0; i < 10; i++) {
      DataCollectionTask dataCollectionTask = create();
      dataCollectionTask.setLastUpdatedAt(time + i);
      dataCollectionTasks.add(dataCollectionTask);
      hPersistence.save(dataCollectionTask);
    }
    dataCollectionTasks.get(0).setLastUpdatedAt(time + 11);
    hPersistence.save(dataCollectionTasks.get(0));
    for (int i = 1; i < 10; i++) {
      Optional<DataCollectionTask> nextTask = dataCollectionTaskService.getNextTask(accountId, cvConfigId);
      assertThat(nextTask.isPresent()).isTrue();
      assertThat(nextTask.get().getUuid()).isEqualTo(dataCollectionTasks.get(i).getUuid());
    }
    Optional<DataCollectionTask> nextTask = dataCollectionTaskService.getNextTask(accountId, cvConfigId);
    assertThat(nextTask.isPresent()).isTrue();
    assertThat(nextTask.get().getUuid()).isEqualTo(dataCollectionTasks.get(0).getUuid());
    assertThat(dataCollectionTaskService.getNextTask(accountId, cvConfigId).isPresent()).isFalse();
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
    dataCollectionTaskService.updateTaskStatus(result);
    DataCollectionTask updated = dataCollectionTaskService.getDataCollectionTask(dataCollectionTask.getUuid());
    assertThat(updated.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(updated.getException()).isNull();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testUpdateTaskStatus_taskStatusToFailed() {
    DataCollectionTask dataCollectionTask = createAndSave(ExecutionStatus.QUEUED);
    DataCollectionTaskResult result = DataCollectionTaskDTO.DataCollectionTaskResult.builder()
                                          .status(ExecutionStatus.FAILED)
                                          .dataCollectionTaskId(dataCollectionTask.getUuid())
                                          .exception("exception")
                                          .build();
    dataCollectionTaskService.updateTaskStatus(result);
    DataCollectionTask updated = dataCollectionTaskService.getDataCollectionTask(dataCollectionTask.getUuid());
    assertThat(updated.getStatus()).isEqualTo(ExecutionStatus.FAILED);
    assertThat(updated.getException()).isEqualTo("exception");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testEnqueueFirstTask() throws IllegalAccessException {
    String taskId = generateUuid();
    VerificationManagerService verificationManagerService = mock(VerificationManagerService.class);
    FieldUtils.writeField(dataCollectionTaskService, "verificationManagerService", verificationManagerService, true);
    when(verificationManagerService.createDataCollectionTask(eq(accountId), eq(cvConfigId), anyString()))
        .thenReturn(taskId);
    AppDynamicsCVConfig cvConfig = new AppDynamicsCVConfig();
    cvConfig.setUuid(cvConfigId);
    cvConfig.setAccountId(accountId);
    cvConfig.setApplicationId(1234);
    cvConfig.setTierId(1234);
    cvConfig.setMetricPack(MetricPack.builder()
                               .dataCollectionDsl("data-collection-dsl")
                               .identifier("Performance and Availability")
                               .build());
    String taskIdFromApi = dataCollectionTaskService.enqueueFirstTask(cvConfig);
    DataCollectionTask savedTask =
        hPersistence.createQuery(DataCollectionTask.class).filter(DataCollectionTaskKeys.cvConfigId, cvConfigId).get();
    assertThat(savedTask.getStatus()).isEqualTo(ExecutionStatus.QUEUED);
    assertThat(savedTask.getDataCollectionInfo()).isInstanceOf(AppDynamicsDataCollectionInfo.class);
    assertThat(taskIdFromApi).isEqualTo(taskId);
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
        .accountId(accountId)
        .status(executionStatus)
        .validAfter(System.currentTimeMillis())
        .dataCollectionInfo(createDataCollectionInfo())
        .build();
  }

  private DataCollectionTask createAndSave(ExecutionStatus executionStatus) {
    DataCollectionTask dataCollectionTask = create(executionStatus);
    hPersistence.save(dataCollectionTask);
    return dataCollectionTask;
  }
}