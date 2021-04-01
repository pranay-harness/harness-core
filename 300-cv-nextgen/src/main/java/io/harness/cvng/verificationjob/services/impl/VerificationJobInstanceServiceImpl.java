package io.harness.cvng.verificationjob.services.impl;

import static io.harness.cvng.activity.CVActivityConstants.HEALTH_VERIFICATION_RETRIGGER_BUFFER_MINS;
import static io.harness.cvng.beans.DataCollectionExecutionStatus.QUEUED;
import static io.harness.cvng.beans.DataCollectionExecutionStatus.SUCCESS;
import static io.harness.cvng.core.utils.DateTimeUtils.roundDownTo1MinBoundary;
import static io.harness.cvng.verificationjob.entities.VerificationJobInstance.ENV_IDENTIFIER_KEY;
import static io.harness.cvng.verificationjob.entities.VerificationJobInstance.ORG_IDENTIFIER_KEY;
import static io.harness.cvng.verificationjob.entities.VerificationJobInstance.PROJECT_IDENTIFIER_KEY;
import static io.harness.cvng.verificationjob.entities.VerificationJobInstance.SERVICE_IDENTIFIER_KEY;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;

import static java.util.stream.Collectors.groupingBy;

import io.harness.cvng.activity.beans.ActivityVerificationSummary;
import io.harness.cvng.activity.beans.DeploymentActivityPopoverResultDTO;
import io.harness.cvng.activity.beans.DeploymentActivityResultDTO.DeploymentResultSummary;
import io.harness.cvng.activity.beans.DeploymentActivityResultDTO.DeploymentVerificationJobInstanceSummary;
import io.harness.cvng.activity.beans.DeploymentActivityVerificationResultDTO;
import io.harness.cvng.alert.services.api.AlertRuleService;
import io.harness.cvng.analysis.beans.Risk;
import io.harness.cvng.analysis.services.api.VerificationJobInstanceAnalysisService;
import io.harness.cvng.beans.DataCollectionInfo;
import io.harness.cvng.beans.activity.ActivityVerificationStatus;
import io.harness.cvng.beans.job.VerificationJobType;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.core.beans.TimeRange;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.DataCollectionTask;
import io.harness.cvng.core.entities.DataCollectionTask.Type;
import io.harness.cvng.core.entities.DeploymentDataCollectionTask;
import io.harness.cvng.core.entities.MetricCVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;
import io.harness.cvng.core.services.api.DataCollectionTaskService;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.cvng.core.services.api.MonitoringSourcePerpetualTaskService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.dashboard.services.api.HealthVerificationHeatMapService;
import io.harness.cvng.statemachine.services.intfc.OrchestrationService;
import io.harness.cvng.verificationjob.beans.AdditionalInfo;
import io.harness.cvng.verificationjob.beans.TestVerificationBaselineExecutionDTO;
import io.harness.cvng.verificationjob.entities.HealthVerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob.VerificationJobKeys;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.ExecutionStatus;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.ProgressLog;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.VerificationJobInstanceKeys;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import io.harness.persistence.HPersistence;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.UpdateOptions;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
public class VerificationJobInstanceServiceImpl implements VerificationJobInstanceService {
  @Inject private HPersistence hPersistence;
  @Inject private CVConfigService cvConfigService;
  @Inject private DataCollectionTaskService dataCollectionTaskService;
  @Inject private Injector injector;
  @Inject private MetricPackService metricPackService;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private VerificationJobInstanceAnalysisService verificationJobInstanceAnalysisService;
  @Inject private OrchestrationService orchestrationService;
  @Inject private Clock clock;
  @Inject private HealthVerificationHeatMapService healthVerificationHeatMapService;
  @Inject private NextGenService nextGenService;
  @Inject private AlertRuleService alertRuleService;
  @Inject private MonitoringSourcePerpetualTaskService monitoringSourcePerpetualTaskService;

