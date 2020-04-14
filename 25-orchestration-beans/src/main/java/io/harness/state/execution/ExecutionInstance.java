package io.harness.state.execution;

import io.harness.annotations.Redesign;
import io.harness.beans.EmbeddedUser;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.persistence.CreatedAtAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAccess;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAccess;
import io.harness.plan.ExecutionPlan;
import io.harness.state.execution.status.ExecutionInstanceStatus;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import javax.validation.constraints.NotNull;

@Data
@Builder
@Redesign
@FieldNameConstants(innerTypeName = "ExecutionInstanceKeys")
@Entity(value = "executionInstances", noClassnameStored = true)
public class ExecutionInstance implements PersistentRegularIterable, CreatedAtAware, CreatedAtAccess, CreatedByAware,
                                          CreatedByAccess, UpdatedAtAware, UuidAccess {
  @Id @NotNull private String uuid;
  private EmbeddedUser createdBy;
  private long createdAt;
  private ExecutionPlan executionPlan;
  private Long nextIteration;

  ExecutionInstanceStatus status;
  private Long startTs;
  private Long endTs;
  private Long expiryTs;
  long lastUpdatedAt;

  @Override
  public void updateNextIteration(String fieldName, Long nextIteration) {
    this.nextIteration = nextIteration;
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    return nextIteration;
  }
}
