/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.activity.services.impl;

import static io.harness.cvng.activity.CVActivityConstants.HEALTH_VERIFICATION_RETRIGGER_BUFFER_MINS;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.activity.beans.ActivityVerificationResultDTO;
import io.harness.cvng.activity.beans.ActivityVerificationResultDTO.CategoryRisk;
import io.harness.cvng.activity.beans.ActivityVerificationSummary;
import io.harness.cvng.activity.beans.DeploymentActivityResultDTO.DeploymentVerificationJobInstanceSummary;
import io.harness.cvng.activity.beans.DeploymentActivitySummaryDTO;
import io.harness.cvng.activity.entities.Activity;
import io.harness.cvng.activity.entities.Activity.ActivityKeys;
import io.harness.cvng.activity.entities.Activity.ActivityUpdatableEntity;
import io.harness.cvng.activity.entities.DeploymentActivity;
import io.harness.cvng.activity.entities.KubernetesClusterActivity.KubernetesClusterActivityKeys;
import io.harness.cvng.activity.entities.KubernetesClusterActivity.ServiceEnvironment.ServiceEnvironmentKeys;
import io.harness.cvng.activity.services.api.ActivityService;
import io.harness.cvng.activity.services.api.ActivityUpdateHandler;
import io.harness.cvng.analysis.beans.LogAnalysisClusterChartDTO;
import io.harness.cvng.analysis.beans.LogAnalysisClusterDTO;
import io.harness.cvng.analysis.beans.TransactionMetricInfoSummaryPageDTO;
import io.harness.cvng.analysis.entities.HealthVerificationPeriod;
import io.harness.cvng.analysis.services.api.DeploymentLogAnalysisService;
import io.harness.cvng.analysis.services.api.DeploymentTimeSeriesAnalysisService;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.activity.ActivityDTO;
import io.harness.cvng.beans.activity.ActivityStatusDTO;
import io.harness.cvng.beans.activity.ActivityType;
import io.harness.cvng.beans.activity.ActivityVerificationStatus;
import io.harness.cvng.beans.job.VerificationJobType;
import io.harness.cvng.cdng.entities.CVNGStepTask;
import io.harness.cvng.cdng.services.api.CVNGStepTaskService;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.core.beans.DatasourceTypeDTO;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.HealthSourceDTO;
import io.harness.cvng.core.beans.params.PageParams;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.beans.params.filterParams.DeploymentLogAnalysisFilter;
import io.harness.cvng.core.beans.params.filterParams.DeploymentTimeSeriesAnalysisFilter;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.utils.monitoredService.CVConfigToHealthSourceTransformer;
import io.harness.cvng.dashboard.services.api.HealthVerificationHeatMapService;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.ExecutionStatus;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.VerificationJobInstanceBuilder;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.data.structure.EmptyPredicate;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.ng.beans.PageResponse;
import io.harness.persistence.HPersistence;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
@OwnedBy(HarnessTeam.CV)
public class ActivityServiceImpl implements ActivityService {
  private static final int RECENT_DEPLOYMENT_ACTIVITIES_RESULT_SIZE = 5;
  @Inject private HPersistence hPersistence;
  @Inject private VerificationJobInstanceService verificationJobInstanceService;
  @Inject private VerificationJobService verificationJobService;
  @Inject private NextGenService nextGenService;
  @Inject private HealthVerificationHeatMapService healthVerificationHeatMapService;
  @Inject private DeploymentTimeSeriesAnalysisService deploymentTimeSeriesAnalysisService;
  @Inject private DeploymentLogAnalysisService deploymentLogAnalysisService;
  @Inject private Map<DataSourceType, CVConfigToHealthSourceTransformer> dataSourceTypeToHealthSourceTransformerMap;
  @Inject private Map<ActivityType, ActivityUpdatableEntity> activityUpdatableEntityMap;
  @Inject private Map<ActivityType, ActivityUpdateHandler> activityUpdateHandlerMap;
  // TODO: remove the dependency once UI moves to new APIs
  @Inject private CVNGStepTaskService cvngStepTaskService;
  @Inject private PersistentLocker persistentLocker;