  @Override
  public String create(VerificationJobInstance verificationJobInstance) {
    hPersistence.save(verificationJobInstance);
    return verificationJobInstance.getUuid();
  }
  public List<String> create(List<VerificationJobInstance> verificationJobInstances) {
    return hPersistence.save(verificationJobInstances);
  }
  @Override
  public List<String> dedupCreate(List<VerificationJobInstance> verificationJobInstances) {
    verificationJobInstances.forEach(verificationJobInstance
        -> Preconditions.checkState(verificationJobInstance.getResolvedJob().getType() == VerificationJobType.HEALTH));
    Set<String> jobInstanceIds = new HashSet<>();
    verificationJobInstances.forEach(verificationJobInstance -> {
      String jobInstanceId = generateUuid();
      Query<VerificationJobInstance> query =
          hPersistence.createQuery(VerificationJobInstance.class, excludeAuthority)
              .filter(VerificationJobInstanceKeys.accountId, verificationJobInstance.getAccountId())
              .filter(ORG_IDENTIFIER_KEY, verificationJobInstance.getResolvedJob().getOrgIdentifier())
              .filter(PROJECT_IDENTIFIER_KEY, verificationJobInstance.getResolvedJob().getProjectIdentifier())
              .filter(ENV_IDENTIFIER_KEY, verificationJobInstance.getResolvedJob().getEnvIdentifier())
              .filter(SERVICE_IDENTIFIER_KEY, verificationJobInstance.getResolvedJob().getServiceIdentifier())
              .field(VerificationJobInstanceKeys.startTime)
              .greaterThanOrEq(verificationJobInstance.getStartTime().minus(
                  Duration.ofMinutes(HEALTH_VERIFICATION_RETRIGGER_BUFFER_MINS)))
              .field(VerificationJobInstanceKeys.startTime)
              .lessThanOrEq(verificationJobInstance.getStartTime());
      VerificationJobInstance savedInstance = hPersistence.upsert(query,
          hPersistence.createUpdateOperations(VerificationJobInstance.class)
              .setOnInsert(VerificationJobInstanceKeys.uuid, jobInstanceId)
              .setOnInsert(VerificationJobInstanceKeys.accountId, verificationJobInstance.getAccountId())
              .setOnInsert(VerificationJobInstanceKeys.executionStatus, verificationJobInstance.getExecutionStatus())
              .setOnInsert(VerificationJobInstanceKeys.startTime, verificationJobInstance.getStartTime())
              .setOnInsert(VerificationJobInstanceKeys.resolvedJob, verificationJobInstance.getResolvedJob()),
          new FindAndModifyOptions().upsert(true));
      // only the activity which inserted should have verification associated.
      if (jobInstanceId.equals(savedInstance.getUuid())) {
        jobInstanceIds.add(savedInstance.getUuid());
      }
    });
    return jobInstanceIds.stream().collect(Collectors.toList());
  }

  @Override
  public List<VerificationJobInstance> get(List<String> verificationJobInstanceIds) {
    if (isEmpty(verificationJobInstanceIds)) {
      return Collections.emptyList();
    }
    return hPersistence.createQuery(VerificationJobInstance.class, excludeAuthority)
        .field(VerificationJobInstanceKeys.uuid)
        .in(verificationJobInstanceIds)
        .asList();
  }

  public List<VerificationJobInstance> getHealthVerificationJobInstances(List<String> verificationJobInstanceIds) {
    if (isEmpty(verificationJobInstanceIds)) {
      return Collections.emptyList();
    }
    return get(verificationJobInstanceIds)
        .stream()
        .filter(
            verificationJobInstance -> verificationJobInstance.getResolvedJob().getType() == VerificationJobType.HEALTH)
        .collect(Collectors.toList());
  }

  @Override
  public VerificationJobInstance getVerificationJobInstance(String verificationJobInstanceId) {
    return hPersistence.get(VerificationJobInstance.class, verificationJobInstanceId);
  }

  @Override
  public void processVerificationJobInstance(VerificationJobInstance verificationJobInstance) {
    log.info("Processing verificationJobInstance with ID: {}", verificationJobInstance.getUuid());
    if (verificationJobInstance.getResolvedJob().shouldDoDataCollection()) {
      createDataCollectionTasks(verificationJobInstance);
    } else {
      createAndQueueHealthVerification(verificationJobInstance);
    }
  }

  private void createAndQueueHealthVerification(VerificationJobInstance verificationJobInstance) {
    // We dont do any data collection for health verification. So just queue the analysis.
    VerificationJob verificationJob = verificationJobInstance.getResolvedJob();
    List<CVConfig> cvConfigs = getCVConfigsForVerificationJob(verificationJobInstance.getResolvedJob());
    cvConfigs.forEach(cvConfig -> {
      String verificationTaskId = verificationTaskService.create(
          cvConfig.getAccountId(), cvConfig.getUuid(), verificationJobInstance.getUuid());
      log.info("For verificationJobInstance with ID: {}, creating a new health analysis with verificationTaskID {}",
          verificationJobInstance.getUuid(), verificationTaskId);
      orchestrationService.queueAnalysis(verificationTaskId,
          ((HealthVerificationJob) verificationJobInstance.getResolvedJob())
              .getPreActivityVerificationStartTime(verificationJobInstance.getStartTime(),
                  verificationJobInstance.getPreActivityVerificationStartTime()),
          verificationJobInstance.getStartTime());
    });

    markRunning(verificationJobInstance.getUuid(), cvConfigs);
  }

