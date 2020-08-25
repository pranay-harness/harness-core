package io.harness.cvng.verificationjob.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;
import io.harness.cvng.analysis.beans.ExecutionStatus;
import io.harness.cvng.verificationjob.beans.DeploymentVerificationTaskDTO;
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
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;

@Data
@Builder
@FieldNameConstants(innerTypeName = "VerificationTaskKeys")
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "deploymentVerificationTasks")
@HarnessEntity(exportable = true)
public class DeploymentVerificationTask
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess, PersistentRegularIterable {
  @Id private String uuid;
  @NotNull @FdIndex private String accountId;
  private long createdAt;
  private long lastUpdatedAt;
  private ExecutionStatus executionStatus;
  private String verificationJobId;
  private String verificationJobIdentifier;
  private Instant deploymentStartTime;
  private Instant verificationTaskStartTime;
  @FdIndex private long dataCollectionTaskIteration;
  @FdIndex private Long analysisOrchestrationIteration;
  private Duration dataCollectionDelay;
  private List<String> dataCollectionTaskIds;
  private Set<String> oldVersionHosts;
  private Set<String> newVersionHosts;
  private Integer newHostsTrafficSplitPercentage;
  @Builder.Default @FdTtlIndex private Date validUntil = Date.from(OffsetDateTime.now().plusDays(31).toInstant());
  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    if (VerificationTaskKeys.dataCollectionTaskIteration.equals(fieldName)) {
      this.dataCollectionTaskIteration = nextIteration;
      return;
    }
    if (VerificationTaskKeys.analysisOrchestrationIteration.equals(fieldName)) {
      this.analysisOrchestrationIteration = nextIteration;
      return;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    if (VerificationTaskKeys.dataCollectionTaskIteration.equals(fieldName)) {
      return this.dataCollectionTaskIteration;
    }
    if (VerificationTaskKeys.analysisOrchestrationIteration.equals(fieldName)) {
      return this.analysisOrchestrationIteration;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  public DeploymentVerificationTaskDTO toDTO() {
    return DeploymentVerificationTaskDTO.builder()
        .verificationJobIdentifier(verificationJobIdentifier)
        .deploymentStartTimeMs(deploymentStartTime.toEpochMilli())
        .newHostsTrafficSplitPercentage(newHostsTrafficSplitPercentage)
        .oldVersionHosts(oldVersionHosts)
        .newVersionHosts(newVersionHosts)
        .verificationTaskStartTimeMs(verificationTaskStartTime.toEpochMilli())
        .dataCollectionDelayMs(dataCollectionDelay.toMillis())
        .build();
  }
}