  @Override
  public Activity get(String activityId) {
    Preconditions.checkNotNull(activityId, "ActivityID should not be null");
    return hPersistence.get(Activity.class, activityId);
  }

  @Override
  public Activity getByVerificationJobInstanceId(String verificationJobInstanceId) {
    return hPersistence.createQuery(Activity.class, excludeAuthority)
        .field(ActivityKeys.verificationJobInstanceIds)
        .contains(verificationJobInstanceId)
        .get();
  }

  @Override
  public String register(String accountId, ActivityDTO activityDTO) {
    Preconditions.checkNotNull(activityDTO);
    Activity activity = getActivityFromDTO(activityDTO);
    activity.validate();
    activity.setVerificationJobInstanceIds(createVerificationJobInstancesForActivity(activity));
    hPersistence.save(activity);
    log.info("Registered  an activity of type {} for account {}, project {}, org {}", activity.getType(), accountId,
        activity.getProjectIdentifier(), activity.getOrgIdentifier());
    return activity.getUuid();
  }
  @Override
  public String register(Activity activity) {
    activity.validate();
    List<VerificationJobInstance> verificationJobInstances = new ArrayList<>();
    activity.getVerificationJobs().forEach(verificationJob -> {
      VerificationJobInstanceBuilder verificationJobInstanceBuilder = fillOutCommonJobInstanceProperties(
          activity, verificationJob.resolveAdditionsFields(verificationJobInstanceService));
      validateJob(verificationJob);
      activity.fillInVerificationJobInstanceDetails(verificationJobInstanceBuilder);

      verificationJobInstances.add(verificationJobInstanceBuilder.build());
    });
    activity.setVerificationJobInstanceIds(verificationJobInstanceService.create(verificationJobInstances));
    hPersistence.save(activity);
    log.info("Registered  an activity of type {} for account {}, project {}, org {}", activity.getType(),
        activity.getAccountId(), activity.getProjectIdentifier(), activity.getOrgIdentifier());
    return activity.getUuid();
  }

  @Override
  public void updateActivityStatus(Activity activity) {
    if (CollectionUtils.isEmpty(activity.getVerificationJobInstanceIds())) {
      return;
    }
    ActivityVerificationSummary summary = verificationJobInstanceService.getActivityVerificationSummary(
        verificationJobInstanceService.get(activity.getVerificationJobInstanceIds()));
    if (!summary.getAggregatedStatus().equals(ActivityVerificationStatus.IN_PROGRESS)
        && !summary.getAggregatedStatus().equals(ActivityVerificationStatus.NOT_STARTED)) {
      Query<Activity> activityQuery =
          hPersistence.createQuery(Activity.class).filter(ActivityKeys.uuid, activity.getUuid());
      UpdateOperations<Activity> activityUpdateOperations =
          hPersistence.createUpdateOperations(Activity.class)
              .set(ActivityKeys.analysisStatus, summary.getAggregatedStatus())
              .set(ActivityKeys.verificationSummary, summary);
      hPersistence.update(activityQuery, activityUpdateOperations);
      log.info("Updated the status of activity {} to {}", activity.getUuid(), summary.getAggregatedStatus());
    }
  }

  public String createActivity(Activity activity) {
    return hPersistence.save(activity);
  }