  public List<CVConfig> getCVConfigsForVerificationJob(VerificationJob verificationJob) {
    Preconditions.checkNotNull(verificationJob);
    List<String> monitoringSourceFilter = verificationJob.getMonitoringSources();
    if (verificationJob.isDefaultJob()) {
      monitoringSourceFilter = null;
    }

    return cvConfigService.listByMonitoringSources(verificationJob.getAccountId(), verificationJob.getOrgIdentifier(),
        verificationJob.getProjectIdentifier(), verificationJob.getServiceIdentifier(),
        verificationJob.getEnvIdentifier(), monitoringSourceFilter);
  }
  @Override
  public void markTimedOutIfNoProgress(VerificationJobInstance verificationJobInstance) {
    Preconditions.checkNotNull(verificationJobInstance);
    Preconditions.checkState(ExecutionStatus.nonFinalStatuses().contains(verificationJobInstance.getExecutionStatus()),
        "executionStatus should be non final status");
    if (verificationJobInstance.isExecutionTimedOut(clock.instant())) {
      log.error("VerificationJobInstance timed out {} endTime: {}", verificationJobInstance,
          verificationJobInstance.getEndTime());
      // TODO: add telemetry and alerting
      UpdateOperations<VerificationJobInstance> updateOperations =
          hPersistence.createUpdateOperations(VerificationJobInstance.class)
              .set(VerificationJobInstanceKeys.executionStatus, ExecutionStatus.TIMEOUT);
      Query<VerificationJobInstance> query =
          hPersistence.createQuery(VerificationJobInstance.class)
              .filter(VerificationJobInstanceKeys.uuid, verificationJobInstance.getUuid())
              .field(VerificationJobInstanceKeys.executionStatus)
              .in(ExecutionStatus.nonFinalStatuses()); // To avoid any race condition.
      hPersistence.update(query, updateOperations);
    }
  }

  @Override
  public CVConfig getEmbeddedCVConfig(String cvConfigId, String verificationJobInstanceId) {
    VerificationJobInstance verificationJobInstance = getVerificationJobInstance(verificationJobInstanceId);
    if (verificationJobInstance.getCvConfigMap()
        != null) { // TODO: this is just migration logic. Remove this check once VerificationJobInstances expires.
      return verificationJobInstance.getCvConfigMap().get(cvConfigId);
    }
    return cvConfigService.get(cvConfigId);
  }

  @Override
  public void createDataCollectionTasks(VerificationJobInstance verificationJobInstance) {
    VerificationJob verificationJob = verificationJobInstance.getResolvedJob();
    Preconditions.checkNotNull(verificationJob);
    List<CVConfig> cvConfigs = getCVConfigsForVerificationJob(verificationJob);
    Preconditions.checkState(isNotEmpty(cvConfigs), "No config is matching the criteria");
    createDataCollectionTasks(verificationJobInstance, verificationJob, cvConfigs);
    markRunning(verificationJobInstance.getUuid(), cvConfigs);
  }

  @Override
  public void logProgress(ProgressLog progressLog) {
    progressLog.setCreatedAt(clock.instant());
    progressLog.validate();
    String verificationJobInstanceId =
        verificationTaskService.getVerificationJobInstanceId(progressLog.getVerificationTaskId());
    UpdateOperations<VerificationJobInstance> verificationJobInstanceUpdateOperations =
        hPersistence.createUpdateOperations(VerificationJobInstance.class)
            .addToSet(VerificationJobInstanceKeys.progressLogs, progressLog);
    if (progressLog.shouldUpdateJobStatus()) {
      verificationJobInstanceUpdateOperations.set(
          VerificationJobInstanceKeys.executionStatus, progressLog.getVerificationJobExecutionStatus());
    }
    UpdateOptions options = new UpdateOptions();
    options.upsert(true);
    hPersistence.getDatastore(VerificationJobInstance.class)
        .update(hPersistence.createQuery(VerificationJobInstance.class)
                    .filter(VerificationJobInstanceKeys.uuid, verificationJobInstanceId),
            verificationJobInstanceUpdateOperations, options);
    updateStatusIfDone(verificationJobInstanceId);
  }

  private void updateStatusIfDone(String verificationJobInstanceId) {
    VerificationJobInstance verificationJobInstance = getVerificationJobInstance(verificationJobInstanceId);
    if (verificationJobInstance.getExecutionStatus() != ExecutionStatus.RUNNING) {
      // If the last update already updated the status.
      return;
    }
    int verificationTaskCount =
        verificationTaskService
            .getVerificationTaskIds(verificationJobInstance.getAccountId(), verificationJobInstanceId)
            .size();
    if (verificationJobInstance.getProgressLogs()
            .stream()
            .filter(progressLog -> progressLog.isLastProgressLog(verificationJobInstance))
            .map(ProgressLog::getVerificationTaskId)
            .distinct()
            .count()
        == verificationTaskCount) {
      verificationJobInstance.setExecutionStatus(ExecutionStatus.SUCCESS);
      ActivityVerificationStatus activityVerificationStatus = getDeploymentVerificationStatus(verificationJobInstance);
      UpdateOperations<VerificationJobInstance> verificationJobInstanceUpdateOperations =
          hPersistence.createUpdateOperations(VerificationJobInstance.class);
      verificationJobInstanceUpdateOperations.set(VerificationJobInstanceKeys.executionStatus, SUCCESS)
          .set(VerificationJobInstanceKeys.verificationStatus, activityVerificationStatus);
      hPersistence.getDatastore(VerificationJobInstance.class)
          .update(hPersistence.createQuery(VerificationJobInstance.class)
                      .filter(VerificationJobInstanceKeys.uuid, verificationJobInstanceId),
              verificationJobInstanceUpdateOperations, new UpdateOptions());

      alertRuleService.processDeploymentVerificationJobInstanceId(verificationJobInstanceId);
    }
  }

