package io.harness.steps.barriers.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.distribution.barrier.Barrier.State;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.Wither;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(CDC)
@Data
@Builder
@FieldNameConstants(innerTypeName = "BarrierExecutionInstanceKeys")
@Entity(value = "barrierExecutionInstances")
@Document("barrierExecutionInstances")
@TypeAlias("barrierExecutionInstance")
public final class BarrierExecutionInstance implements PersistentEntity, UuidAware, PersistentRegularIterable {
  @Id @org.mongodb.morphia.annotations.Id private String uuid;

  @NotNull private String name;
  @NotNull private String planNodeId;
  @NotNull private String identifier;
  @NotNull private String planExecutionId;
  @NotNull private State barrierState;
  @NotNull private String barrierGroupId;

  @Builder.Default private long expiredIn = 600_000; // 10 minutes

  private Long nextIteration;

  // audit fields
  @Wither @FdIndex @CreatedDate Long createdAt;
  @Wither @LastModifiedDate Long lastUpdatedAt;
  @Version Long version;

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    this.nextIteration = nextIteration;
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    return nextIteration;
  }
}
