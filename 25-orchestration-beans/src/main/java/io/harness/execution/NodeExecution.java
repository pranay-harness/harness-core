package io.harness.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.ambiance.Level;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.status.Status;
import io.harness.facilitator.modes.ExecutableResponse;
import io.harness.facilitator.modes.ExecutionMode;
import io.harness.interrupts.InterruptEffect;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import io.harness.persistence.converters.DurationConverter;
import io.harness.plan.PlanNode;
import io.harness.serializer.KryoUtils;
import io.harness.state.io.FailureInfo;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepTransput;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Converters;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;

import java.time.Duration;
import java.util.List;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Data
@Builder
@Redesign
@FieldNameConstants(innerTypeName = "NodeExecutionKeys")
@Converters({DurationConverter.class})
@Entity(value = "nodeExecutions")
public final class NodeExecution implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  // Immutable
  @Id String uuid;
  @NotNull String planExecutionId;
  @Singular List<Level> levels;
  @NotNull PlanNode node;
  @NotNull ExecutionMode mode;
  @Indexed long createdAt;
  private Long startTs;
  private Long endTs;
  private Duration initialWaitDuration;

  // Resolved StepParameters stored just before invoking step.
  StepParameters resolvedStepParameters;

  // For Wait Notify
  String notifyId;

  // Relationships
  String parentId;
  String nextId;
  String previousId;

  // Mutable
  long lastUpdatedAt;
  Status status;
  private Long expiryTs;

  @Singular List<ExecutableResponse> executableResponses;
  @Singular private List<InterruptEffect> interruptHistories;
  @Singular List<StepTransput> additionalInputs;
  FailureInfo failureInfo;

  @Singular List<String> retryIds;
  boolean oldRetry;

  public boolean isRetry() {
    return !isEmpty(retryIds);
  }

  public int retryCount() {
    if (isRetry()) {
      return retryIds.size();
    }
    return 0;
  }

  public boolean isChildSpawningMode() {
    return mode == ExecutionMode.CHILD || mode == ExecutionMode.CHILDREN || mode == ExecutionMode.CHILD_CHAIN;
  }

  public boolean isTaskSpawningMode() {
    return mode == ExecutionMode.TASK || mode == ExecutionMode.TASK_CHAIN;
  }

  public ExecutableResponse obtainLatestExecutableResponse() {
    if (isEmpty(executableResponses)) {
      return null;
    }
    return executableResponses.get(executableResponses.size() - 1);
  }

  public NodeExecution deepCopy() {
    return KryoUtils.clone(this);
  }
}