  @Override
  public Optional<TimeRange> getPreDeploymentTimeRange(String verificationJobInstanceId) {
    VerificationJobInstance verificationJobInstance = getVerificationJobInstance(verificationJobInstanceId);
    VerificationJob verificationJob = verificationJobInstance.getResolvedJob();
    return verificationJob.getPreActivityTimeRange(verificationJobInstance.getDeploymentStartTime());
  }

  @Override
  public DeploymentActivityVerificationResultDTO getAggregatedVerificationResult(
      List<String> verificationJobInstanceIds) {
    List<VerificationJobInstance> verificationJobInstances = get(verificationJobInstanceIds);
    List<VerificationJobInstance> postDeploymentVerificationJobInstances =
        getPostDeploymentVerificationJobInstances(verificationJobInstances);
    Map<EnvironmentType, List<VerificationJobInstance>> preAndProductionDeploymentGroup =
        getPreAndProductionDeploymentGroup(verificationJobInstances);

    return DeploymentActivityVerificationResultDTO.builder()
        .preProductionDeploymentSummary(
            getActivityVerificationSummary(preAndProductionDeploymentGroup.get(EnvironmentType.PreProduction)))
        .productionDeploymentSummary(
            getActivityVerificationSummary(preAndProductionDeploymentGroup.get(EnvironmentType.Production)))
        .postDeploymentSummary(getActivityVerificationSummary(postDeploymentVerificationJobInstances))
        .build();
  }

  @Override
  public void addResultsToDeploymentResultSummary(
      String accountId, List<String> verificationJobInstanceIds, DeploymentResultSummary deploymentResultSummary) {
    List<VerificationJobInstance> verificationJobInstances = get(verificationJobInstanceIds);
    List<VerificationJobInstance> postDeploymentVerificationJobInstances =
        getPostDeploymentVerificationJobInstances(verificationJobInstances);
    Map<EnvironmentType, List<VerificationJobInstance>> preAndProductionDeploymentGroup =
        getPreAndProductionDeploymentGroup(verificationJobInstances);
    addDeploymentVerificationJobInstanceSummaries(preAndProductionDeploymentGroup.get(EnvironmentType.PreProduction),
        deploymentResultSummary.getPreProductionDeploymentVerificationJobInstanceSummaries());
    addDeploymentVerificationJobInstanceSummaries(preAndProductionDeploymentGroup.get(EnvironmentType.Production),
        deploymentResultSummary.getProductionDeploymentVerificationJobInstanceSummaries());
    addDeploymentVerificationJobInstanceSummaries(postDeploymentVerificationJobInstances,
        deploymentResultSummary.getPostDeploymentVerificationJobInstanceSummaries());
  }

  @Override
  public DeploymentActivityPopoverResultDTO getDeploymentVerificationPopoverResult(
      List<String> verificationJobInstanceIds) {
    List<VerificationJobInstance> verificationJobInstances = get(verificationJobInstanceIds);
    Preconditions.checkState(isNotEmpty(verificationJobInstances), "No VerificationJobInstance found with IDs %s",
        verificationJobInstanceIds.toString());
    List<VerificationJobInstance> postDeploymentVerificationJobInstances =
        getPostDeploymentVerificationJobInstances(verificationJobInstances);
    Map<EnvironmentType, List<VerificationJobInstance>> preAndProductionDeploymentGroup =
        getPreAndProductionDeploymentGroup(verificationJobInstances);

    return DeploymentActivityPopoverResultDTO.builder()
        .preProductionDeploymentSummary(
            deploymentPopoverSummary(preAndProductionDeploymentGroup.get(EnvironmentType.PreProduction)))
        .productionDeploymentSummary(
            deploymentPopoverSummary(preAndProductionDeploymentGroup.get(EnvironmentType.Production)))
        .postDeploymentSummary(deploymentPopoverSummary(postDeploymentVerificationJobInstances))
        .build();
  }
  @Override
  public List<TestVerificationBaselineExecutionDTO> getTestJobBaselineExecutions(
      String accountId, String orgIdentifier, String projectIdentifier, String verificationJobIdentifier) {
    return getTestJobBaselineExecutions(accountId, orgIdentifier, projectIdentifier, verificationJobIdentifier, 5);
  }