  @Override
  public DeploymentActivitySummaryDTO getDeploymentSummary(String activityId) {
    CVNGStepTask cvngStepTask = cvngStepTaskService.getByCallBackId(activityId);
    if (cvngStepTask != null && StringUtils.isNotEmpty(cvngStepTask.getVerificationJobInstanceId())) {
      return cvngStepTaskService.getDeploymentSummary(activityId);
    }
    DeploymentActivity activity = (DeploymentActivity) get(activityId);
    DeploymentVerificationJobInstanceSummary deploymentVerificationJobInstanceSummary =
        getDeploymentVerificationJobInstanceSummary(activity);
    deploymentVerificationJobInstanceSummary.setTimeSeriesAnalysisSummary(
        deploymentTimeSeriesAnalysisService.getAnalysisSummary(activity.getVerificationJobInstanceIds()));
    deploymentVerificationJobInstanceSummary.setLogsAnalysisSummary(deploymentLogAnalysisService.getAnalysisSummary(
        activity.getAccountId(), activity.getVerificationJobInstanceIds()));

    return DeploymentActivitySummaryDTO.builder()
        .deploymentVerificationJobInstanceSummary(deploymentVerificationJobInstanceSummary)
        .serviceIdentifier(activity.getServiceIdentifier())
        .serviceName(getServiceNameFromActivity(activity))
        .envIdentifier(activity.getEnvironmentIdentifier())
        .envName(deploymentVerificationJobInstanceSummary.getEnvironmentName())
        .deploymentTag(activity.getDeploymentTag())
        .build();
  }

  private DeploymentVerificationJobInstanceSummary getDeploymentVerificationJobInstanceSummary(Activity activity) {
    List<String> verificationJobInstanceIds = activity.getVerificationJobInstanceIds();
    DeploymentVerificationJobInstanceSummary deploymentVerificationJobInstanceSummary =
        verificationJobInstanceService.getDeploymentVerificationJobInstanceSummary(verificationJobInstanceIds);
    deploymentVerificationJobInstanceSummary.setActivityId(activity.getUuid());
    deploymentVerificationJobInstanceSummary.setActivityStartTime(activity.getActivityStartTime().toEpochMilli());
    return deploymentVerificationJobInstanceSummary;
  }
  @Override
  public ActivityStatusDTO getActivityStatus(String accountId, String activityId) {
    DeploymentVerificationJobInstanceSummary deploymentVerificationJobInstanceSummary =
        getDeploymentVerificationJobInstanceSummary(get(activityId));
    return ActivityStatusDTO.builder()
        .durationMs(deploymentVerificationJobInstanceSummary.getDurationMs())
        .remainingTimeMs(deploymentVerificationJobInstanceSummary.getRemainingTimeMs())
        .progressPercentage(deploymentVerificationJobInstanceSummary.getProgressPercentage())
        .activityId(activityId)
        .status(deploymentVerificationJobInstanceSummary.getStatus())
        .build();
  }

  @Override
  public String getDeploymentTagFromActivity(String accountId, String verificationJobInstanceId) {
    DeploymentActivity deploymentActivity =
        (DeploymentActivity) hPersistence.createQuery(Activity.class, excludeAuthority)
            .filter(ActivityKeys.accountId, accountId)
            .filter(ActivityKeys.type, ActivityType.DEPLOYMENT)
            .field(ActivityKeys.verificationJobInstanceIds)
            .hasThisOne(verificationJobInstanceId)
            .get();
    if (deploymentActivity != null) {
      return deploymentActivity.getDeploymentTag();
    } else {
      throw new IllegalStateException("Activity not found for verificationJobInstanceId: " + verificationJobInstanceId);
    }
  }

  @Override
  public ActivityVerificationResultDTO getActivityVerificationResult(String accountId, String activityId) {
    Preconditions.checkNotNull(activityId, "ActivityId cannot be null while trying to fetch result");
    return getResultForAnActivity(get(activityId));
  }

