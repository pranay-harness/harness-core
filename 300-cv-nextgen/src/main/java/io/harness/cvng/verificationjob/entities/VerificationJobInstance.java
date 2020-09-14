package io.harness.cvng.verificationjob.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;
import io.harness.cvng.analysis.beans.ExecutionStatus;
import io.harness.cvng.statemachine.entities.AnalysisStatus;
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
  private String verificationJobId;
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
  private Duration duration;
  private List<ProgressLog> progressLogs;
  @Builder.Default @FdTtlIndex private Date validUntil = Date.from(OffsetDateTime.now().plusDays(31).toInstant());
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
    return getStartTime().plus(getDuration());
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
}