  public List<TestVerificationBaselineExecutionDTO> getTestJobBaselineExecutions(
      String accountId, String orgIdentifier, String projectIdentifier, String verificationJobIdentifier, int limit) {
    List<VerificationJobInstance> verificationJobInstances =
        hPersistence.createQuery(VerificationJobInstance.class)
            .filter(VerificationJobInstanceKeys.executionStatus, ExecutionStatus.SUCCESS)
            .filter(VerificationJobInstanceKeys.accountId, accountId)
            .filter(PROJECT_IDENTIFIER_KEY, projectIdentifier)
            .filter(ORG_IDENTIFIER_KEY, orgIdentifier)
            .filter(VerificationJobInstance.VERIFICATION_JOB_IDENTIFIER_KEY, verificationJobIdentifier)
            .filter(VerificationJobInstance.VERIFICATION_JOB_TYPE_KEY, VerificationJobType.TEST)
            .filter(VerificationJobInstanceKeys.verificationStatus, ActivityVerificationStatus.VERIFICATION_PASSED)
            .order(Sort.descending(VerificationJobInstanceKeys.createdAt))
            .asList(new FindOptions().limit(limit));
    return verificationJobInstances.stream()
        .map(verificationJobInstance
            -> TestVerificationBaselineExecutionDTO.builder()
                   .verificationJobInstanceId(verificationJobInstance.getUuid())
                   .createdAt(verificationJobInstance.getCreatedAt())
                   .build())
        .collect(Collectors.toList());
  }

  @Override
  public Optional<String> getLastSuccessfulTestVerificationJobExecutionId(
      String accountId, String projectIdentifier, String orgIdentifier, String verificationJobIdentifier) {
    List<TestVerificationBaselineExecutionDTO> testVerificationBaselineExecutionDTOs =
        getTestJobBaselineExecutions(accountId, projectIdentifier, orgIdentifier, verificationJobIdentifier, 1);
    if (testVerificationBaselineExecutionDTOs.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(testVerificationBaselineExecutionDTOs.get(0).getVerificationJobInstanceId());
    }
  }

  private List<VerificationJobInstance> getPostDeploymentVerificationJobInstances(
      List<VerificationJobInstance> verificationJobInstances) {
    return verificationJobInstances.stream()
        .filter(
            verificationJobInstance -> verificationJobInstance.getResolvedJob().getType() == VerificationJobType.HEALTH)
        .collect(Collectors.toList());
  }

  private Map<EnvironmentType, List<VerificationJobInstance>> getPreAndProductionDeploymentGroup(
      List<VerificationJobInstance> verificationJobInstances) {
    return verificationJobInstances.stream()
        .filter(
            verificationJobInstance -> verificationJobInstance.getResolvedJob().getType() != VerificationJobType.HEALTH)
        .collect(groupingBy(verificationJobInstance -> {
          VerificationJob resolvedJob = verificationJobInstance.getResolvedJob();
          EnvironmentResponseDTO environmentResponseDTO = getEnvironment(resolvedJob);
          return environmentResponseDTO.getType();
        }));
  }

  private void addDeploymentVerificationJobInstanceSummaries(List<VerificationJobInstance> verificationJobInstances,
      List<DeploymentVerificationJobInstanceSummary> deploymentVerificationJobInstanceSummaries) {
    if (!isEmpty(verificationJobInstances)) {
      verificationJobInstances.forEach(verificationJobInstance -> {
        deploymentVerificationJobInstanceSummaries.add(
            getDeploymentVerificationJobInstanceSummary(verificationJobInstance));
      });
    }
  }

  //  TODO find the right place for this switch case
  private AdditionalInfo getAdditionalInfo(String accountId, VerificationJobInstance verificationJobInstance) {
    switch (verificationJobInstance.getResolvedJob().getType()) {
      case CANARY:
      case BLUE_GREEN:
        return verificationJobInstanceAnalysisService.getCanaryBlueGreenAdditionalInfo(
            accountId, verificationJobInstance);
      case TEST:
        return verificationJobInstanceAnalysisService.getLoadTestAdditionalInfo(accountId, verificationJobInstance);
      case HEALTH:
        return verificationJobInstanceAnalysisService.getHealthAdditionInfo(accountId, verificationJobInstance);
      default:
        throw new IllegalStateException(
            "Failed to get additional info due to unknown type: " + verificationJobInstance.getResolvedJob().getType());
    }
  }

