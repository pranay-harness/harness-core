package io.harness.cvng.verificationjob.services.impl;

import static io.harness.cvng.core.services.CVNextGenConstants.DATA_COLLECTION_DELAY;
import static io.harness.cvng.verificationjob.beans.VerificationJobType.CANARY;
import static io.harness.cvng.verificationjob.beans.VerificationJobType.TEST;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.NEMANJA;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static io.harness.rule.OwnerRule.RAGHU;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.activity.beans.ActivityVerificationSummary;
import io.harness.cvng.activity.beans.DeploymentActivityPopoverResultDTO;
import io.harness.cvng.activity.beans.DeploymentActivityPopoverResultDTO.DeploymentPopoverSummary;
import io.harness.cvng.activity.beans.DeploymentActivityResultDTO;
import io.harness.cvng.activity.beans.DeploymentActivityResultDTO.DeploymentResultSummary;
import io.harness.cvng.activity.beans.DeploymentActivityVerificationResultDTO;
import io.harness.cvng.analysis.beans.CanaryDeploymentAdditionalInfo;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataCollectionConnectorBundle;
import io.harness.cvng.beans.DataCollectionType;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.activity.ActivityVerificationStatus;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.CVConfig.CVConfigKeys;
import io.harness.cvng.core.entities.DataCollectionTask;
import io.harness.cvng.core.entities.DataCollectionTask.DataCollectionTaskKeys;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.DataCollectionTaskService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.models.VerificationType;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.cvng.statemachine.entities.AnalysisStateMachine;
import io.harness.cvng.statemachine.entities.AnalysisStateMachine.AnalysisStateMachineKeys;
import io.harness.cvng.verificationjob.beans.CanaryVerificationJobDTO;
import io.harness.cvng.verificationjob.beans.HealthVerificationJobDTO;
import io.harness.cvng.verificationjob.beans.Sensitivity;
import io.harness.cvng.verificationjob.beans.TestVerificationBaselineExecutionDTO;
import io.harness.cvng.verificationjob.beans.TestVerificationJobDTO;
import io.harness.cvng.verificationjob.beans.VerificationJobDTO;
import io.harness.cvng.verificationjob.beans.VerificationJobInstanceDTO;
import io.harness.cvng.verificationjob.beans.VerificationJobType;
import io.harness.cvng.verificationjob.entities.HealthVerificationJob;
import io.harness.cvng.verificationjob.entities.TestVerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob.RuntimeParameter;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.AnalysisProgressLog;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.ExecutionStatus;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class VerificationJobInstanceServiceImplTest extends CvNextGenTest {
  @Inject private VerificationJobService verificationJobService;
  @Inject private VerificationJobInstanceService verificationJobInstanceService;
  @Inject private CVConfigService cvConfigService;
  @Mock private VerificationManagerService verificationManagerService;
  @Inject private DataCollectionTaskService dataCollectionTaskService;
  @Inject private HPersistence hPersistence;
  @Mock private NextGenService nextGenService;
  @Inject private VerificationTaskService verificationTaskService;

  @Mock private Clock clock;
  private Instant fakeNow;
  private String accountId;
  private String verificationJobIdentifier;
  private long deploymentStartTimeMs;
  private String connectorId;
  private String perpetualTaskId;
  private String projectIdentifier;
  private String orgIdentifier;
  private String cvConfigId;
  private String serviceIdentifier;

  @Before
  public void setup() throws IllegalAccessException {
    MockitoAnnotations.initMocks(this);
    verificationJobIdentifier = generateUuid();
    accountId = generateUuid();
    accountId = generateUuid();
    projectIdentifier = generateUuid();
    orgIdentifier = generateUuid();
    cvConfigId = generateUuid();
    serviceIdentifier = generateUuid();
    deploymentStartTimeMs = Instant.parse("2020-07-27T10:44:06.390Z").toEpochMilli();
    connectorId = generateUuid();
    perpetualTaskId = generateUuid();
    fakeNow = Instant.parse("2020-07-27T10:50:00.390Z");
    clock = Clock.fixed(fakeNow, ZoneOffset.UTC);
    FieldUtils.writeField(verificationJobInstanceService, "clock", clock, true);
    FieldUtils.writeField(
        verificationJobInstanceService, "verificationManagerService", verificationManagerService, true);
    FieldUtils.writeField(verificationJobInstanceService, "nextGenService", nextGenService, true);
    when(verificationManagerService.createDataCollectionTask(any(), any(), any(), any())).thenReturn(perpetualTaskId);

    when(nextGenService.getEnvironment("dev", accountId, orgIdentifier, projectIdentifier))
        .thenReturn(EnvironmentResponseDTO.builder()
                        .accountId(accountId)
                        .identifier("dev")
                        .name("Harness dev")
                        .type(EnvironmentType.PreProduction)
                        .projectIdentifier(projectIdentifier)
                        .orgIdentifier(orgIdentifier)
                        .build());

    when(nextGenService.getEnvironment("prod", accountId, orgIdentifier, projectIdentifier))
        .thenReturn(EnvironmentResponseDTO.builder()
                        .accountId(accountId)
                        .identifier("prod")
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
    verificationJobService.upsert(accountId, newCanaryVerificationJobDTO());
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
    verificationJobService.upsert(accountId, newCanaryVerificationJobDTO());
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
    verificationJobService.upsert(accountId, newCanaryVerificationJobDTO());
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
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testProcessVerificationJobInstance_canaryAndTest() {
    VerificationJob job = newCanaryVerificationJobDTO().getVerificationJob();
    job.setAccountId(accountId);
    job.setIdentifier(verificationJobIdentifier);
    hPersistence.save(job);
    CVConfig cvConfig = cvConfigService.save(newCVConfig());
    String verificationTaskId = verificationJobInstanceService.create(accountId, newVerificationJobInstanceDTO());
    VerificationJobInstance verificationJobInstance =
        verificationJobInstanceService.getVerificationJobInstance(verificationTaskId);
    verificationJobInstance.setResolvedJob(job);

    // behavior under test
    verificationJobInstanceService.processVerificationJobInstance(verificationJobInstance);

    // validate that data collection tasks are created since this is canary
    Map<String, String> params = new HashMap<>();
    params.put(DataCollectionTaskKeys.dataCollectionWorkerId,
        UUID.nameUUIDFromBytes((verificationJobInstance.getUuid() + ":" + connectorId).getBytes(Charsets.UTF_8))
            .toString());
    params.put(CVConfigKeys.connectorIdentifier, connectorId);
    verify(verificationManagerService)
        .createDataCollectionTask(
            eq(accountId), eq(orgIdentifier), eq(projectIdentifier), any(DataCollectionConnectorBundle.class));
    VerificationJobInstance saved = verificationJobInstanceService.getVerificationJobInstance(verificationTaskId);
    assertThat(saved.getPerpetualTaskIds()).isEqualTo(Lists.newArrayList(perpetualTaskId));
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testProcessVerificationJobInstance_health() {
    VerificationJob job = newHealthVerificationJobDTO().getVerificationJob();
    job.setAccountId(accountId);
    job.setIdentifier(verificationJobIdentifier);
    hPersistence.save(job);
    CVConfig cvConfig = cvConfigService.save(newCVConfig());
    String verificationTaskId = verificationJobInstanceService.create(accountId, newVerificationJobInstanceDTO());
    VerificationJobInstance verificationJobInstance =
        verificationJobInstanceService.getVerificationJobInstance(verificationTaskId);
    String verificationJobInstanceId = generateUuid();
    verificationJobInstance.setUuid(verificationJobInstanceId);
    verificationJobInstance.setResolvedJob(job);
    verificationJobInstance.setStartTime(Instant.now());
    verificationJobInstance.setPreActivityVerificationStartTime(
        verificationJobInstance.getStartTime().minus(job.getDuration()));
    verificationJobInstance.setPostActivityVerificationStartTime(Instant.now());
    hPersistence.save(verificationJobInstance);

    // behavior under test
    verificationJobInstanceService.processVerificationJobInstance(verificationJobInstance);

    // validate that state machine is created since this is health
    Set<String> verTaskIds = verificationTaskService.getVerificationTaskIds(accountId, verificationJobInstanceId);
    AnalysisStateMachine stateMachine = hPersistence.createQuery(AnalysisStateMachine.class)
                                            .field(AnalysisStateMachineKeys.verificationTaskId)
                                            .in(verTaskIds)
                                            .get();
    assertThat(stateMachine).isNotNull();
    assertThat(stateMachine.getStatus().name()).isEqualTo(AnalysisStatus.RUNNING.name());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void createDataCollectionTasks_validatePerpetualTaskCreationWithCorrectParams() {
    VerificationJob job = newCanaryVerificationJobDTO().getVerificationJob();
    job.setAccountId(accountId);
    job.setIdentifier(verificationJobIdentifier);
    hPersistence.save(job);
    CVConfig cvConfig = cvConfigService.save(newCVConfig());
    String verificationTaskId = verificationJobInstanceService.create(accountId, newVerificationJobInstanceDTO());
    VerificationJobInstance verificationJobInstance =
        verificationJobInstanceService.getVerificationJobInstance(verificationTaskId);
    verificationJobInstance.setResolvedJob(job);
    verificationJobInstanceService.createDataCollectionTasks(verificationJobInstance);
    Map<String, String> params = new HashMap<>();
    params.put(DataCollectionTaskKeys.dataCollectionWorkerId,
        UUID.nameUUIDFromBytes((verificationJobInstance.getUuid() + ":" + connectorId).getBytes(Charsets.UTF_8))
            .toString());
    params.put(CVConfigKeys.connectorIdentifier, connectorId);
    verify(verificationManagerService)
        .createDataCollectionTask(
            eq(accountId), eq(orgIdentifier), eq(projectIdentifier), any(DataCollectionConnectorBundle.class));
    VerificationJobInstance saved = verificationJobInstanceService.getVerificationJobInstance(verificationTaskId);
    assertThat(saved.getPerpetualTaskIds()).isEqualTo(Lists.newArrayList(perpetualTaskId));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void createDataCollectionTasks_validateDataCollectionTasksCreation() {
    VerificationJob job = newCanaryVerificationJobDTO().getVerificationJob();
    job.setAccountId(accountId);
    job.setIdentifier(verificationJobIdentifier);
    hPersistence.save(job);
    cvConfigService.save(newCVConfig());
    String verificationJobInstanceId =
        verificationJobInstanceService.create(accountId, newVerificationJobInstanceDTO());
    VerificationJobInstance verificationJobInstance =
        verificationJobInstanceService.getVerificationJobInstance(verificationJobInstanceId);
    verificationJobInstance.setResolvedJob(job);
    verificationJobInstanceService.createDataCollectionTasks(verificationJobInstance);
    String workerId = getDataCollectionWorkerId(verificationJobInstance.getUuid(), connectorId);
    DataCollectionTask firstTask = dataCollectionTaskService.getNextTask(accountId, workerId).get();
    assertThat(firstTask).isNotNull();
    assertThat(firstTask.getStartTime()).isEqualTo(Instant.parse("2020-07-27T10:29:00Z"));
    assertThat(firstTask.getEndTime()).isEqualTo(Instant.parse("2020-07-27T10:44:00Z"));
    assertThat(firstTask.getValidAfter()).isEqualTo(Instant.parse("2020-07-27T10:44:00Z").plus(Duration.ofMinutes(5)));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreateDataCollectionTasks_validateDataCollectionTasksCreationWithDefaultDataCollectionDelay() {
    VerificationJob job = newCanaryVerificationJobDTO().getVerificationJob();
    job.setAccountId(accountId);
    job.setIdentifier(verificationJobIdentifier);
    hPersistence.save(job);
    cvConfigService.save(newCVConfig());
    VerificationJobInstanceDTO dto = VerificationJobInstanceDTO.builder()
                                         .verificationJobIdentifier(verificationJobIdentifier)
                                         .deploymentStartTimeMs(deploymentStartTimeMs)
                                         .verificationTaskStartTimeMs(deploymentStartTimeMs)
                                         .build();
    String verificationJobInstanceId = verificationJobInstanceService.create(accountId, dto);
    VerificationJobInstance verificationJobInstance =
        verificationJobInstanceService.getVerificationJobInstance(verificationJobInstanceId);
    verificationJobInstance.setResolvedJob(job);
    verificationJobInstanceService.createDataCollectionTasks(verificationJobInstance);
    VerificationJobInstance updated =
        verificationJobInstanceService.getVerificationJobInstance(verificationJobInstanceId);
    String workerId = getDataCollectionWorkerId(verificationJobInstanceId, connectorId);
    DataCollectionTask firstTask = dataCollectionTaskService.getNextTask(accountId, workerId).get();
    assertThat(firstTask).isNotNull();
    assertThat(firstTask.getEndTime()).isEqualTo(Instant.parse("2020-07-27T10:44:00Z"));
    assertThat(firstTask.getValidAfter()).isEqualTo(Instant.parse("2020-07-27T10:44:00Z").plus(DATA_COLLECTION_DELAY));
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
    AnalysisProgressLog progressLog = AnalysisProgressLog.builder()
                                          .startTime(verificationJobInstance.getStartTime())
                                          .endTime(verificationJobInstance.getStartTime().plus(Duration.ofMinutes(1)))
                                          .analysisStatus(AnalysisStatus.SUCCESS)
                                          .log("time series analysis done")
                                          .build();
    verificationJobInstanceService.logProgress(verificationJobInstanceId, progressLog);
    verificationJobInstance = verificationJobInstanceService.getVerificationJobInstance(verificationJobInstanceId);
    assertThat(verificationJobInstance.getProgressLogs()).hasSize(1);
    assertThat(((AnalysisProgressLog) verificationJobInstance.getProgressLogs().get(0)).getAnalysisStatus())
        .isEqualTo(AnalysisStatus.SUCCESS);
    assertThat(verificationJobInstance.getProgressLogs().get(0).getLog()).isEqualTo("time series analysis done");

    assertThat(verificationJobInstance.getExecutionStatus()).isEqualTo(ExecutionStatus.QUEUED);
    progressLog = AnalysisProgressLog.builder()
                      .startTime(verificationJobInstance.getEndTime().minus(Duration.ofMinutes(1)))
                      .endTime(verificationJobInstance.getEndTime())
                      .analysisStatus(AnalysisStatus.SUCCESS)
                      .isFinalState(false)
                      .build();
    verificationJobInstanceService.logProgress(verificationJobInstanceId, progressLog);
    verificationJobInstance = verificationJobInstanceService.getVerificationJobInstance(verificationJobInstanceId);
    assertThat(verificationJobInstance.getProgressLogs()).hasSize(2);
    assertThat(((AnalysisProgressLog) verificationJobInstance.getProgressLogs().get(1)).getAnalysisStatus())
        .isEqualTo(AnalysisStatus.SUCCESS);
    assertThat(verificationJobInstance.getExecutionStatus()).isEqualTo(ExecutionStatus.QUEUED);

    progressLog = AnalysisProgressLog.builder()
                      .startTime(verificationJobInstance.getEndTime().minus(Duration.ofMinutes(1)))
                      .endTime(verificationJobInstance.getEndTime())
                      .analysisStatus(AnalysisStatus.SUCCESS)
                      .isFinalState(true)
                      .build();
    verificationJobInstanceService.logProgress(verificationJobInstanceId, progressLog);
    verificationJobInstance = verificationJobInstanceService.getVerificationJobInstance(verificationJobInstanceId);
    assertThat(verificationJobInstance.getProgressLogs()).hasSize(3);
    assertThat(((AnalysisProgressLog) verificationJobInstance.getProgressLogs().get(2)).getAnalysisStatus())
        .isEqualTo(AnalysisStatus.SUCCESS);
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
    AnalysisProgressLog progressLog = AnalysisProgressLog.builder()
                                          .startTime(verificationJobInstance.getStartTime())
                                          .endTime(verificationJobInstance.getStartTime().plus(Duration.ofMinutes(1)))
                                          .analysisStatus(AnalysisStatus.FAILED)
                                          .build();
    verificationJobInstanceService.logProgress(verificationJobInstanceId, progressLog);
    verificationJobInstance = verificationJobInstanceService.getVerificationJobInstance(verificationJobInstanceId);
    assertThat(verificationJobInstance.getProgressLogs()).hasSize(1);
    assertThat(((AnalysisProgressLog) verificationJobInstance.getProgressLogs().get(0)).getAnalysisStatus())
        .isEqualTo(AnalysisStatus.FAILED);
    assertThat(verificationJobInstance.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testDeleteDataCollectionWorkers_whenSuccessful() {
    verificationJobService.upsert(accountId, newCanaryVerificationJobDTO());
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
    verificationJobService.upsert(accountId, newCanaryVerificationJobDTO());
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
    VerificationJobInstance verificationJobInstance = createVerificationJobInstance(verificationJobIdentifier, "prod");
    DeploymentActivityVerificationResultDTO deploymentActivityVerificationResultDTO =
        verificationJobInstanceService.getAggregatedVerificationResult(
            Collections.singletonList(verificationJobInstance.getUuid()));
    assertThat(deploymentActivityVerificationResultDTO).isNotNull();
    assertThat(deploymentActivityVerificationResultDTO.getPreProductionDeploymentSummary()).isNull();
    assertThat(deploymentActivityVerificationResultDTO.getPostDeploymentSummary()).isNull();
    assertThat(deploymentActivityVerificationResultDTO.getProductionDeploymentSummary())
        .isEqualTo(ActivityVerificationSummary.builder()
                       .total(1)
                       .startTime(verificationJobInstance.getStartTime().toEpochMilli())
                       .riskScore(-1.0)
                       .notStarted(1)
                       .durationMs(Duration.ofMinutes(15).toMillis())
                       .remainingTimeMs(1200000)
                       .build());
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testSetDeploymentJobSummaries() {
    VerificationJobInstance devVerificationJobInstance =
        createVerificationJobInstance("devVerificationJobInstance", "dev");
    VerificationJobInstance prodVerificationJobInstance =
        createVerificationJobInstance("prodVerificationJobInstance", "prod");

    DeploymentResultSummary deploymentResultSummary =
        DeploymentResultSummary.builder()
            .preProductionDeploymentVerificationJobInstanceSummaries(new ArrayList<>())
            .productionDeploymentVerificationJobInstanceSummaries(new ArrayList<>())
            .postDeploymentVerificationJobInstanceSummaries(new ArrayList<>())
            .build();

    verificationJobInstanceService.addResultsToDeploymentResultSummary(accountId,
        Arrays.asList(devVerificationJobInstance.getUuid(), prodVerificationJobInstance.getUuid()),
        deploymentResultSummary);

    assertThat(deploymentResultSummary.getPreProductionDeploymentVerificationJobInstanceSummaries().size())
        .isEqualTo(1);
    assertThat(deploymentResultSummary.getPreProductionDeploymentVerificationJobInstanceSummaries()
                   .get(0)
                   .getEnvironmentName())
        .isEqualTo("Harness dev");
    assertThat(deploymentResultSummary.getProductionDeploymentVerificationJobInstanceSummaries().size()).isEqualTo(1);
    assertThat(
        deploymentResultSummary.getProductionDeploymentVerificationJobInstanceSummaries().get(0).getEnvironmentName())
        .isEqualTo("Harness prod");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetDeploymentVerificationPopoverResult_verificationJobInstanceDoesNotExist() {
    String verificationJobInstanceId = generateUuid();
    assertThatThrownBy(()
                           -> verificationJobInstanceService.getDeploymentVerificationPopoverResult(
                               Arrays.asList(verificationJobInstanceId)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No VerificationJobInstance found with IDs " + Arrays.asList(verificationJobInstanceId).toString());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetDeploymentVerificationPopoverResult_validVerificationJobInstancesWithFailedInstance() {
    VerificationJobInstance devVerificationJobInstance =
        createVerificationJobInstance("devVerificationJobInstance", "dev");
    VerificationJobInstance prodVerificationJobInstance =
        createVerificationJobInstance("prodVerificationJobInstance", "prod");
    verificationJobInstanceService.logProgress(prodVerificationJobInstance.getUuid(),
        AnalysisProgressLog.builder()
            .analysisStatus(AnalysisStatus.FAILED)
            .startTime(prodVerificationJobInstance.getStartTime())
            .endTime(prodVerificationJobInstance.getStartTime().plus(Duration.ofMinutes(1)))
            .isFinalState(true)
            .log("Time series")
            .build());
    DeploymentActivityPopoverResultDTO deploymentActivityPopoverResultDTO =
        verificationJobInstanceService.getDeploymentVerificationPopoverResult(
            Arrays.asList(devVerificationJobInstance.getUuid(), prodVerificationJobInstance.getUuid()));
    assertThat(deploymentActivityPopoverResultDTO.getPreProductionDeploymentSummary())
        .isEqualTo(DeploymentPopoverSummary.builder()
                       .total(1)
                       .verificationResults(Collections.singletonList(
                           DeploymentActivityPopoverResultDTO.VerificationResult.builder()
                               .jobName(devVerificationJobInstance.getResolvedJob().getJobName())
                               .progressPercentage(0)
                               .remainingTimeMs(1200000L)
                               .status(ActivityVerificationStatus.NOT_STARTED)
                               .startTime(devVerificationJobInstance.getStartTime().toEpochMilli())
                               .build()))
                       .build());
    assertThat(deploymentActivityPopoverResultDTO.getProductionDeploymentSummary())
        .isEqualTo(DeploymentPopoverSummary.builder()
                       .total(1)
                       .verificationResults(Collections.singletonList(
                           DeploymentActivityPopoverResultDTO.VerificationResult.builder()
                               .jobName(prodVerificationJobInstance.getResolvedJob().getJobName())
                               .progressPercentage(6)
                               .remainingTimeMs(0L)
                               .status(ActivityVerificationStatus.ERROR)
                               .startTime(prodVerificationJobInstance.getStartTime().toEpochMilli())
                               .build()))
                       .build());
    assertThat(deploymentActivityPopoverResultDTO.getPostDeploymentSummary()).isEqualTo(null);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetDeploymentVerificationPopoverResult_validVerificationJobInstancesWithInProgressInstance() {
    VerificationJobInstance devVerificationJobInstance =
        createVerificationJobInstance("devVerificationJobInstance", "dev", ExecutionStatus.RUNNING, CANARY);
    VerificationJobInstance prodVerificationJobInstance =
        createVerificationJobInstance("prodVerificationJobInstance", "prod", ExecutionStatus.RUNNING, CANARY);
    verificationJobInstanceService.logProgress(prodVerificationJobInstance.getUuid(),
        AnalysisProgressLog.builder()
            .analysisStatus(AnalysisStatus.SUCCESS)
            .startTime(prodVerificationJobInstance.getStartTime())
            .endTime(prodVerificationJobInstance.getStartTime().plus(Duration.ofMinutes(1)))
            .isFinalState(true)
            .log("Time series")
            .build());
    DeploymentActivityPopoverResultDTO deploymentActivityPopoverResultDTO =
        verificationJobInstanceService.getDeploymentVerificationPopoverResult(
            Arrays.asList(devVerificationJobInstance.getUuid(), prodVerificationJobInstance.getUuid()));
    assertThat(deploymentActivityPopoverResultDTO.getPreProductionDeploymentSummary())
        .isEqualTo(DeploymentPopoverSummary.builder()
                       .total(1)
                       .verificationResults(Collections.singletonList(
                           DeploymentActivityPopoverResultDTO.VerificationResult.builder()
                               .jobName(devVerificationJobInstance.getResolvedJob().getJobName())
                               .progressPercentage(0)
                               .remainingTimeMs(1200000L)
                               .status(ActivityVerificationStatus.IN_PROGRESS)
                               .startTime(devVerificationJobInstance.getStartTime().toEpochMilli())
                               .build()))
                       .build());
    assertThat(deploymentActivityPopoverResultDTO.getProductionDeploymentSummary())
        .isEqualTo(DeploymentPopoverSummary.builder()
                       .total(1)
                       .verificationResults(Collections.singletonList(
                           DeploymentActivityPopoverResultDTO.VerificationResult.builder()
                               .jobName(prodVerificationJobInstance.getResolvedJob().getJobName())
                               .progressPercentage(6)
                               .remainingTimeMs(3666000L)
                               .status(ActivityVerificationStatus.IN_PROGRESS)
                               .startTime(prodVerificationJobInstance.getStartTime().toEpochMilli())
                               .build()))
                       .build());
    assertThat(deploymentActivityPopoverResultDTO.getPostDeploymentSummary()).isEqualTo(null);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetTestJobBaselineExecutions_noOldInstanceExist() {
    assertThat(verificationJobInstanceService.getTestJobBaselineExecutions(
                   accountId, orgIdentifier, projectIdentifier, generateUuid()))
        .isEmpty();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetTestJobBaselineExecutions_multipleOldSuccessfulInstances() {
    String verificationJobIdentifier = generateUuid();
    List<ExecutionStatus> executionStatuses =
        Arrays.asList(ExecutionStatus.QUEUED, ExecutionStatus.SUCCESS, ExecutionStatus.FAILED, ExecutionStatus.SUCCESS);
    List<VerificationJobInstance> verificationJobInstances = new ArrayList<>();
    for (ExecutionStatus executionStatus : executionStatuses) {
      verificationJobInstances.add(
          createVerificationJobInstance(verificationJobIdentifier, generateUuid(), executionStatus, TEST));
    }
    verificationJobInstances.add(
        createVerificationJobInstance(verificationJobIdentifier, generateUuid(), ExecutionStatus.SUCCESS, CANARY));
    verificationJobInstances.add(
        createVerificationJobInstance(generateUuid(), generateUuid(), ExecutionStatus.SUCCESS, TEST));
    List<TestVerificationBaselineExecutionDTO> testVerificationBaselineExecutionDTOS =
        verificationJobInstanceService.getTestJobBaselineExecutions(
            accountId, orgIdentifier, projectIdentifier, verificationJobIdentifier);
    assertThat(testVerificationBaselineExecutionDTOS).hasSize(2);
    assertThat(testVerificationBaselineExecutionDTOS.get(0))
        .isEqualTo(TestVerificationBaselineExecutionDTO.builder()
                       .verificationJobInstanceId(verificationJobInstances.get(1).getUuid())
                       .createdAt(verificationJobInstances.get(1).getCreatedAt())
                       .build());
    assertThat(testVerificationBaselineExecutionDTOS.get(1))
        .isEqualTo(TestVerificationBaselineExecutionDTO.builder()
                       .verificationJobInstanceId(verificationJobInstances.get(3).getUuid())
                       .createdAt(verificationJobInstances.get(3).getCreatedAt())
                       .build());
  }
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetLastSuccessfulTestVerificationJobExecutionId_doesNotExist() {
    assertThat(verificationJobInstanceService.getLastSuccessfulTestVerificationJobExecutionId(
                   accountId, orgIdentifier, projectIdentifier, generateUuid()))
        .isEmpty();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetLastSuccessfulTestVerificationJobExecutionId_lastSuccessfulTestVerificationJobInstance() {
    String verificationJobIdentifier = generateUuid();
    List<ExecutionStatus> executionStatuses =
        Arrays.asList(ExecutionStatus.QUEUED, ExecutionStatus.SUCCESS, ExecutionStatus.FAILED);
    List<VerificationJobInstance> verificationJobInstances = new ArrayList<>();
    for (ExecutionStatus executionStatus : executionStatuses) {
      verificationJobInstances.add(
          createVerificationJobInstance(verificationJobIdentifier, generateUuid(), executionStatus, TEST));
    }
    verificationJobInstances.add(
        createVerificationJobInstance(verificationJobIdentifier, generateUuid(), ExecutionStatus.SUCCESS, CANARY));
    verificationJobInstances.add(
        createVerificationJobInstance(generateUuid(), generateUuid(), ExecutionStatus.SUCCESS, TEST));
    assertThat(verificationJobInstanceService
                   .getLastSuccessfulTestVerificationJobExecutionId(
                       accountId, orgIdentifier, projectIdentifier, verificationJobIdentifier)
                   .get())
        .isEqualTo(verificationJobInstances.get(1).getUuid());
  }
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetDeploymentVerificationJobInstanceSummary() {
    VerificationJobInstance devVerificationJobInstance =
        createVerificationJobInstance("devVerificationJobInstance", "dev");
    VerificationJobInstance prodVerificationJobInstance =
        createVerificationJobInstance("prodVerificationJobInstance", "prod");
    DeploymentActivityResultDTO.DeploymentVerificationJobInstanceSummary deploymentVerificationJobInstanceSummary =
        verificationJobInstanceService.getDeploymentVerificationJobInstanceSummary(
            Lists.newArrayList(devVerificationJobInstance.getUuid(), prodVerificationJobInstance.getUuid()));
    assertThat(deploymentVerificationJobInstanceSummary.getEnvironmentName()).isEqualTo("Harness dev");
    assertThat(deploymentVerificationJobInstanceSummary.getVerificationJobInstanceId())
        .isEqualTo(devVerificationJobInstance.getUuid());
    assertThat(deploymentVerificationJobInstanceSummary.getActivityId()).isNull();
    assertThat(deploymentVerificationJobInstanceSummary.getActivityStartTime()).isZero();
    assertThat(deploymentVerificationJobInstanceSummary.getJobName())
        .isEqualTo(devVerificationJobInstance.getResolvedJob().getJobName());
    assertThat(deploymentVerificationJobInstanceSummary.getProgressPercentage()).isEqualTo(0);
    assertThat(deploymentVerificationJobInstanceSummary.getRiskScore()).isNull();
    assertThat(deploymentVerificationJobInstanceSummary.getStatus()).isEqualTo(ActivityVerificationStatus.NOT_STARTED);
    assertThat(deploymentVerificationJobInstanceSummary.getAdditionalInfo())
        .isEqualTo(CanaryDeploymentAdditionalInfo.builder()
                       .primary(Collections.emptySet())
                       .canary(Collections.emptySet())
                       .build());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testFilterRunningVerificationJobInstances() {
    VerificationJobInstance verificationJobInstance1 =
        createVerificationJobInstance("devVerificationJobInstance", "dev", ExecutionStatus.RUNNING, CANARY);
    VerificationJobInstance verificationJobInstance2 =
        createVerificationJobInstance("prodVerificationJobInstance1", "prod", ExecutionStatus.QUEUED, CANARY);
    VerificationJobInstance verificationJobInstance3 =
        createVerificationJobInstance("prodVerificationJobInstance2", "prod", ExecutionStatus.FAILED, CANARY);
    List<VerificationJobInstance> filteredList = verificationJobInstanceService.filterRunningVerificationJobInstances(
        Arrays.asList(verificationJobInstance1.getUuid(), verificationJobInstance2.getUuid(),
            verificationJobInstance3.getUuid()));
    assertThat(filteredList).hasSize(1);
    assertThat(filteredList.get(0).getUuid()).isEqualTo(verificationJobInstance1.getUuid());
    filteredList = verificationJobInstanceService.filterRunningVerificationJobInstances(
        Arrays.asList(verificationJobInstance2.getUuid(), verificationJobInstance3.getUuid()));
    assertThat(filteredList).isEmpty();
    filteredList = verificationJobInstanceService.filterRunningVerificationJobInstances(Collections.emptyList());
    assertThat(filteredList).isEmpty();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testResetPerpetualTask() {
    CVConfig cvConfig = cvConfigService.save(newCVConfig());
    cvConfigId = cvConfig.getUuid();
    VerificationJobInstance verificationJobInstance =
        createVerificationJobInstance("devVerificationJobInstance", "dev", ExecutionStatus.RUNNING, CANARY);
    String perpetualTaskId = generateUuid();
    HashMap<String, String> connectorToPerpetualTaskIdMap = new HashMap<>();
    connectorToPerpetualTaskIdMap.put(cvConfig.getConnectorIdentifier(), perpetualTaskId);
    verificationJobInstance.setConnectorsToPerpetualTaskIdsMap(connectorToPerpetualTaskIdMap);
    verificationJobInstanceService.resetPerpetualTask(verificationJobInstance, cvConfig);
    ArgumentCaptor<DataCollectionConnectorBundle> captor = ArgumentCaptor.forClass(DataCollectionConnectorBundle.class);
    verify(verificationManagerService, times(1))
        .resetDataCollectionTask(
            eq(accountId), eq(orgIdentifier), eq(projectIdentifier), eq(perpetualTaskId), captor.capture());
    DataCollectionConnectorBundle dataCollectionConnectorBundle = captor.getValue();
    Map<String, String> params = new HashMap<>();
    params.put(DataCollectionTaskKeys.dataCollectionWorkerId,
        getDataCollectionWorkerId(verificationJobInstance.getUuid(), cvConfig.getConnectorIdentifier()));
    params.put(CVConfigKeys.connectorIdentifier, cvConfig.getConnectorIdentifier());
    assertThat(dataCollectionConnectorBundle.getParams()).isEqualTo(params);
    assertThat(dataCollectionConnectorBundle.getDataCollectionType()).isEqualTo(DataCollectionType.CV);
  }
  private String getDataCollectionWorkerId(String verificationJobInstanceId, String connectorId) {
    return UUID.nameUUIDFromBytes((verificationJobInstanceId + ":" + connectorId).getBytes(Charsets.UTF_8)).toString();
  }
  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testCreate_whenHealthJob() {
    Instant now = Instant.now();
    int numOfJobInstances = 10;
    List<VerificationJobInstance> verificationJobInstances = new ArrayList<>();
    for (int i = 0; i < numOfJobInstances; i++) {
      verificationJobInstances.add(
          VerificationJobInstance.builder()
              .accountId(accountId)
              .startTime(now.plusSeconds(i))
              .executionStatus(ExecutionStatus.QUEUED)
              .verificationJobIdentifier("job-" + i)
              .preActivityVerificationStartTime(now.minusSeconds(i * 60))
              .postActivityVerificationStartTime(now.plusSeconds(i * 60))
              .resolvedJob(
                  HealthVerificationJob.builder()
                      .accountId(accountId)
                      .orgIdentifier(orgIdentifier)
                      .projectIdentifier(projectIdentifier)
                      .identifier("job-" + i)
                      .envIdentifier(
                          RuntimeParameter.builder().isRuntimeParam(false).value(i % 2 == 0 ? "e0" : "e1").build())
                      .serviceIdentifier(
                          RuntimeParameter.builder().isRuntimeParam(false).value(i % 2 == 0 ? "s0" : "s1").build())
                      .build())
              .build());
    }

    List<String> jobIds = verificationJobInstanceService.create(verificationJobInstances);
    assertThat(jobIds.size()).isEqualTo(2);
    verificationJobInstances = hPersistence.createQuery(VerificationJobInstance.class, excludeAuthority).asList();
    Collections.sort(
        verificationJobInstances, Comparator.comparing(VerificationJobInstance::getVerificationJobIdentifier));
    for (int i = 0; i < 2; i++) {
      VerificationJobInstance verificationJobInstance = verificationJobInstances.get(i);
      assertThat(verificationJobInstance.getAccountId()).isEqualTo(accountId);
      assertThat(verificationJobInstance.getExecutionStatus()).isEqualTo(ExecutionStatus.QUEUED);
      assertThat(verificationJobInstance.getVerificationJobIdentifier()).isEqualTo("job-" + i);
      assertThat(verificationJobInstance.getStartTime()).isEqualTo(now.plusSeconds(i));
      assertThat(verificationJobInstance.getPreActivityVerificationStartTime()).isEqualTo(now.minusSeconds(i * 60));
      assertThat(verificationJobInstance.getPostActivityVerificationStartTime()).isEqualTo(now.plusSeconds(i * 60));

      HealthVerificationJob resolvedJob = (HealthVerificationJob) verificationJobInstance.getResolvedJob();
      assertThat(resolvedJob.getIdentifier()).isEqualTo("job-" + i);
      assertThat(resolvedJob.getAccountId()).isEqualTo(accountId);
      assertThat(resolvedJob.getOrgIdentifier()).isEqualTo(orgIdentifier);
      assertThat(resolvedJob.getProjectIdentifier()).isEqualTo(projectIdentifier);
      assertThat(resolvedJob.getEnvIdentifier()).isEqualTo("e" + i);
      assertThat(resolvedJob.getServiceIdentifier()).isEqualTo("s" + i);
    }
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testCreate_whenNonHealthJob() {
    Instant now = Instant.now();
    int numOfJobInstances = 10;
    List<VerificationJobInstance> verificationJobInstances = new ArrayList<>();
    for (int i = 0; i < numOfJobInstances; i++) {
      verificationJobInstances.add(
          VerificationJobInstance.builder()
              .uuid("id-" + i)
              .accountId(accountId)
              .startTime(now.plusSeconds(i))
              .executionStatus(ExecutionStatus.QUEUED)
              .verificationJobIdentifier("job-" + i)
              .preActivityVerificationStartTime(now.minusSeconds(i * 60))
              .postActivityVerificationStartTime(now.plusSeconds(i * 60))
              .resolvedJob(
                  TestVerificationJob.builder()
                      .accountId(accountId)
                      .orgIdentifier(orgIdentifier)
                      .projectIdentifier(projectIdentifier)
                      .identifier("job-" + i)
                      .envIdentifier(RuntimeParameter.builder().isRuntimeParam(false).value("e1").build())
                      .serviceIdentifier(RuntimeParameter.builder().isRuntimeParam(false).value("s1").build())
                      .duration(RuntimeParameter.builder().isRuntimeParam(false).value("10m").build())
                      .sensitivity(
                          RuntimeParameter.builder().isRuntimeParam(false).value(Sensitivity.LOW.name()).build())
                      .build())
              .build());
    }

    List<String> jobIds = verificationJobInstanceService.create(verificationJobInstances);
    assertThat(jobIds.size()).isEqualTo(numOfJobInstances);
    Collections.sort(jobIds);
    for (int i = 0; i < 2; i++) {
      VerificationJobInstance verificationJobInstance = hPersistence.get(VerificationJobInstance.class, jobIds.get(i));
      assertThat(verificationJobInstance.getAccountId()).isEqualTo(accountId);
      assertThat(verificationJobInstance.getExecutionStatus()).isEqualTo(ExecutionStatus.QUEUED);
      assertThat(verificationJobInstance.getVerificationJobIdentifier()).isEqualTo("job-" + i);
      assertThat(verificationJobInstance.getStartTime()).isEqualTo(now.plusSeconds(i));
      assertThat(verificationJobInstance.getPreActivityVerificationStartTime()).isEqualTo(now.minusSeconds(i * 60));
      assertThat(verificationJobInstance.getPostActivityVerificationStartTime()).isEqualTo(now.plusSeconds(i * 60));

      TestVerificationJob resolvedJob = (TestVerificationJob) verificationJobInstance.getResolvedJob();
      assertThat(resolvedJob.getIdentifier()).isEqualTo("job-" + i);
      assertThat(resolvedJob.getAccountId()).isEqualTo(accountId);
      assertThat(resolvedJob.getOrgIdentifier()).isEqualTo(orgIdentifier);
      assertThat(resolvedJob.getProjectIdentifier()).isEqualTo(projectIdentifier);
      assertThat(resolvedJob.getEnvIdentifier()).isEqualTo("e1");
      assertThat(resolvedJob.getServiceIdentifier()).isEqualTo("s1");
    }
  }

  private VerificationJobDTO newCanaryVerificationJobDTO() {
    CanaryVerificationJobDTO canaryVerificationJobDTO = new CanaryVerificationJobDTO();
    canaryVerificationJobDTO.setIdentifier(verificationJobIdentifier);
    canaryVerificationJobDTO.setJobName(generateUuid());
    canaryVerificationJobDTO.setDataSources(Lists.newArrayList(DataSourceType.SPLUNK));
    canaryVerificationJobDTO.setSensitivity(Sensitivity.MEDIUM.name());
    canaryVerificationJobDTO.setServiceIdentifier("service");
    canaryVerificationJobDTO.setOrgIdentifier(orgIdentifier);
    canaryVerificationJobDTO.setProjectIdentifier(projectIdentifier);
    canaryVerificationJobDTO.setEnvIdentifier("env");
    canaryVerificationJobDTO.setSensitivity(Sensitivity.MEDIUM.name());
    canaryVerificationJobDTO.setDuration("15m");
    return canaryVerificationJobDTO;
  }

  private VerificationJobDTO newHealthVerificationJobDTO() {
    HealthVerificationJobDTO healthVerificationJobDTO = new HealthVerificationJobDTO();
    healthVerificationJobDTO.setIdentifier(verificationJobIdentifier);
    healthVerificationJobDTO.setJobName(generateUuid());
    healthVerificationJobDTO.setDataSources(Lists.newArrayList(DataSourceType.SPLUNK));
    healthVerificationJobDTO.setServiceIdentifier("service");
    healthVerificationJobDTO.setOrgIdentifier(orgIdentifier);
    healthVerificationJobDTO.setProjectIdentifier(projectIdentifier);
    healthVerificationJobDTO.setEnvIdentifier("env");
    healthVerificationJobDTO.setDuration("15m");
    return healthVerificationJobDTO;
  }

  private CVConfig newCVConfig() {
    SplunkCVConfig cvConfig = new SplunkCVConfig();
    cvConfig.setQuery("exception");
    cvConfig.setServiceInstanceIdentifier("serviceInstanceIdentifier");
    cvConfig.setVerificationType(VerificationType.LOG);
    cvConfig.setAccountId(accountId);
    cvConfig.setConnectorIdentifier(connectorId);
    cvConfig.setServiceIdentifier("service");
    cvConfig.setEnvIdentifier("env");
    cvConfig.setProjectIdentifier(projectIdentifier);
    cvConfig.setOrgIdentifier(orgIdentifier);
    cvConfig.setIdentifier("groupId");
    cvConfig.setMonitoringSourceName(generateUuid());
    cvConfig.setCategory(CVMonitoringCategory.PERFORMANCE);
    cvConfig.setProductName("productName");
    return cvConfig;
  }
  private VerificationJobInstance createVerificationJobInstance() {
    verificationJobService.upsert(accountId, newCanaryVerificationJobDTO());
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

  private VerificationJobInstance createVerificationJobInstance(String verificationJobIdentifier, String envIdentifier,
      ExecutionStatus executionStatus, VerificationJobType verificationJobType) {
    verificationJobService.upsert(
        accountId, createVerificationJobDTO(verificationJobIdentifier, envIdentifier, verificationJobType));
    VerificationJob verificationJob = verificationJobService.getVerificationJob(accountId, verificationJobIdentifier);
    VerificationJobInstance verificationJobInstance =
        VerificationJobInstance.builder()
            .accountId(accountId)
            .executionStatus(executionStatus)
            .verificationJobIdentifier(verificationJobIdentifier)
            .deploymentStartTime(Instant.ofEpochMilli(deploymentStartTimeMs))
            .resolvedJob(verificationJob)
            .createdAt(deploymentStartTimeMs + Duration.ofMinutes(2).toMillis())
            .startTime(Instant.ofEpochMilli(deploymentStartTimeMs + Duration.ofMinutes(2).toMillis()))
            .build();
    verificationJobInstanceService.create(verificationJobInstance);
    verificationTaskService.create(accountId, cvConfigId, verificationJobInstance.getUuid());
    return verificationJobInstance;
  }
  private VerificationJobInstance createVerificationJobInstance(
      String verificationJobIdentifier, String envIdentifier) {
    return createVerificationJobInstance(verificationJobIdentifier, envIdentifier, ExecutionStatus.QUEUED, CANARY);
  }
  private VerificationJobDTO newTestVerificationJobDTO(String verificationJobIdentifier, String envIdentifier) {
    TestVerificationJobDTO verificationJobDTO = new TestVerificationJobDTO();
    fillCommonFields(verificationJobIdentifier, envIdentifier, verificationJobDTO);
    verificationJobDTO.setBaselineVerificationJobInstanceId(generateUuid());
    verificationJobDTO.setSensitivity(Sensitivity.MEDIUM.name());

    return verificationJobDTO;
  }

  private void fillCommonFields(
      String verificationJobIdentifier, String envIdentifier, VerificationJobDTO verificationJobDTO) {
    verificationJobDTO.setIdentifier(verificationJobIdentifier);
    verificationJobDTO.setJobName(generateUuid());
    verificationJobDTO.setDataSources(Lists.newArrayList(DataSourceType.SPLUNK));
    verificationJobDTO.setServiceIdentifier(serviceIdentifier);
    verificationJobDTO.setOrgIdentifier(orgIdentifier);
    verificationJobDTO.setProjectIdentifier(projectIdentifier);
    verificationJobDTO.setEnvIdentifier(envIdentifier);
    verificationJobDTO.setDuration("15m");
  }

  private VerificationJobDTO createVerificationJobDTO(
      String verificationJobIdentifier, String envIdentifier, VerificationJobType verificationJobType) {
    switch (verificationJobType) {
      case TEST:
        return newTestVerificationJobDTO(verificationJobIdentifier, envIdentifier);
      case CANARY:
        return newCanaryVerificationJobDTO(verificationJobIdentifier, envIdentifier);
      default:
        throw new IllegalStateException("Not implemented: " + verificationJobType);
    }
  }

  private VerificationJobDTO newCanaryVerificationJobDTO(String verificationJobIdentifier, String envIdentifier) {
    CanaryVerificationJobDTO verificationJobDTO = new CanaryVerificationJobDTO();
    fillCommonFields(verificationJobIdentifier, envIdentifier, verificationJobDTO);
    verificationJobDTO.setSensitivity(Sensitivity.MEDIUM.name());
    return verificationJobDTO;
  }
}
