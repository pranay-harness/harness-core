package io.harness.cvng.analysis.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;
import io.harness.cvng.analysis.beans.ExecutionStatus;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.time.Instant;

@Data
@NoArgsConstructor
@FieldNameConstants(innerTypeName = "LearningEngineTaskKeys")
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "learningEngineTasks")
@HarnessEntity(exportable = true)
public abstract class LearningEngineTask implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  @Id private String uuid;
  private String cvConfigId;
  private long createdAt;
  private long lastUpdatedAt;
  private String accountId;
  private LearningEngineTaskType analysisType;
  private int taskPriority = 1;
  private String analysisSaveUrl;
  private String failureUrl;
  private Instant analysisStartTime;
  private Instant analysisEndTime;
  private long analysisEndEpochMinute; // This is temporary. LE needs it for now.
  private ExecutionStatus taskStatus;
  public abstract LearningEngineTaskType getType();

  public enum LearningEngineTaskType {
    SERVICE_GUARD_TIME_SERIES,
    SERVICE_GUARD_LOG_CLUSTER,
    SERVICE_GUARD_LOG_ANALYSIS,
    SERVICE_GUARD_FEEDBACK_ANALYSIS;
  }
}