  private ActivityVerificationStatus getDeploymentVerificationStatus(VerificationJobInstance verificationJobInstance) {
    switch (verificationJobInstance.getExecutionStatus()) {
      case QUEUED:
        return ActivityVerificationStatus.NOT_STARTED;
      case FAILED:
      case TIMEOUT:
        return ActivityVerificationStatus.ERROR;
      case RUNNING:
        return ActivityVerificationStatus.IN_PROGRESS;
      case SUCCESS:
        Optional<Risk> risk = getLatestRisk(verificationJobInstance);
        if (risk.isPresent()) {
          if (risk.get().isLessThanEq(Risk.MEDIUM)) {
            return ActivityVerificationStatus.VERIFICATION_PASSED;
          } else {
            return ActivityVerificationStatus.VERIFICATION_FAILED;
          }
        }
        return ActivityVerificationStatus.IN_PROGRESS;
      default:
        throw new IllegalStateException(verificationJobInstance.getExecutionStatus() + " not supported");
    }
  }

  private Optional<Risk> getLatestRisk(VerificationJobInstance verificationJobInstance) {
    if (ExecutionStatus.noAnalysisStatuses().contains(verificationJobInstance.getExecutionStatus())) {
      return Optional.empty();
    }
    if (verificationJobInstance.getResolvedJob().getType() == VerificationJobType.HEALTH) {
      return healthVerificationHeatMapService.getVerificationRisk(
          verificationJobInstance.getAccountId(), verificationJobInstance.getUuid());
    } else {
      return verificationJobInstanceAnalysisService.getLatestRiskScore(
          verificationJobInstance.getAccountId(), verificationJobInstance.getUuid());
    }
  }

  @Override
  @Nullable
  public ActivityVerificationSummary getActivityVerificationSummary(
      List<VerificationJobInstance> verificationJobInstances) {
    if (isEmpty(verificationJobInstances)) {
      return null;
    }
    VerificationJobInstance minVerificationInstanceJob =
        Collections.min(verificationJobInstances, Comparator.comparing(VerificationJobInstance::getStartTime));
    VerificationJobInstance maxDuration =
        Collections.max(verificationJobInstances, Comparator.comparing(vji -> vji.getResolvedJob().getDuration()));
    int progressPercentage = verificationJobInstances.size() == 0
        ? 0
        : verificationJobInstances.stream().mapToInt(VerificationJobInstance::getProgressPercentage).sum()
            / verificationJobInstances.size();
    long timeRemainingMs =
        verificationJobInstances.stream()
            .mapToLong(verificationJobInstance -> verificationJobInstance.getRemainingTime(clock.instant()).toMillis())
            .max()
            .getAsLong();

    int total = verificationJobInstances.size();
    int progress = 0;
    int passed = 0;
    int failed = 0;
    int notStarted = 0;
    int errors = 0;
    List<Risk> latestRiskScores = new ArrayList<>();
    for (int i = 0; i < verificationJobInstances.size(); i++) {
      VerificationJobInstance verificationJobInstance = verificationJobInstances.get(i);
      switch (verificationJobInstance.getExecutionStatus()) {
        case QUEUED:
          notStarted++;
          break;
        case FAILED:
        case TIMEOUT:
          errors++;
          break;
        case SUCCESS:
          Optional<Risk> risk = getLatestRisk(verificationJobInstance);

          if (risk.isPresent()) {
            latestRiskScores.add(risk.get());
            if (risk.get().isLessThanEq(Risk.MEDIUM)) {
              passed++;
            } else {
              failed++;
            }
          }
          break;
        case RUNNING:
          Optional<Risk> latestRisk = getLatestRisk(verificationJobInstance);
          if (latestRisk.isPresent()) {
            latestRiskScores.add(latestRisk.get());
          }
          progress++;
          break;
        default:
          throw new IllegalStateException(verificationJobInstance.getExecutionStatus() + " not supported");
      }
    }
    return ActivityVerificationSummary.builder()
        .startTime(minVerificationInstanceJob.getStartTime().toEpochMilli())
        .durationMs(maxDuration.getResolvedJob().getDuration().toMillis())
        .remainingTimeMs(timeRemainingMs)
        .progressPercentage(progressPercentage)
        .risk(latestRiskScores.isEmpty() ? null : Collections.max(latestRiskScores))
        .total(total)
        .failed(failed)
        .errors(errors)
        .passed(passed)
        .progress(progress)
        .notStarted(notStarted)
        .build();
  }

  private DeploymentVerificationJobInstanceSummary getDeploymentVerificationJobInstanceSummary(
      VerificationJobInstance verificationJobInstance) {
    return DeploymentVerificationJobInstanceSummary.builder()
        .startTime(verificationJobInstance.getStartTime().toEpochMilli())
        .durationMs(verificationJobInstance.getResolvedJob().getDuration().toMillis())
        .progressPercentage(verificationJobInstance.getProgressPercentage())
        .risk(getLatestRisk(verificationJobInstance).orElse(null))
        .environmentName(getEnvironment(verificationJobInstance.getResolvedJob()).getName())
        .jobName(verificationJobInstance.getResolvedJob().getJobName())
        .verificationJobInstanceId(verificationJobInstance.getUuid())
        .status(getDeploymentVerificationStatus(verificationJobInstance))
        .additionalInfo(getAdditionalInfo(verificationJobInstance.getAccountId(), verificationJobInstance))
        .build();
  }