  private ActivityVerificationResultDTO getResultForAnActivity(Activity activity) {
    List<VerificationJobInstance> verificationJobInstances =
        verificationJobInstanceService.getHealthVerificationJobInstances(activity.getVerificationJobInstanceIds());

    ActivityVerificationSummary summary =
        verificationJobInstanceService.getActivityVerificationSummary(verificationJobInstances);

    if (summary != null) {
      Set<CategoryRisk> preActivityRisks =
          healthVerificationHeatMapService.getAggregatedRisk(activity.getUuid(), HealthVerificationPeriod.PRE_ACTIVITY);
      Set<CategoryRisk> postActivityRisks = healthVerificationHeatMapService.getAggregatedRisk(
          activity.getUuid(), HealthVerificationPeriod.POST_ACTIVITY);
      List<Double> postActivityValidRisks = new ArrayList<>();
      postActivityRisks.stream()
          .filter(risk -> risk.getRisk() != -1.0)
          .forEach(categoryRisk -> postActivityValidRisks.add(categoryRisk.getRisk()));

      Double overallRisk = postActivityValidRisks.size() == 0 ? -1.0 : Collections.max(postActivityValidRisks);
      return ActivityVerificationResultDTO.builder()
          .preActivityRisks(preActivityRisks)
          .postActivityRisks(postActivityRisks)
          .activityName(activity.getActivityName())
          .activityId(activity.getUuid())
          .activityStartTime(activity.getActivityStartTime().toEpochMilli())
          .activityType(activity.getType())
          .environmentIdentifier(activity.getEnvironmentIdentifier())
          .serviceIdentifier(activity.getServiceIdentifier())
          .progressPercentage(summary.getProgressPercentage())
          .status(summary.getAggregatedStatus())
          .remainingTimeMs(summary.getRemainingTimeMs())
          .endTime(activity.getActivityStartTime().toEpochMilli() + summary.getDurationMs())
          .overallRisk(overallRisk.intValue())
          .build();
    }
    return null;
  }

  @Override
  public List<ActivityVerificationResultDTO> getRecentActivityVerificationResults(
      String accountId, String orgIdentifier, String projectIdentifier, int limitCounter) {
    if (limitCounter == 0) {
      limitCounter = RECENT_DEPLOYMENT_ACTIVITIES_RESULT_SIZE;
    }

    List<Activity> activities = hPersistence.createQuery(Activity.class, excludeAuthority)
                                    .filter(ActivityKeys.accountId, accountId)
                                    .filter(ActivityKeys.orgIdentifier, orgIdentifier)
                                    .filter(ActivityKeys.projectIdentifier, projectIdentifier)
                                    .field(ActivityKeys.type)
                                    .notIn(Arrays.asList(ActivityType.DEPLOYMENT))
                                    .order(Sort.descending(ActivityKeys.activityStartTime))
                                    .field(ActivityKeys.verificationJobInstanceIds)
                                    .exists()
                                    .asList(new FindOptions().limit(limitCounter));

    if (isEmpty(activities)) {
      log.info("No recent activities found for org {}, project {}", orgIdentifier, projectIdentifier);
      return null;
    }

    List<ActivityVerificationResultDTO> activityResultList = new ArrayList<>();
    activities.forEach(activity -> {
      ActivityVerificationResultDTO resultDTO = getResultForAnActivity(activity);
      if (resultDTO != null) {
        activityResultList.add(resultDTO);
      }
    });
    return activityResultList;
  }

  private String getServiceNameFromActivity(Activity activity) {
    return nextGenService
        .getService(activity.getAccountId(), activity.getOrgIdentifier(), activity.getProjectIdentifier(),
            activity.getServiceIdentifier())
        .getName();
  }

