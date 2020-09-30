package io.harness.cvng.verificationjob.entities;

import com.google.common.collect.Lists;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;
import io.harness.cvng.CVConstants;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.cvng.verificationjob.beans.VerificationJobInstanceDTO;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;

@Data
@Builder
@FieldNameConstants(innerTypeName = "VerificationJobInstanceKeys")
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "verificationJobInstances", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class VerificationJobInstance
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess, PersistentRegularIterable {
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
  private Duration dataCollectionDelay;
  private List<String> perpetualTaskIds;
  private Set<String> oldVersionHosts;
  private Set<String> newVersionHosts;
  private Integer newHostsTrafficSplitPercentage;
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
    return getStartTime().plus(resolvedJob.getDuration());
  }

  public List<ProgressLog> getProgressLogs() {
    if (progressLogs == null) {
      return Collections.emptyList();
    }
    return progressLogs;
  }
  @Value
  @Builder
  public static class ProgressLog {
    AnalysisStatus analysisStatus;
    Instant startTime;
    Instant endTime;
    boolean isFinalState;
    String log;
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

  public int getProgressPercentage() {
    if (getProgressLogs().isEmpty()) {
      return 0;
    }
    ProgressLog lastProgressLog = getProgressLogs().get(getProgressLogs().size() - 1);
    Instant endTime = lastProgressLog.getEndTime();
    Duration total = getResolvedJob().getDuration();
    Duration completedTillNow = Duration.between(getStartTime(), endTime);

    return (int) (completedTillNow.get(ChronoUnit.MILLIS) * 100.0 / total.get(ChronoUnit.MILLIS));
  }

  public Duration getTimeRemainingMs(Instant currentTime) {
    if (ExecutionStatus.finalStatuses().contains(executionStatus)) {
      return Duration.ZERO;
    } else if (executionStatus == ExecutionStatus.QUEUED) {
      return getResolvedJob().getDuration().plus(Duration.ofMinutes(5));
    } else {
      int percentage = getProgressPercentage();
      if (percentage == 0) {
        return getResolvedJob().getDuration().plus(Duration.ofMinutes(5));
      }
      Duration durationTillNow = Duration.between(getStartTime(), currentTime);
      Duration durationFor1Percent = Duration.ofMillis(durationTillNow.toMillis() / percentage);
      return Duration.ofMillis((100 - percentage) * durationFor1Percent.toMillis());
    }
  }
}