  @Override
  public DeploymentVerificationJobInstanceSummary getDeploymentVerificationJobInstanceSummary(
      List<String> verificationJobInstanceIds) {
    Preconditions.checkState(isNotEmpty(verificationJobInstanceIds), "Should have at least one element");
    // TODO:  Currently taking just first element to respond. We need to talk to UX and create mocks to show the full
    // details in case of multiple verificationJobInstances.
    VerificationJobInstance verificationJobInstance = getVerificationJobInstance(verificationJobInstanceIds.get(0));
    return getDeploymentVerificationJobInstanceSummary(verificationJobInstance);
  }

  @Override
  public List<VerificationJobInstance> getRunningOrQueuedJobInstances(String accountId, String orgIdentifier,
      String projectIdentifier, String envIdentifier, String serviceIdentifier, VerificationJobType jobType,
      Instant endTimeBefore) {
    Preconditions.checkNotNull(orgIdentifier);
    Preconditions.checkNotNull(projectIdentifier);
    Preconditions.checkNotNull(envIdentifier);
    Preconditions.checkNotNull(serviceIdentifier);

    List<VerificationJobInstance> verificationJobInstances =
        hPersistence.createQuery(VerificationJobInstance.class, excludeAuthority)
            .filter(VerificationJobInstanceKeys.accountId, accountId)
            .filter(VerificationJobInstanceKeys.resolvedJob + "." + VerificationJobKeys.orgIdentifier, orgIdentifier)
            .filter(VerificationJobInstanceKeys.resolvedJob + "." + VerificationJobKeys.projectIdentifier,
                projectIdentifier)
            .filter(VerificationJobInstanceKeys.resolvedJob + "." + VerificationJobKeys.type, jobType)
            .field(VerificationJobInstanceKeys.executionStatus)
            .notIn(ExecutionStatus.finalStatuses())
            .asList();

    return verificationJobInstances.stream()
        .filter(instance
            -> instance.getResolvedJob().getServiceIdentifier().equals(serviceIdentifier)
                && instance.getResolvedJob().getEnvIdentifier().equals(envIdentifier)
                && instance.getEndTime().isAfter(endTimeBefore))
        .collect(Collectors.toList());
  }

  @Nullable
  private DeploymentActivityPopoverResultDTO.DeploymentPopoverSummary deploymentPopoverSummary(
      List<VerificationJobInstance> verificationJobInstances) {
    if (isEmpty(verificationJobInstances)) {
      return null;
    }

    List<DeploymentActivityPopoverResultDTO.VerificationResult> verificationResults =
        verificationJobInstances.stream()
            .map(verificationJobInstance
                -> DeploymentActivityPopoverResultDTO.VerificationResult.builder()
                       .status(getDeploymentVerificationStatus(verificationJobInstance))
                       .jobName(verificationJobInstance.getResolvedJob().getJobName())
                       .progressPercentage(verificationJobInstance.getProgressPercentage())
                       .remainingTimeMs(verificationJobInstance.getRemainingTime(clock.instant()).toMillis())
                       .startTime(verificationJobInstance.getStartTime().toEpochMilli())
                       .risk(getLatestRisk(verificationJobInstance).orElse(null))
                       .build())
            .collect(Collectors.toList());
    return DeploymentActivityPopoverResultDTO.DeploymentPopoverSummary.builder()
        .total(verificationJobInstances.size())
        .verificationResults(verificationResults)
        .build();
  }

  private EnvironmentResponseDTO getEnvironment(VerificationJob verificationJob) {
    return nextGenService.getEnvironment(verificationJob.getAccountId(), verificationJob.getOrgIdentifier(),
        verificationJob.getProjectIdentifier(), verificationJob.getEnvIdentifier());
  }

  private String getDataCollectionWorkerId(
      VerificationJobInstance verificationJobInstance, String monitoringSourceIdentifier, String connectorIdentifier) {
    return monitoringSourcePerpetualTaskService.getDeploymentWorkerId(verificationJobInstance.getAccountId(),
        verificationJobInstance.getResolvedJob().getOrgIdentifier(),
        verificationJobInstance.getResolvedJob().getProjectIdentifier(), connectorIdentifier,
        monitoringSourceIdentifier);
  }