  @Override
  public List<String> createVerificationJobInstancesForActivity(Activity activity) {
    List<VerificationJobInstance> jobInstancesToCreate = new ArrayList<>();
    List<VerificationJob> verificationJobs = new ArrayList<>();
    Map<String, Map<String, String>> runtimeDetailsMap = new HashMap<>();
    if (isEmpty(activity.getVerificationJobRuntimeDetails())) {
      // check to see if any other jobs are currently running
      List<VerificationJobInstance> runningInstances = verificationJobInstanceService.getRunningOrQueuedJobInstances(
          activity.getAccountId(), activity.getOrgIdentifier(), activity.getProjectIdentifier(),
          activity.getEnvironmentIdentifier(), activity.getServiceIdentifier(), VerificationJobType.HEALTH,
          activity.getActivityStartTime().plus(Duration.ofMinutes(HEALTH_VERIFICATION_RETRIGGER_BUFFER_MINS)));
      if (isNotEmpty(runningInstances)) {
        log.info(
            "There are verification jobs that are already running for {}, {}, {}. So we will not trigger a new one",
            activity.getProjectIdentifier(), activity.getEnvironmentIdentifier(), activity.getServiceIdentifier());
        return null;
      }
      verificationJobs.add(
          verificationJobService.getResolvedHealthVerificationJob(activity.getAccountId(), activity.getOrgIdentifier(),
              activity.getProjectIdentifier(), activity.getEnvironmentIdentifier(), activity.getServiceIdentifier()));
    } else {
      activity.getVerificationJobRuntimeDetails().forEach(jobDetail -> {
        String jobIdentifier = jobDetail.getVerificationJobIdentifier();
        Preconditions.checkNotNull(jobIdentifier, "Job Identifier must be present in the jobs to trigger");
        VerificationJob verificationJob = verificationJobService.getVerificationJob(
            activity.getAccountId(), activity.getOrgIdentifier(), activity.getProjectIdentifier(), jobIdentifier);
        Preconditions.checkNotNull(verificationJob, "No Job exists for verificationJobIdentifier: '%s'", jobIdentifier);
        verificationJobs.add(verificationJob);
        if (isNotEmpty(jobDetail.getRuntimeValues())) {
          runtimeDetailsMap.put(verificationJob.getIdentifier(), jobDetail.getRuntimeValues());
        }
      });
    }

    verificationJobs.forEach(verificationJob -> {
      if (runtimeDetailsMap.containsKey(verificationJob.getIdentifier())) {
        verificationJob.resolveVerificationJob(runtimeDetailsMap.get(verificationJob.getIdentifier()));
      }
      VerificationJobInstanceBuilder verificationJobInstanceBuilder = fillOutCommonJobInstanceProperties(
          activity, verificationJob.resolveAdditionsFields(verificationJobInstanceService));
      validateJob(verificationJob);
      activity.fillInVerificationJobInstanceDetails(verificationJobInstanceBuilder);

      jobInstancesToCreate.add(verificationJobInstanceBuilder.build());
    });
    Preconditions.checkState(!jobInstancesToCreate.isEmpty(), "Should have at least one VerificationJobInstance");
    if (activity.deduplicateEvents()) {
      return verificationJobInstanceService.dedupCreate(jobInstancesToCreate);
    } else {
      return verificationJobInstanceService.create(jobInstancesToCreate);
    }
  }

  @Override
  public TransactionMetricInfoSummaryPageDTO getDeploymentActivityTimeSeriesData(String accountId, String activityId,
      DeploymentTimeSeriesAnalysisFilter deploymentTimeSeriesAnalysisFilter, PageParams pageParams) {
    List<String> verificationJobInstanceIds = getVerificationJobInstanceId(activityId);
    // TODO: We currently support only one verificationJobInstance per deployment. Hence this check. Revisit if that
    // changes later
    Preconditions.checkState(verificationJobInstanceIds.size() == 1,
        "We do not support more than one monitored source validation from deployment");
    return deploymentTimeSeriesAnalysisService.getMetrics(
        accountId, verificationJobInstanceIds.get(0), deploymentTimeSeriesAnalysisFilter, pageParams);
  }

  @Override
  public Set<DatasourceTypeDTO> getDataSourcetypes(String accountId, String activityId) {
    Preconditions.checkNotNull(accountId);
    Preconditions.checkNotNull(activityId);
    return verificationJobInstanceService.getDataSourcetypes(get(activityId).getVerificationJobInstanceIds());
  }

