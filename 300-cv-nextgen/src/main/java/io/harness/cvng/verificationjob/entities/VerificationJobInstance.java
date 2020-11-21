package io.harness.cvng.verificationjob.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.cvng.CVConstants;
import io.harness.cvng.beans.DataCollectionExecutionStatus;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.cvng.verificationjob.beans.VerificationJobInstanceDTO;
import io.harness.cvng.verificationjob.beans.VerificationJobType;
import io.harness.cvng.verificationjob.entities.VerificationJob.VerificationJobKeys;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@FieldNameConstants(innerTypeName = "VerificationJobInstanceKeys")
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "verificationJobInstances", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class VerificationJobInstance
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess, PersistentRegularIterable {
  public static final String VERIFICATION_JOB_TYPE_KEY =
      String.format("%s.%s", VerificationJobInstanceKeys.resolvedJob, VerificationJobKeys.type);
  public static String PROJECT_IDENTIFIER_KEY =
      String.format("%s.%s", VerificationJobInstanceKeys.resolvedJob, VerificationJobKeys.projectIdentifier);
  public static String ORG_IDENTIFIER_KEY =
      String.format("%s.%s", VerificationJobInstanceKeys.resolvedJob, VerificationJobKeys.orgIdentifier);
  public static String VERIFICATION_JOB_IDENTIFIER_KEY =
      String.format("%s.%s", VerificationJobInstanceKeys.resolvedJob, VerificationJobKeys.identifier);
  @Id private String uuid;
  @NotNull @FdIndex private String accountId;
  private long createdAt;
  private long lastUpdatedAt;
  private ExecutionStatus executionStatus;

  private String verificationJobIdentifier;

  private Instant deploymentStartTime;
  private Instant startTime;
  @FdIndex private Long dataCollectionTaskIteration;
  @FdIndex private Long analysisOrchestrationIteration;
  @FdIndex private Long deletePerpetualTaskIteration;

  // TODO: Refactor and Split into separate job instances

  // this stuff is only required for deployment verification
  private Duration dataCollectionDelay;
  private List<String> perpetualTaskIds;
  private Set<String> oldVersionHosts;
  private Set<String> newVersionHosts;
  private Integer newHostsTrafficSplitPercentage;

  // this stuff is only required for health verification
  private Instant preActivityVerificationStartTime;
  private Instant postActivityVerificationStartTime;

  private List<ProgressLog> progressLogs;

  private VerificationJob resolvedJob;

  @Builder.Default
  @FdTtlIndex
  private Date validUntil =
      Date.from(OffsetDateTime.now().plus(CVConstants.VERIFICATION_JOB_INSTANCE_EXPIRY_DURATION).toInstant());

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    if (VerificationJobInstanceKeys.dataCollectionTaskIteration.equals(fieldName)) {
      this.dataCollectionTaskIteration = nextIteration;
      return;
    }
    if (VerificationJobInstanceKeys.analysisOrchestrationIteration.equals(fieldName)) {
      this.analysisOrchestrationIteration = nextIteration;
      return;
    }
    if (VerificationJobInstanceKeys.deletePerpetualTaskIteration.equals(fieldName)) {
      this.deletePerpetualTaskIteration = nextIteration;
      return;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    if (VerificationJobInstanceKeys.dataCollectionTaskIteration.equals(fieldName)) {
      return this.dataCollectionTaskIteration;
    }
    if (VerificationJobInstanceKeys.analysisOrchestrationIteration.equals(fieldName)) {
      return this.analysisOrchestrationIteration;
    }
    if (VerificationJobInstanceKeys.deletePerpetualTaskIteration.equals(fieldName)) {
      return this.deletePerpetualTaskIteration;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  public VerificationJobInstanceDTO toDTO() {
    return VerificationJobInstanceDTO.builder()
        .verificationJobIdentifier(verificationJobIdentifier)
        .deploymentStartTimeMs(deploymentStartTime.toEpochMilli())
        .newHostsTrafficSplitPercentage(newHostsTrafficSplitPercentage)
        .oldVersionHosts(oldVersionHosts)
        .newVersionHosts(newVersionHosts)
        .verificationTaskStartTimeMs(startTime.toEpochMilli())
        .dataCollectionDelayMs(dataCollectionDelay.toMillis())
        .build();
  }

  public Instant getEndTime() {
    return getStartTime().plus(getExecutionDuration());
  }

  public List<ProgressLog> getProgressLogs() {
    if (progressLogs == null) {
      return Collections.emptyList();
    }
    return progressLogs;
  }
  @Data
  @SuperBuilder
  public abstract static class ProgressLog {
    Instant startTime;
    Instant endTime;
    boolean isFinalState;
    String log;
    public abstract ExecutionStatus getVerificationJobExecutionStatus();
    public boolean shouldUpdateJobStatus(VerificationJobInstance verificationJobInstance) {
      return getEndTime().equals(verificationJobInstance.getEndTime()) && isFinalState() || isFailedStatus();
    }

    public abstract boolean isFailedStatus();
  }
  @Value
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  public static class AnalysisProgressLog extends VerificationJobInstance.ProgressLog {
    AnalysisStatus analysisStatus;

    @Override
    public ExecutionStatus getVerificationJobExecutionStatus() {
      return AnalysisStatus.mapToVerificationJobExecutionStatus(analysisStatus);
    }

    @Override
    public boolean isFailedStatus() {
      return AnalysisStatus.getFailedStatuses().contains(analysisStatus);
    }
  }
  @Value
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  public static class DataCollectionProgressLog extends VerificationJobInstance.ProgressLog {
    DataCollectionExecutionStatus executionStatus;

    @Override
    public boolean isFailedStatus() {
      return DataCollectionExecutionStatus.getFailedStatuses().contains(executionStatus);
    }

    @Override
    public ExecutionStatus getVerificationJobExecutionStatus() {
      Preconditions.checkState(DataCollectionExecutionStatus.getFailedStatuses().contains(executionStatus),
          "Final status can only be set for failed status: " + executionStatus);
      return ExecutionStatus.FAILED;
    }
  }

  public enum ExecutionStatus {
    QUEUED,
    RUNNING,
    FAILED,
    SUCCESS,
    TIMEOUT;

    public static List<ExecutionStatus> finalStatuses() {
      return Lists.newArrayList(SUCCESS, FAILED, TIMEOUT);
    }
  }
  private List<ProgressLog> getFinalStateProgressLogs() {
    return getProgressLogs().stream().filter(progressLog -> progressLog.isFinalState).collect(Collectors.toList());
  }
  public int getProgressPercentage() {
    // TODO: Reexamine this logic. This will return 0 for anything that's not marked as a final state.
    List<ProgressLog> finalStateLogs = getFinalStateProgressLogs();
    if (finalStateLogs.isEmpty()) {
      return 0;
    }
    ProgressLog lastProgressLog = finalStateLogs.get(finalStateLogs.size() - 1);
    Instant endTime = lastProgressLog.getEndTime();
    Duration total = getExecutionDuration();
    Duration completedTillNow = Duration.between(getStartTime(), endTime);

    return (int) (completedTillNow.get(ChronoUnit.SECONDS) * 100.0 / total.get(ChronoUnit.SECONDS));
  }

  private Duration getExecutionDuration() {
    Duration jobDuration = getResolvedJob().getDuration();
    if (VerificationJobType.getDeploymentJobTypes().contains(getResolvedJob().getType())) {
      return jobDuration;
    }
    return jobDuration.plus(jobDuration);
  }

  private boolean isHealthJob() {
    return !VerificationJobType.getDeploymentJobTypes().contains(getResolvedJob().getType());
  }

  public Duration getTimeRemainingMs(Instant currentTime) {
    if (isFinalStatus()) {
      return Duration.ZERO;
    } else if (executionStatus == ExecutionStatus.QUEUED) {
      return getResolvedJob().getDuration().plus(Duration.ofMinutes(5));
    } else {
      int percentage = getProgressPercentage();
      if (percentage == 0) {
        return getResolvedJob().getDuration().plus(Duration.ofMinutes(5));
      }
      Instant startTimeForDuration = getStartTime();
      if (!isHealthJob()) {
        startTimeForDuration = Collections.max(Arrays.asList(getStartTime(), Instant.ofEpochMilli(createdAt)));
      }
      Duration durationTillNow = Duration.between(startTimeForDuration, currentTime);

      Duration durationFor1Percent = Duration.ofMillis(durationTillNow.toMillis() / percentage);
      return Duration.ofMillis((100 - percentage) * durationFor1Percent.toMillis());
    }
  }

  private boolean isFinalStatus() {
    return ExecutionStatus.finalStatuses().contains(executionStatus);
  }
}
