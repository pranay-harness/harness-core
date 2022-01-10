/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.activity.services.impl;

import static io.harness.cvng.verificationjob.CVVerificationJobConstants.ENV_IDENTIFIER_KEY;
import static io.harness.cvng.verificationjob.CVVerificationJobConstants.JOB_IDENTIFIER_KEY;
import static io.harness.cvng.verificationjob.CVVerificationJobConstants.SERVICE_IDENTIFIER_KEY;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.eraro.ErrorCode.FAILED_TO_ACQUIRE_PERSISTENT_LOCK;
import static io.harness.exception.WingsException.SRE;
import static io.harness.rule.OwnerRule.ABHIJITH;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.KANHAIYA;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static io.harness.rule.OwnerRule.RAGHU;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.activity.beans.ActivityVerificationResultDTO;
import io.harness.cvng.activity.beans.ActivityVerificationResultDTO.CategoryRisk;
import io.harness.cvng.activity.beans.ActivityVerificationSummary;
import io.harness.cvng.activity.beans.DeploymentActivityResultDTO;
import io.harness.cvng.activity.beans.DeploymentActivitySummaryDTO;
import io.harness.cvng.activity.entities.Activity;
import io.harness.cvng.activity.entities.Activity.ActivityKeys;
import io.harness.cvng.activity.entities.DeploymentActivity;
import io.harness.cvng.activity.entities.KubernetesActivity;
import io.harness.cvng.activity.services.api.ActivityService;
import io.harness.cvng.analysis.beans.DeploymentTimeSeriesAnalysisDTO;
import io.harness.cvng.analysis.beans.Risk;
import io.harness.cvng.analysis.entities.DeploymentTimeSeriesAnalysis;
import io.harness.cvng.analysis.entities.HealthVerificationPeriod;
import io.harness.cvng.analysis.services.api.DeploymentTimeSeriesAnalysisService;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.activity.ActivityDTO.VerificationJobRuntimeDetails;
import io.harness.cvng.beans.activity.ActivityStatusDTO;
import io.harness.cvng.beans.activity.ActivityType;
import io.harness.cvng.beans.activity.ActivityVerificationStatus;
import io.harness.cvng.beans.activity.DeploymentActivityDTO;
import io.harness.cvng.beans.job.Sensitivity;
import io.harness.cvng.beans.job.VerificationJobType;
import io.harness.cvng.cdng.entities.CVNGStepTask;
import io.harness.cvng.cdng.services.api.CVNGStepTaskService;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.HealthSourceDTO;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.dashboard.services.api.HealthVerificationHeatMapService;
import io.harness.cvng.verificationjob.entities.CanaryVerificationJob;
import io.harness.cvng.verificationjob.entities.HealthVerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob.RuntimeParameter;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.ExecutionStatus;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.exception.PersistentLockException;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.ng.core.service.dto.ServiceResponseDTO;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ActivityServiceImplTest extends CvNextGenTestBase {
  @Inject private HPersistence hPersistence;
  @Inject private ActivityService activityService;
  @Inject private VerificationJobService realVerificationJobService;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private VerificationJobInstanceService realVerificationJobInstanceService;
  @Inject private DeploymentTimeSeriesAnalysisService deploymentTimeSeriesAnalysisService;
  @Inject private CVNGStepTaskService cvngStepTaskService;
  @Mock private VerificationJobService verificationJobService;
  @Mock private VerificationJobInstanceService verificationJobInstanceService;
  @Mock private HealthVerificationHeatMapService healthVerificationHeatMapService;
  @Mock private NextGenService nextGenService;
  @Mock private PersistentLocker mockedPersistentLocker;

  private String projectIdentifier;
  private String orgIdentifier;
  private String accountId;
  private Instant instant;
  private String serviceIdentifier;
  private String envIdentifier;
  private String deploymentTag;
  private BuilderFactory builderFactory;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    builderFactory = BuilderFactory.getDefault();
    instant = Instant.parse("2020-07-27T10:44:06.390Z");
    projectIdentifier = builderFactory.getContext().getProjectIdentifier();
    orgIdentifier = builderFactory.getContext().getOrgIdentifier();
    accountId = builderFactory.getContext().getAccountId();
    serviceIdentifier = builderFactory.getContext().getServiceIdentifier();
    envIdentifier = builderFactory.getContext().getEnvIdentifier();
    deploymentTag = "build#1";

    FieldUtils.writeField(activityService, "verificationJobService", verificationJobService, true);
    FieldUtils.writeField(activityService, "verificationJobInstanceService", verificationJobInstanceService, true);
    FieldUtils.writeField(activityService, "nextGenService", nextGenService, true);
    FieldUtils.writeField(activityService, "healthVerificationHeatMapService", healthVerificationHeatMapService, true);
    when(nextGenService.getService(any(), any(), any(), any()))
        .thenReturn(ServiceResponseDTO.builder().name("service name").build());
    when(verificationJobInstanceService.getCVConfigsForVerificationJob(any()))
        .thenReturn(Lists.newArrayList(new AppDynamicsCVConfig()));
    realVerificationJobService.createDefaultVerificationJobs(accountId, orgIdentifier, projectIdentifier);
    FieldUtils.writeField(
        activityService, "deploymentTimeSeriesAnalysisService", deploymentTimeSeriesAnalysisService, true);
    FieldUtils.writeField(deploymentTimeSeriesAnalysisService, "nextGenService", nextGenService, true);
    when(nextGenService.get(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Optional.of(ConnectorInfoDTO.builder().name("AppDynamics Connector").build()));
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetActivity() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    when(verificationJobInstanceService.create(anyList())).thenReturn(Arrays.asList("taskId1"));

    activityService.register(accountId, getDeploymentActivity(verificationJob));

    Activity activity = hPersistence.createQuery(Activity.class)
                            .filter(ActivityKeys.projectIdentifier, projectIdentifier)
                            .filter(ActivityKeys.orgIdentifier, orgIdentifier)
                            .get();

    String id = activity.getUuid();

    Activity fromDb = activityService.get(id);

    assertThat(activity.getUuid()).isEqualTo(fromDb.getUuid());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetByVerificationJobInstanceId() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    when(verificationJobInstanceService.create(anyList())).thenReturn(Arrays.asList("taskId1"));

    activityService.register(accountId, getDeploymentActivity(verificationJob));

    Activity activity = hPersistence.createQuery(Activity.class)
                            .filter(ActivityKeys.projectIdentifier, projectIdentifier)
                            .filter(ActivityKeys.orgIdentifier, orgIdentifier)
                            .get();

    Activity fromDb = activityService.getByVerificationJobInstanceId("taskId1");

    assertThat(activity.getUuid()).isEqualTo(fromDb.getUuid());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetActivityVerificationResult() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    activityService.register(accountId, getDeploymentActivity(verificationJob));

    Activity activity = hPersistence.createQuery(Activity.class)
                            .filter(ActivityKeys.projectIdentifier, projectIdentifier)
                            .filter(ActivityKeys.orgIdentifier, orgIdentifier)
                            .get();

    String id = activity.getUuid();
    ActivityVerificationSummary summary = createActivitySummary(Instant.now());
    when(verificationJobInstanceService.getActivityVerificationSummary(anyList())).thenReturn(summary);
    Set<CategoryRisk> preActivityRisks = new HashSet<>();
    preActivityRisks.add(CategoryRisk.builder().category(CVMonitoringCategory.PERFORMANCE).risk(0.2).build());

    Set<CategoryRisk> postActivityRisks = new HashSet<>();
    postActivityRisks.add(CategoryRisk.builder().category(CVMonitoringCategory.PERFORMANCE).risk(0.7).build());

    when(healthVerificationHeatMapService.getAggregatedRisk(id, HealthVerificationPeriod.PRE_ACTIVITY))
        .thenReturn(preActivityRisks);

    when(healthVerificationHeatMapService.getAggregatedRisk(id, HealthVerificationPeriod.POST_ACTIVITY))
        .thenReturn(postActivityRisks);

    ActivityVerificationResultDTO resultDTO = activityService.getActivityVerificationResult(accountId, id);
    assertThat(resultDTO).isNotNull();
    assertThat(resultDTO.getActivityId()).isEqualTo(id);
    assertThat(resultDTO.getActivityType().name()).isEqualTo(activity.getType().name());
    assertThat(resultDTO.getOverallRisk()).isEqualTo(0);
    assertThat(resultDTO.getProgressPercentage()).isEqualTo(summary.getProgressPercentage());

    verify(healthVerificationHeatMapService).getAggregatedRisk(id, HealthVerificationPeriod.PRE_ACTIVITY);
    verify(healthVerificationHeatMapService).getAggregatedRisk(id, HealthVerificationPeriod.POST_ACTIVITY);
    verify(verificationJobInstanceService, times(1)).getActivityVerificationSummary(anyList());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetActivityVerificationResult_validateOverallRisk() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    activityService.register(accountId, getDeploymentActivity(verificationJob));

    Activity activity = hPersistence.createQuery(Activity.class)
                            .filter(ActivityKeys.projectIdentifier, projectIdentifier)
                            .filter(ActivityKeys.orgIdentifier, orgIdentifier)
                            .get();

    String id = activity.getUuid();
    ActivityVerificationSummary summary = createActivitySummary(Instant.now());
    when(verificationJobInstanceService.getActivityVerificationSummary(anyList())).thenReturn(summary);
    Set<CategoryRisk> preActivityRisks = new HashSet<>();
    preActivityRisks.add(CategoryRisk.builder().category(CVMonitoringCategory.PERFORMANCE).risk(0.2).build());
    preActivityRisks.add(CategoryRisk.builder().category(CVMonitoringCategory.ERRORS).risk(91.0).build());

    Set<CategoryRisk> postActivityRisks = new HashSet<>();
    postActivityRisks.add(CategoryRisk.builder().category(CVMonitoringCategory.PERFORMANCE).risk(23.0).build());
    postActivityRisks.add(CategoryRisk.builder().category(CVMonitoringCategory.ERRORS).risk(34.0).build());

    when(healthVerificationHeatMapService.getAggregatedRisk(id, HealthVerificationPeriod.PRE_ACTIVITY))
        .thenReturn(preActivityRisks);

    when(healthVerificationHeatMapService.getAggregatedRisk(id, HealthVerificationPeriod.POST_ACTIVITY))
        .thenReturn(postActivityRisks);

    ActivityVerificationResultDTO resultDTO = activityService.getActivityVerificationResult(accountId, id);
    assertThat(resultDTO).isNotNull();
    assertThat(resultDTO.getActivityId()).isEqualTo(id);
    assertThat(resultDTO.getActivityType().name()).isEqualTo(activity.getType().name());

    // overall risk should be max of post deployment risks
    assertThat(resultDTO.getOverallRisk()).isEqualTo(34);
    assertThat(resultDTO.getProgressPercentage()).isEqualTo(summary.getProgressPercentage());

    verify(healthVerificationHeatMapService).getAggregatedRisk(id, HealthVerificationPeriod.PRE_ACTIVITY);
    verify(healthVerificationHeatMapService).getAggregatedRisk(id, HealthVerificationPeriod.POST_ACTIVITY);
    verify(verificationJobInstanceService, times(1)).getActivityVerificationSummary(anyList());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetActivityVerificationResult_noRisks() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    activityService.register(accountId, getDeploymentActivity(verificationJob));

    Activity activity = hPersistence.createQuery(Activity.class)
                            .filter(ActivityKeys.projectIdentifier, projectIdentifier)
                            .filter(ActivityKeys.orgIdentifier, orgIdentifier)
                            .get();

    String id = activity.getUuid();
    ActivityVerificationSummary summary = createActivitySummary(Instant.now());
    when(verificationJobInstanceService.getActivityVerificationSummary(anyList())).thenReturn(summary);
    Set<CategoryRisk> preActivityRisks = new HashSet<>();

    Set<CategoryRisk> postActivityRisks = new HashSet<>();

    when(healthVerificationHeatMapService.getAggregatedRisk(id, HealthVerificationPeriod.PRE_ACTIVITY))
        .thenReturn(preActivityRisks);

    when(healthVerificationHeatMapService.getAggregatedRisk(id, HealthVerificationPeriod.POST_ACTIVITY))
        .thenReturn(postActivityRisks);

    ActivityVerificationResultDTO resultDTO = activityService.getActivityVerificationResult(accountId, id);
    assertThat(resultDTO).isNotNull();
    assertThat(resultDTO.getActivityId()).isEqualTo(id);
    assertThat(resultDTO.getActivityType().name()).isEqualTo(activity.getType().name());

    // overall risk should be max of post deployment risks
    assertThat(resultDTO.getOverallRisk()).isEqualTo(-1);
    assertThat(resultDTO.getProgressPercentage()).isEqualTo(summary.getProgressPercentage());

    verify(healthVerificationHeatMapService).getAggregatedRisk(id, HealthVerificationPeriod.PRE_ACTIVITY);
    verify(healthVerificationHeatMapService).getAggregatedRisk(id, HealthVerificationPeriod.POST_ACTIVITY);
    verify(verificationJobInstanceService, times(1)).getActivityVerificationSummary(anyList());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetActivityVerificationResult_noSummary() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    activityService.register(accountId, getDeploymentActivity(verificationJob));

    Activity activity = hPersistence.createQuery(Activity.class)
                            .filter(ActivityKeys.projectIdentifier, projectIdentifier)
                            .filter(ActivityKeys.orgIdentifier, orgIdentifier)
                            .get();

    String id = activity.getUuid();
    ActivityVerificationSummary summary = createActivitySummary(Instant.now());
    when(verificationJobInstanceService.getActivityVerificationSummary(anyList())).thenReturn(null);
    Set<CategoryRisk> preActivityRisks = new HashSet<>();
    preActivityRisks.add(CategoryRisk.builder().category(CVMonitoringCategory.PERFORMANCE).risk(20.0).build());

    Set<CategoryRisk> postActivityRisks = new HashSet<>();
    postActivityRisks.add(CategoryRisk.builder().category(CVMonitoringCategory.PERFORMANCE).risk(70.0).build());

    when(healthVerificationHeatMapService.getAggregatedRisk(id, HealthVerificationPeriod.PRE_ACTIVITY))
        .thenReturn(preActivityRisks);

    when(healthVerificationHeatMapService.getAggregatedRisk(id, HealthVerificationPeriod.POST_ACTIVITY))
        .thenReturn(postActivityRisks);

    ActivityVerificationResultDTO resultDTO = activityService.getActivityVerificationResult(accountId, id);
    assertThat(resultDTO).isNull();

    verify(healthVerificationHeatMapService, never()).getAggregatedRisk(id, HealthVerificationPeriod.PRE_ACTIVITY);
    verify(healthVerificationHeatMapService, never()).getAggregatedRisk(id, HealthVerificationPeriod.POST_ACTIVITY);
    verify(verificationJobInstanceService, times(1)).getActivityVerificationSummary(anyList());
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testUpsert_createEntityIsEqual() {
    useMockedPersistentLocker();
    Activity activity = builderFactory.getDeploymentActivityBuilder().build();
    String activityUuid = activityService.upsert(activity);
    Activity activityFromDb = activityService.get(activityUuid);
    assertThat(activityFromDb).isEqualTo(activity);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testUpsert_updateEntity() {
    useMockedPersistentLocker();
    Activity existingActivity = builderFactory.getDeploymentActivityBuilder().build();
    Activity updatingActivity = builderFactory.getDeploymentActivityBuilder().build();
    List<String> verificationJobInstanceIds = new ArrayList<>();
    verificationJobInstanceIds.addAll(existingActivity.getVerificationJobInstanceIds());
    verificationJobInstanceIds.addAll(updatingActivity.getVerificationJobInstanceIds());
    ActivityVerificationSummary summary = createActivitySummary(Instant.now());
    summary.setTotal(2);
    summary.setPassed(2);
    summary.setProgress(0);

    List<VerificationJobInstance> jobInstances = Arrays.asList(builderFactory.verificationJobInstanceBuilder()
                                                                   .deploymentStartTime(Instant.now())
                                                                   .startTime(Instant.now())
                                                                   .uuid(verificationJobInstanceIds.get(0))
                                                                   .build(),
        builderFactory.verificationJobInstanceBuilder()
            .deploymentStartTime(Instant.now())
            .startTime(Instant.now())
            .uuid(verificationJobInstanceIds.get(1))
            .build());
    when(verificationJobInstanceService.get(existingActivity.getVerificationJobInstanceIds())).thenReturn(jobInstances);
    when(verificationJobInstanceService.getActivityVerificationSummary(eq(jobInstances))).thenReturn(summary);

    activityService.upsert(existingActivity);
    String uuid = activityService.upsert(updatingActivity);

    Activity activityFromDb = activityService.get(uuid);
    assertThat(activityFromDb.getVerificationJobInstanceIds()).isEqualTo(verificationJobInstanceIds);
    assertThat(activityFromDb.getVerificationSummary()).isEqualTo(summary);
    assertThat(activityFromDb.getActivityName()).isEqualTo(updatingActivity.getActivityName());
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testUpsert_whenEntityLocked() {
    useMockedPersistentLocker();
    when(mockedPersistentLocker.waitToAcquireLock(any(), any(), any(), any()))
        .thenThrow(new PersistentLockException("Lock not acquired", FAILED_TO_ACQUIRE_PERSISTENT_LOCK, SRE));
    Activity activity = builderFactory.getDeploymentActivityBuilder().build();
    assertThatThrownBy(() -> activityService.upsert(activity)).isInstanceOf(PersistentLockException.class);
    assertThat(activity.getUuid()).isNull();
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testUpsert_lockClose() {
    useMockedPersistentLocker();
    AcquiredLock acquiredLock = mock(AcquiredLock.class);
    when(mockedPersistentLocker.waitToAcquireLock(any(), any(), any(), any())).thenReturn(acquiredLock);
    activityService.upsert(builderFactory.getDeploymentActivityBuilder().build());
    verify(acquiredLock).close();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetDeploymentSummary() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    DeploymentActivityDTO deploymentActivityDTO = getDeploymentActivity(verificationJob);
    VerificationJobInstance verificationJobInstance = createVerificationJobInstance();
    CVConfig cvConfig = verificationJobInstance.getCvConfigMap().values().iterator().next();
    String verificationJobInstanceId = realVerificationJobInstanceService.create(verificationJobInstance);
    String verificationTaskId = verificationTaskService.createDeploymentVerificationTask(
        accountId, cvConfig.getUuid(), verificationJobInstanceId, cvConfig.getType());

    deploymentTimeSeriesAnalysisService.save(createDeploymentTimeSeriesAnalysis(verificationTaskId));

    String activityId = activityService.register(accountId, deploymentActivityDTO);
    Activity activity = hPersistence.get(Activity.class, activityId);
    activity.setVerificationJobInstanceIds(Arrays.asList(verificationJobInstanceId));
    hPersistence.save(activity);

    DeploymentActivityResultDTO.DeploymentVerificationJobInstanceSummary deploymentVerificationJobInstanceSummary =
        DeploymentActivityResultDTO.DeploymentVerificationJobInstanceSummary.builder()
            .environmentName("env name")
            .build();
    when(verificationJobInstanceService.getDeploymentVerificationJobInstanceSummary(anyList()))
        .thenReturn(deploymentVerificationJobInstanceSummary);
    assertThat(deploymentVerificationJobInstanceSummary.getActivityId()).isNull();
    DeploymentActivitySummaryDTO deploymentActivitySummaryDTO = activityService.getDeploymentSummary(activityId);
    assertThat(deploymentActivitySummaryDTO.getServiceIdentifier())
        .isEqualTo(deploymentActivityDTO.getServiceIdentifier());
    assertThat(deploymentActivitySummaryDTO.getDeploymentTag()).isEqualTo(deploymentActivityDTO.getDeploymentTag());
    assertThat(deploymentActivitySummaryDTO.getEnvIdentifier())
        .isEqualTo(deploymentActivityDTO.getEnvironmentIdentifier());
    assertThat(deploymentActivitySummaryDTO.getServiceName()).isEqualTo("service name");
    assertThat(deploymentActivitySummaryDTO.getEnvName()).isEqualTo("env name");
    assertThat(deploymentVerificationJobInstanceSummary.getActivityId()).isEqualTo(activityId);
    assertThat(deploymentVerificationJobInstanceSummary.getActivityStartTime())
        .isEqualTo(deploymentActivityDTO.getActivityStartTime());
    assertThat(deploymentVerificationJobInstanceSummary.getTimeSeriesAnalysisSummary()).isNotNull();
    assertThat(deploymentVerificationJobInstanceSummary.getTimeSeriesAnalysisSummary().getTotalNumMetrics())
        .isEqualTo(2);
    assertThat(deploymentVerificationJobInstanceSummary.getTimeSeriesAnalysisSummary().getNumAnomMetrics())
        .isEqualTo(1);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetActivityStatus() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    DeploymentActivityDTO deploymentActivityDTO = getDeploymentActivity(verificationJob);
    String activityId = activityService.register(accountId, deploymentActivityDTO);
    DeploymentActivityResultDTO.DeploymentVerificationJobInstanceSummary deploymentVerificationJobInstanceSummary =
        DeploymentActivityResultDTO.DeploymentVerificationJobInstanceSummary.builder()
            .durationMs(verificationJob.getDuration().toMillis())
            .status(ActivityVerificationStatus.NOT_STARTED)
            .build();
    when(verificationJobInstanceService.getDeploymentVerificationJobInstanceSummary(anyList()))
        .thenReturn(deploymentVerificationJobInstanceSummary);
    assertThat(deploymentVerificationJobInstanceSummary.getActivityId()).isNull();
    ActivityStatusDTO activityStatusDTO = activityService.getActivityStatus(accountId, activityId);
    assertThat(activityStatusDTO.getActivityId()).isEqualTo(activityId);
    assertThat(activityStatusDTO.getDurationMs()).isEqualTo(verificationJob.getDuration().toMillis());
    assertThat(activityStatusDTO.getStatus()).isEqualTo(ActivityVerificationStatus.NOT_STARTED);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category({UnitTests.class})
  public void testCreateVerificationJobInstancesForActivity_defaultJob() throws IllegalAccessException {
    FieldUtils.writeField(activityService, "verificationJobService", realVerificationJobService, true);
    realVerificationJobInstanceService = spy(realVerificationJobInstanceService);
    doReturn(Lists.newArrayList(new AppDynamicsCVConfig()))
        .when(realVerificationJobInstanceService)
        .getCVConfigsForVerificationJob(any());

    FieldUtils.writeField(activityService, "verificationJobInstanceService", realVerificationJobInstanceService, true);

    KubernetesActivity kubernetesActivity = getKubernetesActivity();
    kubernetesActivity.setVerificationJobRuntimeDetails(null);

    assertThat(activityService.createVerificationJobInstancesForActivity(kubernetesActivity)).isNotEmpty();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testCreateVerificationJobInstancesForActivity_whenHealthJob() throws IllegalAccessException {
    FieldUtils.writeField(activityService, "verificationJobService", realVerificationJobService, true);
    realVerificationJobInstanceService = spy(realVerificationJobInstanceService);
    doReturn(Lists.newArrayList(new AppDynamicsCVConfig()))
        .when(realVerificationJobInstanceService)
        .getCVConfigsForVerificationJob(any());
    FieldUtils.writeField(activityService, "verificationJobInstanceService", realVerificationJobInstanceService, true);
    KubernetesActivity kubernetesActivity = getKubernetesActivity();
    realVerificationJobService.save(HealthVerificationJob.builder()
                                        .accountId(accountId)
                                        .jobName("job-name")
                                        .orgIdentifier(kubernetesActivity.getOrgIdentifier())
                                        .projectIdentifier(kubernetesActivity.getProjectIdentifier())
                                        .envIdentifier(RuntimeParameter.builder()
                                                           .isRuntimeParam(false)
                                                           .value(kubernetesActivity.getEnvironmentIdentifier())
                                                           .build())
                                        .serviceIdentifier(RuntimeParameter.builder()
                                                               .isRuntimeParam(false)
                                                               .value(kubernetesActivity.getServiceIdentifier())
                                                               .build())
                                        .duration(RuntimeParameter.builder().isRuntimeParam(false).value("30m").build())
                                        .dataSources(Lists.newArrayList(DataSourceType.APP_DYNAMICS))
                                        .type(VerificationJobType.HEALTH)
                                        .identifier(generateUuid())
                                        .build());
    kubernetesActivity.setVerificationJobRuntimeDetails(null);
    assertThat(activityService.createVerificationJobInstancesForActivity(kubernetesActivity).size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testCreateVerificationJobInstancesForActivity_whenHealthJobRunning() throws IllegalAccessException {
    FieldUtils.writeField(activityService, "verificationJobService", realVerificationJobService, true);
    FieldUtils.writeField(activityService, "verificationJobInstanceService", realVerificationJobInstanceService, true);
    KubernetesActivity kubernetesActivity = getKubernetesActivity();
    HealthVerificationJob healthVerificationJob =
        HealthVerificationJob.builder()
            .accountId(accountId)
            .jobName("job-name")
            .orgIdentifier(kubernetesActivity.getOrgIdentifier())
            .projectIdentifier(kubernetesActivity.getProjectIdentifier())
            .identifier(generateUuid())
            .envIdentifier(RuntimeParameter.builder()
                               .isRuntimeParam(false)
                               .value(kubernetesActivity.getEnvironmentIdentifier())
                               .build())
            .serviceIdentifier(RuntimeParameter.builder()
                                   .isRuntimeParam(false)
                                   .value(kubernetesActivity.getServiceIdentifier())
                                   .build())
            .duration(RuntimeParameter.builder().isRuntimeParam(false).value("30m").build())
            .dataSources(Lists.newArrayList(DataSourceType.APP_DYNAMICS))
            .type(VerificationJobType.HEALTH)
            .build();
    realVerificationJobService.save(healthVerificationJob);
    realVerificationJobInstanceService.create(builderFactory.verificationJobInstanceBuilder()
                                                  .accountId(kubernetesActivity.getAccountId())
                                                  .deploymentStartTime(Instant.now())
                                                  .startTime(Instant.now())
                                                  .resolvedJob(healthVerificationJob)
                                                  .executionStatus(ExecutionStatus.RUNNING)
                                                  .startTime(Instant.now())
                                                  .build());
    assertThat(activityService.createVerificationJobInstancesForActivity(kubernetesActivity)).isNull();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testCreateVerificationJobInstancesForActivity_whenHealthJobSuccess() throws IllegalAccessException {
    FieldUtils.writeField(activityService, "verificationJobService", realVerificationJobService, true);
    realVerificationJobInstanceService = spy(realVerificationJobInstanceService);
    doReturn(Lists.newArrayList(new AppDynamicsCVConfig()))
        .when(realVerificationJobInstanceService)
        .getCVConfigsForVerificationJob(any());
    FieldUtils.writeField(activityService, "verificationJobInstanceService", realVerificationJobInstanceService, true);
    KubernetesActivity kubernetesActivity = getKubernetesActivity();
    HealthVerificationJob healthVerificationJob =
        HealthVerificationJob.builder()
            .accountId(accountId)
            .jobName("job-name")
            .orgIdentifier(kubernetesActivity.getOrgIdentifier())
            .projectIdentifier(kubernetesActivity.getProjectIdentifier())
            .envIdentifier(RuntimeParameter.builder()
                               .isRuntimeParam(false)
                               .value(kubernetesActivity.getEnvironmentIdentifier())
                               .build())
            .serviceIdentifier(RuntimeParameter.builder()
                                   .isRuntimeParam(false)
                                   .value(kubernetesActivity.getServiceIdentifier())
                                   .build())
            .duration(RuntimeParameter.builder().isRuntimeParam(false).value("30m").build())
            .dataSources(Lists.newArrayList(DataSourceType.APP_DYNAMICS))
            .type(VerificationJobType.HEALTH)
            .identifier(generateUuid())
            .build();
    realVerificationJobService.save(healthVerificationJob);
    realVerificationJobInstanceService.create(builderFactory.verificationJobInstanceBuilder()
                                                  .accountId(kubernetesActivity.getAccountId())
                                                  .resolvedJob(healthVerificationJob)
                                                  .executionStatus(ExecutionStatus.SUCCESS)
                                                  .build());
    assertThat(activityService.createVerificationJobInstancesForActivity(kubernetesActivity)).hasSize(1);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testUpdateActivityStatus_passed() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    instant = Instant.now();

    activityService.register(accountId, getDeploymentActivity(verificationJob));
    activityService.register(accountId, getDeploymentActivity(verificationJob));

    List<Activity> activities = hPersistence.createQuery(Activity.class).asList();
    activities.get(0).setVerificationJobInstanceIds(Arrays.asList("jobId1"));
    activities.get(1).setVerificationJobInstanceIds(Arrays.asList("jobId2"));

    assertThat(activities.get(0).getAnalysisStatus().name()).isEqualTo(ActivityVerificationStatus.NOT_STARTED.name());
    assertThat(activities.get(0).getVerificationSummary()).isNull();

    ActivityVerificationSummary summary = createActivitySummary(Instant.now());
    summary.setTotal(1);
    summary.setPassed(1);
    summary.setProgress(0);

    List<VerificationJobInstance> jobInstances1 = Arrays.asList(builderFactory.verificationJobInstanceBuilder()
                                                                    .deploymentStartTime(Instant.now())
                                                                    .startTime(Instant.now())
                                                                    .uuid("jobId1")
                                                                    .build());
    when(verificationJobInstanceService.get(Arrays.asList("jobId1"))).thenReturn(jobInstances1);

    when(verificationJobInstanceService.getActivityVerificationSummary(jobInstances1)).thenReturn(summary);
    // when(verificationJobInstanceService.getActivityVerificationSummary(jobInstances1)).thenReturn(createActivitySummary(Instant.now()));

    activityService.updateActivityStatus(activities.get(0));

    activities = hPersistence.createQuery(Activity.class).asList();

    assertThat(activities.get(0).getAnalysisStatus().name())
        .isEqualTo(ActivityVerificationStatus.VERIFICATION_PASSED.name());
    assertThat(activities.get(0).getVerificationSummary()).isNotNull();
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testAbort_inNotStarted() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    instant = Instant.now();
    String activityId = activityService.register(accountId, getDeploymentActivity(verificationJob));
    Activity activity = activityService.get(activityId);
    List<String> verificationJobs = Lists.newArrayList("JOB_INSTANCE_ID");
    activity.setVerificationJobInstanceIds(verificationJobs);
    hPersistence.save(activity);

    activityService.abort(activityId);

    Activity updatedActivity = activityService.get(activityId);
    verify(verificationJobInstanceService).abort(Lists.newArrayList(updatedActivity.getVerificationJobInstanceIds()));
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testAbort_inError() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    instant = Instant.now();
    String activityId = activityService.register(accountId, getDeploymentActivity(verificationJob));
    Activity activity = activityService.get(activityId);
    activity.setAnalysisStatus(ActivityVerificationStatus.ERROR);
    hPersistence.save(activity);

    activityService.abort(activityId);

    Activity updatedActivity = activityService.get(activityId);
    // assert that errored activity is not aborted
    assertThat(updatedActivity.getAnalysisStatus()).isEqualTo(ActivityVerificationStatus.ERROR);
    verify(verificationJobInstanceService, never()).abort(any());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testUpdateActivityStatus_inProgress() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    instant = Instant.now();

    activityService.register(accountId, getDeploymentActivity(verificationJob));
    activityService.register(accountId, getDeploymentActivity(verificationJob));

    List<Activity> activities = hPersistence.createQuery(Activity.class).asList();
    activities.get(0).setVerificationJobInstanceIds(Arrays.asList("jobId1"));
    activities.get(1).setVerificationJobInstanceIds(Arrays.asList("jobId2"));

    assertThat(activities.get(0).getAnalysisStatus().name()).isEqualTo(ActivityVerificationStatus.NOT_STARTED.name());
    assertThat(activities.get(0).getVerificationSummary()).isNull();

    ActivityVerificationSummary summary = createActivitySummary(Instant.now());
    summary.setTotal(1);
    summary.setPassed(1);
    summary.setProgress(0);

    List<VerificationJobInstance> jobInstances1 = Arrays.asList(builderFactory.verificationJobInstanceBuilder()
                                                                    .deploymentStartTime(Instant.now())
                                                                    .startTime(Instant.now())
                                                                    .uuid("jobId1")
                                                                    .build());
    when(verificationJobInstanceService.get(Arrays.asList("jobId1"))).thenReturn(jobInstances1);

    when(verificationJobInstanceService.getActivityVerificationSummary(jobInstances1)).thenReturn(summary);
    when(verificationJobInstanceService.getActivityVerificationSummary(anyList()))
        .thenReturn(createActivitySummary(Instant.now()));

    activityService.updateActivityStatus(activities.get(1));

    activities = hPersistence.createQuery(Activity.class).asList();

    assertThat(activities.get(0).getAnalysisStatus().name()).isEqualTo(ActivityVerificationStatus.NOT_STARTED.name());
    assertThat(activities.get(0).getVerificationSummary()).isNull();
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testHealthSources() throws IllegalAccessException {
    String verificationJobInstanceId = generateUuid();
    String cvConfigIdentifier = "nameSpaced/identifier";
    String activityId = "activityId";
    CVConfig cvConfig = builderFactory.appDynamicsCVConfigBuilder().identifier(cvConfigIdentifier).build();
    CVNGStepTask cvngStepTask = builderFactory.cvngStepTaskBuilder()
                                    .accountId(accountId)
                                    .skip(true)
                                    .callbackId(activityId)
                                    .verificationJobInstanceId(verificationJobInstanceId)
                                    .status(CVNGStepTask.Status.IN_PROGRESS)
                                    .build();
    cvngStepTaskService.create(cvngStepTask);
    VerificationJobInstance verificationJobInstance = builderFactory.verificationJobInstanceBuilder()
                                                          .uuid(verificationJobInstanceId)
                                                          .cvConfigMap(new HashMap<String, CVConfig>() {
                                                            { put(cvConfigIdentifier, cvConfig); }
                                                          })
                                                          .build();
    realVerificationJobInstanceService.create(verificationJobInstance);
    FieldUtils.writeField(activityService, "verificationJobInstanceService", realVerificationJobInstanceService, true);
    Set<HealthSourceDTO> healthSourceDTOSet = activityService.healthSources(accountId, activityId);
    assertThat(healthSourceDTOSet.size()).isEqualTo(1);
    assertThat(healthSourceDTOSet.iterator().next().getIdentifier()).isEqualTo(cvConfigIdentifier);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreateActivityForDemo() {
    List<String> verificationJobInstanceIds = Collections.singletonList(generateUuid());
    when(verificationJobInstanceService.createDemoInstances(anyList())).thenReturn(verificationJobInstanceIds);
    VerificationJob verificationJob = createVerificationJob();
    DeploymentActivity deploymentActivity =
        DeploymentActivity.builder()
            .deploymentTag("tag")
            .verificationStartTime(builderFactory.getClock().instant().minus(Duration.ofMinutes(5)).toEpochMilli())
            .build();
    deploymentActivity.setVerificationJobs(Arrays.asList(verificationJob));
    deploymentActivity.setActivityStartTime(builderFactory.getClock().instant().minus(Duration.ofMinutes(10)));
    deploymentActivity.setOrgIdentifier(orgIdentifier);
    deploymentActivity.setAccountId(accountId);
    deploymentActivity.setProjectIdentifier(projectIdentifier);
    deploymentActivity.setServiceIdentifier(serviceIdentifier);
    deploymentActivity.setEnvironmentIdentifier(envIdentifier);
    deploymentActivity.setActivityName("CDNG demo activity");
    deploymentActivity.setType(ActivityType.DEPLOYMENT);
    String activityId =
        activityService.createActivityForDemo(deploymentActivity, ActivityVerificationStatus.VERIFICATION_FAILED);
    Activity activity = activityService.get(activityId);
    assertThat(activity).isNotNull();
    assertThat(activity.getVerificationJobInstanceIds()).isEqualTo(verificationJobInstanceIds);
  }

  private DeploymentActivityDTO getDeploymentActivity(VerificationJob verificationJob) {
    List<VerificationJobRuntimeDetails> verificationJobDetails = new ArrayList<>();
    Map<String, String> runtimeParams = new HashMap<>();
    runtimeParams.put(JOB_IDENTIFIER_KEY, verificationJob.getIdentifier());
    runtimeParams.put(SERVICE_IDENTIFIER_KEY, "cvngService");
    runtimeParams.put(ENV_IDENTIFIER_KEY, "production");
    VerificationJobRuntimeDetails runtimeDetails = VerificationJobRuntimeDetails.builder()
                                                       .verificationJobIdentifier(verificationJob.getIdentifier())
                                                       .runtimeValues(runtimeParams)
                                                       .build();
    verificationJobDetails.add(runtimeDetails);
    DeploymentActivityDTO activityDTO =
        getDeploymentActivityDTO(verificationJobDetails, instant, deploymentTag, envIdentifier, serviceIdentifier);
    return activityDTO;
  }

  private DeploymentActivityDTO getDeploymentActivityDTO(List<VerificationJobRuntimeDetails> verificationJobDetails,
      Instant verificationStartTime, String deploymentTag, String envIdentifier, String serviceIdentifier) {
    DeploymentActivityDTO activityDTO = DeploymentActivityDTO.builder()
                                            .dataCollectionDelayMs(2000l)
                                            .newVersionHosts(new HashSet<>(Arrays.asList("node1", "node2")))
                                            .oldVersionHosts(new HashSet<>(Arrays.asList("node3", "node4")))
                                            .verificationStartTime(verificationStartTime.toEpochMilli())
                                            .deploymentTag(deploymentTag)
                                            .build();
    activityDTO.setAccountIdentifier(accountId);
    activityDTO.setProjectIdentifier(projectIdentifier);
    activityDTO.setOrgIdentifier(orgIdentifier);
    activityDTO.setActivityStartTime(verificationStartTime.toEpochMilli());
    activityDTO.setEnvironmentIdentifier(envIdentifier);
    activityDTO.setName("Build 23 deploy");
    activityDTO.setVerificationJobRuntimeDetails(verificationJobDetails);
    activityDTO.setServiceIdentifier(serviceIdentifier);
    activityDTO.setTags(Arrays.asList("build88", "prod deploy"));
    return activityDTO;
  }

  private KubernetesActivity getKubernetesActivity() {
    KubernetesActivity activity = KubernetesActivity.builder().build();
    activity.setAccountId(accountId);
    activity.setProjectIdentifier(projectIdentifier);
    activity.setOrgIdentifier(orgIdentifier);
    activity.setActivityStartTime(Instant.now());
    activity.setEnvironmentIdentifier(envIdentifier);
    activity.setServiceIdentifier(generateUuid());
    return activity;
  }

  private VerificationJob createVerificationJob() {
    CanaryVerificationJob testVerificationJob = new CanaryVerificationJob();
    testVerificationJob.setUuid(generateUuid());
    testVerificationJob.setAccountId(accountId);
    testVerificationJob.setIdentifier("identifier");
    testVerificationJob.setJobName(generateUuid());
    testVerificationJob.setMonitoringSources(Collections.singletonList("monitoringIdentifier"));
    testVerificationJob.setSensitivity(Sensitivity.MEDIUM);
    testVerificationJob.setServiceIdentifier(serviceIdentifier, false);
    testVerificationJob.setEnvIdentifier(envIdentifier, false);
    testVerificationJob.setDuration(Duration.ofMinutes(5));
    testVerificationJob.setProjectIdentifier(projectIdentifier);
    testVerificationJob.setOrgIdentifier(orgIdentifier);
    return testVerificationJob;
  }

  private ActivityVerificationSummary createActivitySummary(Instant startTime) {
    return ActivityVerificationSummary.builder()
        .total(1)
        .startTime(startTime.toEpochMilli())
        .risk(Risk.OBSERVE)
        .progress(1)
        .notStarted(0)
        .durationMs(Duration.ofMinutes(15).toMillis())
        .remainingTimeMs(1200000)
        .progressPercentage(25)
        .build();
  }

  private VerificationJobInstance createVerificationJobInstance() {
    VerificationJobInstance jobInstance = builderFactory.verificationJobInstanceBuilder().build();
    jobInstance.setAccountId(accountId);
    return jobInstance;
  }

  private DeploymentTimeSeriesAnalysisDTO.HostInfo createHostInfo(
      String hostName, int risk, Double score, boolean primary, boolean canary) {
    return DeploymentTimeSeriesAnalysisDTO.HostInfo.builder()
        .hostName(hostName)
        .risk(risk)
        .score(score)
        .primary(primary)
        .canary(canary)
        .build();
  }
  private DeploymentTimeSeriesAnalysisDTO.HostData createHostData(
      String hostName, int risk, Double score, List<Double> controlData, List<Double> testData) {
    return DeploymentTimeSeriesAnalysisDTO.HostData.builder()
        .hostName(hostName)
        .risk(risk)
        .score(score)
        .controlData(controlData)
        .testData(testData)
        .build();
  }

  private DeploymentTimeSeriesAnalysisDTO.TransactionMetricHostData createTransactionMetricHostData(
      String transactionName, String metricName, int risk, Double score,
      List<DeploymentTimeSeriesAnalysisDTO.HostData> hostDataList) {
    return DeploymentTimeSeriesAnalysisDTO.TransactionMetricHostData.builder()
        .transactionName(transactionName)
        .metricName(metricName)
        .risk(risk)
        .score(score)
        .hostData(hostDataList)
        .build();
  }
  private DeploymentTimeSeriesAnalysis createDeploymentTimeSeriesAnalysis(String verificationTaskId) {
    DeploymentTimeSeriesAnalysisDTO.HostInfo hostInfo1 = createHostInfo("node1", 1, 1.1, false, true);
    DeploymentTimeSeriesAnalysisDTO.HostInfo hostInfo2 = createHostInfo("node2", 2, 2.2, false, true);
    DeploymentTimeSeriesAnalysisDTO.HostInfo hostInfo3 = createHostInfo("node3", 2, 2.2, false, true);
    DeploymentTimeSeriesAnalysisDTO.HostData hostData1 =
        createHostData("node1", 0, 0.0, Arrays.asList(1D), Arrays.asList(1D));
    DeploymentTimeSeriesAnalysisDTO.HostData hostData2 =
        createHostData("node2", 2, 2.0, Arrays.asList(1D), Arrays.asList(1D));

    DeploymentTimeSeriesAnalysisDTO.TransactionMetricHostData transactionMetricHostData1 =
        createTransactionMetricHostData(
            "/todolist/inside", "Errors per Minute", 0, 0.5, Arrays.asList(hostData1, hostData2));

    DeploymentTimeSeriesAnalysisDTO.HostData hostData3 =
        createHostData("node1", 0, 0.0, Arrays.asList(1D), Arrays.asList(1D));
    DeploymentTimeSeriesAnalysisDTO.HostData hostData4 =
        createHostData("node2", 2, 2.0, Arrays.asList(1D), Arrays.asList(1D));
    DeploymentTimeSeriesAnalysisDTO.HostData hostData5 =
        createHostData("node3", 2, 2.0, Arrays.asList(1D), Arrays.asList(1D));

    DeploymentTimeSeriesAnalysisDTO.TransactionMetricHostData transactionMetricHostData2 =
        createTransactionMetricHostData(
            "/todolist/exception", "Calls per Minute", 2, 2.5, Arrays.asList(hostData3, hostData4, hostData5));
    return DeploymentTimeSeriesAnalysis.builder()
        .accountId(accountId)
        .score(.7)
        .risk(Risk.OBSERVE)
        .verificationTaskId(verificationTaskId)
        .transactionMetricSummaries(Arrays.asList(transactionMetricHostData1, transactionMetricHostData2))
        .hostSummaries(Arrays.asList(hostInfo1, hostInfo2, hostInfo3))
        .startTime(Instant.now())
        .endTime(Instant.now().plus(1, ChronoUnit.MINUTES))
        .build();
  }

  @SneakyThrows
  private void useMockedPersistentLocker() {
    FieldUtils.writeField(activityService, "persistentLocker", mockedPersistentLocker, true);
  }
}