  @Override
  public List<LogAnalysisClusterChartDTO> getDeploymentActivityLogAnalysisClusters(
      String accountId, String activityId, DeploymentLogAnalysisFilter deploymentLogAnalysisFilter) {
    List<String> verificationJobInstanceIds = getVerificationJobInstanceId(activityId);
    // TODO: We currently support only one verificationJobInstance per deployment. Hence this check. Revisit if that
    // changes later
    Preconditions.checkState(verificationJobInstanceIds.size() == 1,
        "We do not support more than one monitored source validation from deployment");
    return deploymentLogAnalysisService.getLogAnalysisClusters(
        accountId, verificationJobInstanceIds.get(0), deploymentLogAnalysisFilter);
  }

  @Override
  public PageResponse<LogAnalysisClusterDTO> getDeploymentActivityLogAnalysisResult(String accountId, String activityId,
      Integer label, DeploymentLogAnalysisFilter deploymentLogAnalysisFilter, PageParams pageParams) {
    List<String> verificationJobInstanceIds = getVerificationJobInstanceId(activityId);
    // TODO: We currently support only one verificationJobInstance per deployment. Hence this check. Revisit if that
    // changes later
    Preconditions.checkState(verificationJobInstanceIds.size() == 1,
        "We do not support more than one monitored source validation from deployment");
    return deploymentLogAnalysisService.getLogAnalysisResult(
        accountId, verificationJobInstanceIds.get(0), label, deploymentLogAnalysisFilter, pageParams);
  }

  @Override
  public void abort(String activityId) {
    Activity activity = hPersistence.createQuery(Activity.class)
                            .filter(ActivityKeys.uuid, activityId)
                            .field(ActivityKeys.analysisStatus)
                            .notIn(ActivityVerificationStatus.getFinalStates())
                            .get();
    if (activity != null) {
      verificationJobInstanceService.abort(activity.getVerificationJobInstanceIds());
    }
  }

  @Override
  public Set<HealthSourceDTO> healthSources(String accountId, String activityId) {
    Set<HealthSourceDTO> healthSourceDTOS = new HashSet<>();
    List<VerificationJobInstance> verificationJobInstances =
        verificationJobInstanceService.get(getVerificationJobInstanceId(activityId));
    verificationJobInstances.forEach(verificationJobInstance -> {
      verificationJobInstance.getCvConfigMap().forEach((s, cvConfig) -> {
        HealthSourceDTO healthSourceDTO = HealthSourceDTO.toHealthSourceDTO(
            HealthSourceDTO.toHealthSource(Arrays.asList(cvConfig), dataSourceTypeToHealthSourceTransformerMap));
        healthSourceDTO.setIdentifier(cvConfig.getFullyQualifiedIdentifier());
        healthSourceDTOS.add(healthSourceDTO);
      });
    });
    return healthSourceDTOS;
  }

  @Override
  public String createActivityForDemo(Activity activity, ActivityVerificationStatus verificationStatus) {
    activity.validate();
    List<VerificationJobInstance> verificationJobInstances = new ArrayList<>();
    activity.getVerificationJobs().forEach(verificationJob -> {
      VerificationJobInstanceBuilder verificationJobInstanceBuilder = fillOutCommonJobInstanceProperties(
          activity, verificationJob.resolveAdditionsFields(verificationJobInstanceService));
      verificationJobInstanceBuilder.verificationStatus(verificationStatus);
      validateJob(verificationJob);
      activity.fillInVerificationJobInstanceDetails(verificationJobInstanceBuilder);

      verificationJobInstances.add(verificationJobInstanceBuilder.build());
    });
    activity.setVerificationJobInstanceIds(
        verificationJobInstanceService.createDemoInstances(verificationJobInstances));
    hPersistence.save(activity);
    log.info("Registered demo activity of type {} for account {}, project {}, org {}", activity.getType(),
        activity.getAccountId(), activity.getProjectIdentifier(), activity.getOrgIdentifier());
    return activity.getUuid();
  }

