package io.harness.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.ambiance.Ambiance;
import io.harness.ambiance.Ambiance.AmbianceKeys;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.status.NodeExecutionStatus;
import io.harness.facilitator.modes.ExecutionMode;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import io.harness.persistence.converters.DurationConverter;
import io.harness.plan.ExecutionNode;
import io.harness.serializer.KryoUtils;
import io.harness.state.io.StateTransput;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
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
@Entity(value = "nodeExecutions")
@Converters({DurationConverter.class})
public final class NodeExecution implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  // Immutable
  @Id String uuid;
  @NotNull Ambiance ambiance;
  @NotNull ExecutionNode node;
  @NotNull ExecutionMode mode;
  @Indexed long createdAt;
  private Long startTs;
  private Long endTs;
  private Duration initialWaitDuration;

  // For Wait Notify
  String notifyId;

  // Relationships
  String parentId;
  String nextId;
  String previousId;

  String taskId;

  // Mutable
  long lastUpdatedAt;
  NodeExecutionStatus status;
  private Long expiryTs;

  // Applicable only for child/children states
  @Singular List<StateTransput> additionalInputs;

  @Singular List<String> retryIds;

  public boolean isRetry() {
    return isNotEmpty(retryIds);
  }

  public int retryCount() {
    if (isRetry()) {
      return retryIds.size();
    }
    return 0;
  }

  public NodeExecution deepCopy() {
    return KryoUtils.clone(this);
  }

  @UtilityClass
  public static final class NodeExecutionKeys {
    public static final String planExecutionId = NodeExecutionKeys.ambiance + "." + AmbianceKeys.planExecutionId;
    public static final String levelRuntimeId = NodeExecutionKeys.ambiance + "." + AmbianceKeys.levelRuntimeId;
    public static final String levelSetupId = NodeExecutionKeys.ambiance + "." + AmbianceKeys.levelSetupId;
    public static final String levelIdentifier = NodeExecutionKeys.ambiance + "." + AmbianceKeys.levelIdentifier;
  }
}
