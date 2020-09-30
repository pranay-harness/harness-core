package io.harness.cvng.verificationjob.services.impl;

import static io.harness.cvng.core.services.CVNextGenConstants.DATA_COLLECTION_DELAY;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.core.beans.DeploymentActivityVerificationResultDTO;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.DataCollectionTask;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.DataCollectionTaskService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.models.VerificationType;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.cvng.verificationjob.beans.CanaryVerificationJobDTO;
import io.harness.cvng.verificationjob.beans.Sensitivity;
import io.harness.cvng.verificationjob.beans.VerificationJobDTO;
import io.harness.cvng.verificationjob.beans.VerificationJobInstanceDTO;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.ExecutionStatus;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.ProgressLog;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class VerificationJobInstanceServiceImplTest extends CvNextGenTest {
  @Inject private VerificationJobService verificationJobService;
  @Inject private VerificationJobInstanceService verificationJobInstanceService;
  @Inject private CVConfigService cvConfigService;
  @Mock private VerificationManagerService verificationManagerService;
  @Inject private DataCollectionTaskService dataCollectionTaskService;
  @Inject private HPersistence hPersistence;
  @Mock private NextGenService nextGenService;
  @Inject private VerificationTaskService verificationTaskService;
  private String accountId;
  private String verificationJobIdentifier;
  private long deploymentStartTimeMs;
  private String connectorId;
  private String perpetualTaskId;
  private String projectIdentifier;
  private String orgIdentifier;
  private String cvConfigId;
  @Before
  public void setup() throws IllegalAccessException {
    MockitoAnnotations.initMocks(this);
    verificationJobIdentifier = generateUuid();
    accountId = generateUuid();
    accountId = generateUuid();
    projectIdentifier = generateUuid();
    orgIdentifier = generateUuid();
    cvConfigId = generateUuid();
    deploymentStartTimeMs = Instant.parse("2020-07-27T10:44:06.390Z").toEpochMilli();
    connectorId = generateUuid();
    perpetualTaskId = generateUuid();
    FieldUtils.writeField(
        verificationJobInstanceService, "verificationManagerService", verificationManagerService, true);
    FieldUtils.writeField(verificationJobInstanceService, "nextGenService", nextGenService, true);
    when(verificationManagerService.createDeploymentVerificationPerpetualTask(any(), any(), any(), any(), any()))
        .thenReturn(perpetualTaskId);
    when(nextGenService.getEnvironment(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(EnvironmentResponseDTO.builder()
                        .accountId(accountId)
                        .identifier("production")
                        .name("Harness prod")
                        .type(EnvironmentType.Production)
                        .projectIdentifier(projectIdentifier)
                        .orgIdentifier(orgIdentifier)
                        .build());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreate_withInvalidJobIdentifier() {
    assertThatThrownBy(() -> verificationJobInstanceService.create(accountId, newVerificationJobInstanceDTO()))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("No Job exists for verificationJobIdentifier: '"
            + "" + verificationJobIdentifier + "'");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreate_withValidJobIdentifier() {
    verificationJobService.upsert(accountId, newVerificationJobDTO());
    VerificationJobInstanceDTO verificationJobInstanceDTO = newVerificationJobInstanceDTO();
    String verificationTaskId = verificationJobInstanceService.create(accountId, verificationJobInstanceDTO);
    VerificationJobInstanceDTO saved = verificationJobInstanceService.get(verificationTaskId);
    assertThat(saved.getVerificationJobIdentifier())
        .isEqualTo(verificationJobInstanceDTO.getVerificationJobIdentifier());
    assertThat(saved.getDeploymentStartTime()).isEqualTo(verificationJobInstanceDTO.getDeploymentStartTime());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreate_nullOptionalParams() {
    verificationJobService.upsert(accountId, newVerificationJobDTO());
    VerificationJobInstanceDTO verificationJobInstanceDTO =
        VerificationJobInstanceDTO.builder()
            .verificationJobIdentifier(verificationJobIdentifier)
            .deploymentStartTimeMs(deploymentStartTimeMs)
            .verificationTaskStartTimeMs(deploymentStartTimeMs + Duration.ofMinutes(2).toMillis())
            .dataCollectionDelayMs(Duration.ofMinutes(5).toMillis())
            .build();
    String verificationTaskId = verificationJobInstanceService.create(accountId, verificationJobInstanceDTO);
    VerificationJobInstanceDTO saved = verificationJobInstanceService.get(verificationTaskId);
    assertThat(saved.getVerificationJobIdentifier())
        .isEqualTo(verificationJobInstanceDTO.getVerificationJobIdentifier());
    assertThat(saved.getDeploymentStartTime()).isEqualTo(verificationJobInstanceDTO.getDeploymentStartTime());
    assertThat(saved.getNewVersionHosts()).isNull();
    assertThat(saved.getOldVersionHosts()).isNull();
    assertThat(saved.getNewHostsTrafficSplitPercentage()).isNull();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreate_validOptionalParams() {
    verificationJobService.upsert(accountId, newVerificationJobDTO());
    VerificationJobInstanceDTO verificationJobInstanceDTO =
        VerificationJobInstanceDTO.builder()
            .verificationJobIdentifier(verificationJobIdentifier)
            .deploymentStartTimeMs(deploymentStartTimeMs)
            .verificationTaskStartTimeMs(deploymentStartTimeMs + Duration.ofMinutes(2).toMillis())
            .dataCollectionDelayMs(Duration.ofMinutes(5).toMillis())
            .newVersionHosts(Sets.newHashSet("newHost1", "newHost2"))
            .oldVersionHosts(Sets.newHashSet("oldHost1", "oldHost2"))
            .newHostsTrafficSplitPercentage(30)
            .build();
    String verificationTaskId = verificationJobInstanceService.create(accountId, verificationJobInstanceDTO);
    VerificationJobInstanceDTO saved = verificationJobInstanceService.get(verificationTaskId);
    assertThat(saved.getVerificationJobIdentifier())
        .isEqualTo(verificationJobInstanceDTO.getVerificationJobIdentifier());
    assertThat(saved.getDeploymentStartTime()).isEqualTo(verificationJobInstanceDTO.getDeploymentStartTime());
    assertThat(saved.getNewVersionHosts()).isEqualTo(Sets.newHashSet("newHost1", "newHost2"));
    assertThat(saved.getOldVersionHosts()).isEqualTo(Sets.newHashSet("oldHost1", "oldHost2"));
    assertThat(saved.getNewHostsTrafficSplitPercentage()).isEqualTo(30);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void createDataCollectionTasks_validatePerpetualTaskCreationWithCorrectParams() {
    VerificationJob job = newVerificationJobDTO().getVerificationJob();
    job.setAccountId(accountId);
    job.setIdentifier(verificationJobIdentifier);
    hPersistence.save(job);
    cvConfigService.save(newCVConfig());
    String verificationTaskId = verificationJobInstanceService.create(accountId, newVerificationJobInstanceDTO());
    VerificationJobInstance verificationJobInstance =
        verificationJobInstanceService.getVerificationJobInstance(verificationTaskId);
    verificationJobInstance.setResolvedJob(job);
    verificationJobInstanceService.createDataCollectionTasks(verificationJobInstance);
    verify(verificationManagerService)
        .createDeploymentVerificationPerpetualTask(eq(accountId), eq(connectorId), eq(orgIdentifier),
            eq(projectIdentifier), eq(getDataCollectionWorkerId(verificationTaskId, connectorId)));
    VerificationJobInstance saved = verificationJobInstanceService.getVerificationJobInstance(verificationTaskId);
    assertThat(saved.getPerpetualTaskIds()).isEqualTo(Lists.newArrayList(perpetualTaskId));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void createDataCollectionTasks_validateDataCollectionTasksCreation() {
    VerificationJob job = newVerificationJobDTO().getVerificationJob();
    job.setAccountId(accountId);
    job.setIdentifier(verificationJobIdentifier);
    hPersistence.save(job);
    cvConfigService.save(newCVConfig());
    String verificationTaskId = verificationJobInstanceService.create(accountId, newVerificationJobInstanceDTO());
    VerificationJobInstance verificationJobInstance =
        verificationJobInstanceService.getVerificationJobInstance(verificationTaskId);
    verificationJobInstance.setResolvedJob(job);
    verificationJobInstanceService.createDataCollectionTasks(verificationJobInstance);
    String workerId = getDataCollectionWorkerId(verificationTaskId, connectorId);
    DataCollectionTask firstTask = dataCollectionTaskService.getNextTask(accountId, workerId).get();
    assertThat(firstTask).isNotNull();
    assertThat(firstTask.getStartTime()).isEqualTo(Instant.parse("2020-07-27T10:29:00Z"));
    assertThat(firstTask.getEndTime()).isEqualTo(Instant.parse("2020-07-27T10:44:00Z"));
    assertThat(firstTask.getValidAfter())
        .isEqualTo(Instant.parse("2020-07-27T10:44:00Z").plus(Duration.ofMinutes(5)).toEpochMilli());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreateDataCollectionTasks_validateDataCollectionTasksCreationWithDefaultDataCollectionDelay() {
    VerificationJob job = newVerificationJobDTO().getVerificationJob();
    job.setAccountId(accountId);
    job.setIdentifier(verificationJobIdentifier);
    hPersistence.save(job);
    cvConfigService.save(newCVConfig());
    VerificationJobInstanceDTO dto = VerificationJobInstanceDTO.builder()
                                         .verificationJobIdentifier(verificationJobIdentifier)
                                         .deploymentStartTimeMs(deploymentStartTimeMs)
                                         .verificationTaskStartTimeMs(deploymentStartTimeMs)
                                         .build();
    String verificationTaskId = verificationJobInstanceService.create(accountId, dto);
    VerificationJobInstance verificationJobInstance =
        verificationJobInstanceService.getVerificationJobInstance(verificationTaskId);
    verificationJobInstance.setResolvedJob(job);
    verificationJobInstanceService.createDataCollectionTasks(verificationJobInstance);
    VerificationJobInstance updated = verificationJobInstanceService.getVerificationJobInstance(verificationTaskId);
    String workerId = getDataCollectionWorkerId(verificationTaskId, connectorId);
    DataCollectionTask firstTask = dataCollectionTaskService.getNextTask(accountId, workerId).get();
    assertThat(firstTask).isNotNull();
    assertThat(firstTask.getEndTime()).isEqualTo(Instant.parse("2020-07-27T10:44:00Z"));
    assertThat(firstTask.getValidAfter())
        .isEqualTo(Instant.parse("2020-07-27T10:44:00Z").plus(DATA_COLLECTION_DELAY).toEpochMilli());
    assertThat(updated.getExecutionStatus()).isEqualTo(ExecutionStatus.RUNNING);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testLogProgress_multipleUpdates() {
    cvConfigService.save(newCVConfig());
    String verificationJobInstanceId = createVerificationJobInstance().getUuid();

    VerificationJobInstance verificationJobInstance =
        verificationJobInstanceService.getVerificationJobInstance(verificationJobInstanceId);
    assertThat(verificationJobInstance.getProgressLogs()).isEmpty();
    ProgressLog progressLog = ProgressLog.builder()
                                  .startTime(verificationJobInstance.getStartTime())
                                  .endTime(verificationJobInstance.getStartTime().plus(Duration.ofMinutes(1)))
                                  .analysisStatus(AnalysisStatus.SUCCESS)
                                  .log("time series analysis done")
                                  .build();
    verificationJobInstanceService.logProgress(verificationJobInstanceId, progressLog);
    verificationJobInstance = verificationJobInstanceService.getVerificationJobInstance(verificationJobInstanceId);
    assertThat(verificationJobInstance.getProgressLogs()).hasSize(1);
    assertThat(verificationJobInstance.getProgressLogs().get(0).getAnalysisStatus()).isEqualTo(AnalysisStatus.SUCCESS);
    assertThat(verificationJobInstance.getProgressLogs().get(0).getLog()).isEqualTo("time series analysis done");

    assertThat(verificationJobInstance.getExecutionStatus()).isEqualTo(ExecutionStatus.QUEUED);
    progressLog = ProgressLog.builder()
                      .startTime(verificationJobInstance.getEndTime().minus(Duration.ofMinutes(1)))
                      .endTime(verificationJobInstance.getEndTime())
                      .analysisStatus(AnalysisStatus.SUCCESS)
                      .isFinalState(false)
                      .build();
    verificationJobInstanceService.logProgress(verificationJobInstanceId, progressLog);
    verificationJobInstance = verificationJobInstanceService.getVerificationJobInstance(verificationJobInstanceId);
    assertThat(verificationJobInstance.getProgressLogs()).hasSize(2);
    assertThat(verificationJobInstance.getProgressLogs().get(1).getAnalysisStatus()).isEqualTo(AnalysisStatus.SUCCESS);
    assertThat(verificationJobInstance.getExecutionStatus()).isEqualTo(ExecutionStatus.QUEUED);

    progressLog = ProgressLog.builder()
                      .startTime(verificationJobInstance.getEndTime().minus(Duration.ofMinutes(1)))
                      .endTime(verificationJobInstance.getEndTime())
                      .analysisStatus(AnalysisStatus.SUCCESS)
                      .isFinalState(true)
                      .build();
    verificationJobInstanceService.logProgress(verificationJobInstanceId, progressLog);
    verificationJobInstance = verificationJobInstanceService.getVerificationJobInstance(verificationJobInstanceId);
    assertThat(verificationJobInstance.getProgressLogs()).hasSize(3);
    assertThat(verificationJobInstance.getProgressLogs().get(2).getAnalysisStatus()).isEqualTo(AnalysisStatus.SUCCESS);
    assertThat(verificationJobInstance.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testLogProgress_onFailure() {
    cvConfigService.save(newCVConfig());
    String verificationJobInstanceId = createVerificationJobInstance().getUuid();
    VerificationJobInstance verificationJobInstance =
        verificationJobInstanceService.getVerificationJobInstance(verificationJobInstanceId);
    assertThat(verificationJobInstance.getProgressLogs()).isEmpty();
    ProgressLog progressLog = ProgressLog.builder()
                                  .startTime(verificationJobInstance.getStartTime())
                                  .endTime(verificationJobInstance.getStartTime().plus(Duration.ofMinutes(1)))
                                  .analysisStatus(AnalysisStatus.FAILED)
                                  .build();
    verificationJobInstanceService.logProgress(verificationJobInstanceId, progressLog);
    verificationJobInstance = verificationJobInstanceService.getVerificationJobInstance(verificationJobInstanceId);
    assertThat(verificationJobInstance.getProgressLogs()).hasSize(1);
    assertThat(verificationJobInstance.getProgressLogs().get(0).getAnalysisStatus()).isEqualTo(AnalysisStatus.FAILED);
    assertThat(verificationJobInstance.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testDeleteDataCollectionWorkers_whenSuccessful() {
    verificationJobService.upsert(accountId, newVerificationJobDTO());
    String verificationJobInstanceId =
        verificationJobInstanceService.create(accountId, newVerificationJobInstanceDTO());
    VerificationJobInstance verificationJobInstance =
        verificationJobInstanceService.getVerificationJobInstance(verificationJobInstanceId);
    List<String> perpetualTaskIds = Lists.newArrayList(generateUuid(), generateUuid());
    verificationJobInstance.setPerpetualTaskIds(perpetualTaskIds);
    hPersistence.save(verificationJobInstance);
    verificationJobInstanceService.deletePerpetualTasks(verificationJobInstance);
    VerificationJobInstance updated =
        verificationJobInstanceService.getVerificationJobInstance(verificationJobInstanceId);
    assertThat(updated.getPerpetualTaskIds()).isNull();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testDeleteDataCollectionWorkers_whenManagerCallFails() {
    verificationJobService.upsert(accountId, newVerificationJobDTO());
    String verificationJobInstanceId =
        verificationJobInstanceService.create(accountId, newVerificationJobInstanceDTO());
    VerificationJobInstance verificationJobInstance =
        verificationJobInstanceService.getVerificationJobInstance(verificationJobInstanceId);
    List<String> perpetualTaskIds = Lists.newArrayList(generateUuid(), generateUuid());
    doThrow(new RuntimeException("exception from manager"))
        .when(verificationManagerService)
        .deletePerpetualTasks(eq(accountId), any());
    verificationJobInstance.setPerpetualTaskIds(perpetualTaskIds);
    hPersistence.save(verificationJobInstance);
    assertThatThrownBy(() -> verificationJobInstanceService.deletePerpetualTasks(verificationJobInstance))
        .hasMessage("exception from manager");
    VerificationJobInstance updated =
        verificationJobInstanceService.getVerificationJobInstance(verificationJobInstanceId);
    assertThat(updated.getPerpetualTaskIds()).isEqualTo(perpetualTaskIds);
  }
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetAggregatedVerificationResult_queuedState() {
    VerificationJobInstance verificationJobInstance = createVerificationJobInstance();
    DeploymentActivityVerificationResultDTO deploymentActivityVerificationResultDTO =
        verificationJobInstanceService.getAggregatedVerificationResult(
            Collections.singletonList(verificationJobInstance.getUuid()));
    assertThat(deploymentActivityVerificationResultDTO).isNotNull();
    assertThat(deploymentActivityVerificationResultDTO.getPreProductionDeploymentSummary()).isNull();
    assertThat(deploymentActivityVerificationResultDTO.getPostDeploymentSummary()).isNull();
    assertThat(deploymentActivityVerificationResultDTO.getProductionDeploymentSummary())
        .isEqualTo(DeploymentActivityVerificationResultDTO.DeploymentSummary.builder()
                       .total(1)
                       .startTime(verificationJobInstance.getStartTime().toEpochMilli())
                       .riskScore(null)
                       .notStarted(1)
                       .durationMs(Duration.ofMinutes(15).toMillis())
                       .timeRemainingMs(1200000)
                       .build());
  }

  private String getDataCollectionWorkerId(String verificationTaskId, String connectorId) {
    return UUID.nameUUIDFromBytes((verificationTaskId + ":" + connectorId).getBytes(Charsets.UTF_8)).toString();
  }
  private VerificationJobDTO newVerificationJobDTO() {
    CanaryVerificationJobDTO canaryVerificationJobDTO = new CanaryVerificationJobDTO();
    canaryVerificationJobDTO.setIdentifier(verificationJobIdentifier);
    canaryVerificationJobDTO.setJobName(generateUuid());
    canaryVerificationJobDTO.setDataSources(Lists.newArrayList(DataSourceType.SPLUNK));
    canaryVerificationJobDTO.setSensitivity(Sensitivity.MEDIUM.name());
    canaryVerificationJobDTO.setServiceIdentifier(generateUuid());
    canaryVerificationJobDTO.setOrgIdentifier(orgIdentifier);
    canaryVerificationJobDTO.setProjectIdentifier(projectIdentifier);
    canaryVerificationJobDTO.setEnvIdentifier(generateUuid());
    canaryVerificationJobDTO.setSensitivity(Sensitivity.MEDIUM.name());
    canaryVerificationJobDTO.setDuration("15m");
    return canaryVerificationJobDTO;
  }

  private CVConfig newCVConfig() {
    SplunkCVConfig cvConfig = new SplunkCVConfig();
    cvConfig.setQuery("exception");
    cvConfig.setServiceInstanceIdentifier("serviceInstanceIdentifier");
    cvConfig.setVerificationType(VerificationType.LOG);
    cvConfig.setAccountId(accountId);
    cvConfig.setConnectorIdentifier(connectorId);
    cvConfig.setServiceIdentifier(generateUuid());
    cvConfig.setEnvIdentifier(generateUuid());
    cvConfig.setProjectIdentifier(generateUuid());
    cvConfig.setGroupId("groupId");
    cvConfig.setCategory(CVMonitoringCategory.PERFORMANCE);
    cvConfig.setProductName("productName");
    return cvConfig;
  }
  private VerificationJobInstance createVerificationJobInstance() {
    verificationJobService.upsert(accountId, newVerificationJobDTO());
    VerificationJob verificationJob = verificationJobService.getVerificationJob(accountId, verificationJobIdentifier);
    VerificationJobInstance verificationJobInstance =
        VerificationJobInstance.builder()
            .accountId(accountId)
            .executionStatus(ExecutionStatus.QUEUED)
            .verificationJobIdentifier(verificationJobIdentifier)
            .deploymentStartTime(Instant.ofEpochMilli(deploymentStartTimeMs))
            .resolvedJob(verificationJob)
            .startTime(Instant.ofEpochMilli(deploymentStartTimeMs + Duration.ofMinutes(2).toMillis()))
            .build();
    verificationJobInstanceService.create(verificationJobInstance);
    verificationTaskService.create(accountId, cvConfigId, verificationJobInstance.getUuid());
    return verificationJobInstance;
  }

  private VerificationJobInstanceDTO newVerificationJobInstanceDTO() {
    return VerificationJobInstanceDTO.builder()
        .verificationJobIdentifier(verificationJobIdentifier)
        .deploymentStartTimeMs(deploymentStartTimeMs)
        .verificationTaskStartTimeMs(deploymentStartTimeMs + Duration.ofMinutes(2).toMillis())
        .dataCollectionDelayMs(Duration.ofMinutes(5).toMillis())
        .build();
  }
}