  private List<String> getVerificationJobInstanceId(String activityId) {
    Preconditions.checkNotNull(activityId);
    CVNGStepTask cvngStepTask = cvngStepTaskService.getByCallBackId(activityId);
    if (cvngStepTask != null && StringUtils.isBlank(cvngStepTask.getVerificationJobInstanceId())) {
      Activity activity = get(activityId);
      Preconditions.checkNotNull(activity, String.format("Activity does not exists with activityID %s", activityId));
      return activity.getVerificationJobInstanceIds();
    } else {
      return Arrays.asList(cvngStepTask.getVerificationJobInstanceId());
    }
  }

  private void validateJob(VerificationJob verificationJob) {
    List<CVConfig> cvConfigs = verificationJobInstanceService.getCVConfigsForVerificationJob(verificationJob);
    Preconditions.checkState(isNotEmpty(cvConfigs),
        "No monitoring sources with identifiers %s defined for environment %s and service %s",
        verificationJob.getMonitoringSources(), verificationJob.getEnvIdentifier(),
        verificationJob.getServiceIdentifier());
  }

  private VerificationJobInstanceBuilder fillOutCommonJobInstanceProperties(
      Activity activity, VerificationJob verificationJob) {
    return VerificationJobInstance.builder()
        .accountId(activity.getAccountId())
        .executionStatus(ExecutionStatus.QUEUED)
        .deploymentStartTime(activity.getActivityStartTime())
        .resolvedJob(verificationJob);
  }

  public Activity getActivityFromDTO(ActivityDTO activityDTO) {
    Activity activity;
    switch (activityDTO.getType()) {
      case DEPLOYMENT:
        activity = DeploymentActivity.builder().build();
        break;
      case KUBERNETES:
        throw new IllegalStateException("KUBERNETES events are handled by its own service");
      default:
        throw new IllegalStateException("Invalid type " + activityDTO.getType());
    }

    activity.fromDTO(activityDTO);
    return activity;
  }

  @Override
  public String upsert(Activity activity) {
    ActivityUpdateHandler handler = activityUpdateHandlerMap.get(activity.getType());
    ActivityUpdatableEntity activityUpdatableEntity = activityUpdatableEntityMap.get(activity.getType());
    try (AcquiredLock acquiredLock = persistentLocker.waitToAcquireLock(Activity.class,
             activityUpdatableEntity.getEntityKeyString(activity), Duration.ofSeconds(6), Duration.ofSeconds(4))) {
      Optional<Activity> optionalFromDb =
          StringUtils.isEmpty(activity.getUuid()) ? getFromDb(activity) : Optional.ofNullable(get(activity.getUuid()));
      if (optionalFromDb.isPresent()) {
        UpdateOperations<Activity> updateOperations =
            hPersistence.createUpdateOperations(activityUpdatableEntity.getEntityClass());
        activityUpdatableEntity.setUpdateOperations(updateOperations, activity);
        if (handler != null) {
          handler.handleUpdate(optionalFromDb.get(), activity);
        }
        hPersistence.update(optionalFromDb.get(), updateOperations);
        return optionalFromDb.get().getUuid();
      } else {
        if (handler != null) {
          handler.handleCreate(activity);
        }
        hPersistence.save(activity);
      }
      log.info("Registered  an activity of type {} for account {}, project {}, org {}", activity.getType(),
          activity.getAccountId(), activity.getProjectIdentifier(), activity.getOrgIdentifier());
      return activity.getUuid();
    }
  }

  @Override
  public List<Activity> get(ServiceEnvironmentParams serviceEnvironmentParams, List<String> changeSourceIdentifiers,
      Instant startTime, Instant endTime, List<ActivityType> activityTypes) {
    Query<Activity> query = createQuery(serviceEnvironmentParams, changeSourceIdentifiers, startTime, endTime);
    if (CollectionUtils.isNotEmpty(activityTypes)) {
      query = query.field(ActivityKeys.type).in(activityTypes);
    }
    return query.asList();
  }

