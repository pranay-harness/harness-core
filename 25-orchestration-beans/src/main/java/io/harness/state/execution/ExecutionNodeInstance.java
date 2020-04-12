package io.harness.state.execution;

import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import io.harness.plan.ExecutionNode;
import io.harness.state.execution.status.NodeExecutionStatus;
import io.harness.state.io.ambiance.Ambiance;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;

import javax.validation.constraints.NotNull;

@Data
@Builder
@FieldNameConstants(innerTypeName = "ExecutionNodeInstanceKeys")
@Entity(value = "executionNodeInstances", noClassnameStored = true)
public class ExecutionNodeInstance implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  // Immutable
  @Id String uuid;
  @Indexed long createdAt;
  @NotNull Ambiance ambiance;
  @NotNull ExecutionNode node;
  @Indexed @NotNull String executionInstanceId;

  // Mutable
  long lastUpdatedAt;
  NodeExecutionStatus status;
  private Long startTs;
  private Long endTs;
  private Long expiryTs;
}
