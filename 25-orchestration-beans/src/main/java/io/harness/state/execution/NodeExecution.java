package io.harness.state.execution;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.facilitate.modes.ExecutionMode;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import io.harness.persistence.converters.DurationConverter;
import io.harness.plan.ExecutionNode;
import io.harness.state.execution.status.NodeExecutionStatus;
import io.harness.state.io.StateTransput;
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

@Data
@Builder
@Redesign
@FieldNameConstants(innerTypeName = "NodeExecutionKeys")
@Entity(value = "nodeExecutions", noClassnameStored = true)
@Converters({DurationConverter.class})
public class NodeExecution implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
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

  // Mutable
  long lastUpdatedAt;
  NodeExecutionStatus status;
  private Long expiryTs;

  // Applicable only for child/children states
  @Singular List<StateTransput> additionalInputs;
}