  @Override
  public Long getCount(ServiceEnvironmentParams serviceEnvironmentParams, List<String> changeSourceIdentifiers,
      Instant startTime, Instant endTime, List<ActivityType> activityTypes) {
    Query<Activity> query = createQuery(serviceEnvironmentParams, changeSourceIdentifiers, startTime, endTime);
    if (CollectionUtils.isNotEmpty(activityTypes)) {
      query = query.field(ActivityKeys.type).in(activityTypes);
    }
    return query.count();
  }

  @Override
  public Long getCount(ProjectParams projectParams, List<String> serviceIdentifiers,
      List<String> environmentIdentifiers, Instant startTime, Instant endTime, List<ActivityType> activityTypes) {
    Query<Activity> query = createQuery(projectParams, startTime, endTime).disableValidation();
    if (EmptyPredicate.isNotEmpty(serviceIdentifiers)) {
      query.or(new Criteria[] {query.criteria(ActivityKeys.serviceIdentifier).in(serviceIdentifiers),
          query
              .criteria(
                  KubernetesClusterActivityKeys.relatedAppServices + "." + ServiceEnvironmentKeys.serviceIdentifier)
              .in(serviceIdentifiers)});
    }
    if (EmptyPredicate.isNotEmpty(environmentIdentifiers)) {
      query.or(new Criteria[] {query.criteria(ActivityKeys.environmentIdentifier).in(environmentIdentifiers),
          query
              .criteria(
                  KubernetesClusterActivityKeys.relatedAppServices + "." + ServiceEnvironmentKeys.environmentIdentifier)
              .in(environmentIdentifiers)});
    }
    if (EmptyPredicate.isNotEmpty(activityTypes)) {
      query = query.field(ActivityKeys.type).in(activityTypes);
    }
    return query.count();
  }

  private Query<Activity> createQuery(ServiceEnvironmentParams serviceEnvironmentParams,
      List<String> changeSourceIdentifiers, Instant startTime, Instant endTime) {
    return createQuery(serviceEnvironmentParams)
        .field(ActivityKeys.changeSourceIdentifier)
        .in(changeSourceIdentifiers)
        .field(ActivityKeys.eventTime)
        .lessThan(endTime)
        .field(ActivityKeys.eventTime)
        .greaterThanOrEq(startTime);
  }

  private Query<Activity> createQuery(ServiceEnvironmentParams serviceEnvironmentParams) {
    return hPersistence.createQuery(Activity.class)
        .filter(ActivityKeys.accountId, serviceEnvironmentParams.getAccountIdentifier())
        .filter(ActivityKeys.orgIdentifier, serviceEnvironmentParams.getOrgIdentifier())
        .filter(ActivityKeys.projectIdentifier, serviceEnvironmentParams.getProjectIdentifier())
        .filter(ActivityKeys.environmentIdentifier, serviceEnvironmentParams.getEnvironmentIdentifier())
        .filter(ActivityKeys.serviceIdentifier, serviceEnvironmentParams.getServiceIdentifier());
  }

  private Query<Activity> createQuery(ProjectParams projectParams, Instant startTime, Instant endTime) {
    return hPersistence.createQuery(Activity.class)
        .filter(ActivityKeys.accountId, projectParams.getAccountIdentifier())
        .filter(ActivityKeys.orgIdentifier, projectParams.getOrgIdentifier())
        .filter(ActivityKeys.projectIdentifier, projectParams.getProjectIdentifier())
        .field(ActivityKeys.eventTime)
        .lessThan(endTime)
        .field(ActivityKeys.eventTime)
        .greaterThanOrEq(startTime);
  }

  private Optional<Activity> getFromDb(Activity activity) {
    ActivityUpdatableEntity activityUpdatableEntity = activityUpdatableEntityMap.get(activity.getType());
    return Optional.ofNullable(
        (Activity) activityUpdatableEntity
            .populateKeyQuery(hPersistence.createQuery(activityUpdatableEntity.getEntityClass()), activity)
            .get());
  }
}