  private void createDataCollectionTasks(
      VerificationJobInstance verificationJobInstance, VerificationJob verificationJob, List<CVConfig> cvConfigs) {
    Optional<TimeRange> preDeploymentTimeRange =
        verificationJob.getPreActivityTimeRange(verificationJobInstance.getDeploymentStartTime());
    List<TimeRange> timeRanges =
        verificationJob.getDataCollectionTimeRanges(roundDownTo1MinBoundary(verificationJobInstance.getStartTime()));
    cvConfigs.forEach(cvConfig -> {
      populateMetricPack(cvConfig);
      List<DataCollectionTask> dataCollectionTasks = new ArrayList<>();
      String verificationTaskId = verificationTaskService.create(
          cvConfig.getAccountId(), cvConfig.getUuid(), verificationJobInstance.getUuid());
      DataCollectionInfoMapper dataCollectionInfoMapper =
          injector.getInstance(Key.get(DataCollectionInfoMapper.class, Names.named(cvConfig.getType().name())));

      if (preDeploymentTimeRange.isPresent()) {
        DataCollectionInfo preDeploymentDataCollectionInfo = dataCollectionInfoMapper.toDataCollectionInfo(cvConfig);
        preDeploymentDataCollectionInfo.setDataCollectionDsl(cvConfig.getDataCollectionDsl());
        preDeploymentDataCollectionInfo.setCollectHostData(verificationJob.collectHostData());
        dataCollectionTasks.add(DeploymentDataCollectionTask.builder()
                                    .verificationTaskId(verificationTaskId)
                                    .dataCollectionWorkerId(getDataCollectionWorkerId(verificationJobInstance,
                                        cvConfig.getIdentifier(), cvConfig.getConnectorIdentifier()))
                                    .startTime(preDeploymentTimeRange.get().getStartTime())
                                    .endTime(preDeploymentTimeRange.get().getEndTime())
                                    .validAfter(preDeploymentTimeRange.get().getEndTime().plus(
                                        verificationJobInstance.getDataCollectionDelay()))
                                    .accountId(verificationJob.getAccountId())
                                    .status(QUEUED)
                                    .dataCollectionInfo(preDeploymentDataCollectionInfo)
                                    .queueAnalysis(cvConfig.queueAnalysisForPreDeploymentTask())
                                    .build());
      }

      timeRanges.forEach(timeRange -> {
        DataCollectionInfo dataCollectionInfo = dataCollectionInfoMapper.toDataCollectionInfo(cvConfig);
        // TODO: For Now the DSL is same for both. We need to see how this evolves when implementation other provider.
        // Keeping this simple for now.
        dataCollectionInfo.setDataCollectionDsl(cvConfig.getDataCollectionDsl());
        dataCollectionInfo.setCollectHostData(verificationJob.collectHostData());
        dataCollectionTasks.add(
            DeploymentDataCollectionTask.builder()
                .type(Type.DEPLOYMENT)
                .verificationTaskId(verificationTaskId)
                .dataCollectionWorkerId(getDataCollectionWorkerId(
                    verificationJobInstance, cvConfig.getIdentifier(), cvConfig.getConnectorIdentifier()))
                .startTime(timeRange.getStartTime())
                .endTime(timeRange.getEndTime())
                .validAfter(timeRange.getEndTime().plus(verificationJobInstance.getDataCollectionDelay()))
                .accountId(verificationJob.getAccountId())
                .status(QUEUED)
                .dataCollectionInfo(dataCollectionInfo)
                .build());
      });
      dataCollectionTaskService.createSeqTasks(dataCollectionTasks);
    });
  }
  private void populateMetricPack(CVConfig cvConfig) {
    if (cvConfig instanceof MetricCVConfig) {
      // TODO: get rid of this. Adding it to unblock. We need to redesign how are we setting DSL.
      metricPackService.populateDataCollectionDsl(cvConfig.getType(), ((MetricCVConfig) cvConfig).getMetricPack());
      metricPackService.populatePaths(cvConfig.getAccountId(), cvConfig.getOrgIdentifier(),
          cvConfig.getProjectIdentifier(), cvConfig.getType(), ((MetricCVConfig) cvConfig).getMetricPack());
    }
  }

  private void markRunning(String verificationJobInstanceId, List<CVConfig> cvConfigs) {
    Map<String, CVConfig> cvConfigMap =
        cvConfigs.stream().collect(Collectors.toMap(CVConfig::getUuid, cvConfig -> cvConfig));
    UpdateOperations<VerificationJobInstance> updateOperations =
        hPersistence.createUpdateOperations(VerificationJobInstance.class)
            .set(VerificationJobInstanceKeys.executionStatus, ExecutionStatus.RUNNING)
            .set(VerificationJobInstanceKeys.cvConfigMap, cvConfigMap);
    Query<VerificationJobInstance> query = hPersistence.createQuery(VerificationJobInstance.class)
                                               .filter(VerificationJobInstanceKeys.uuid, verificationJobInstanceId);
    hPersistence.update(query, updateOperations);
  }
}
