package io.harness.steps.barriers.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.distribution.barrier.Barrier.State;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import io.harness.steps.barriers.beans.BarrierSetupInfo.BarrierSetupInfoKeys;
import io.harness.steps.barriers.beans.StageDetail.StageDetailKeys;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
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
  @NotNull private BarrierSetupInfo setupInfo;

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

  @UtilityClass
  public static class BarrierExecutionInstanceKeys {
    public static final String stages = BarrierExecutionInstanceKeys.setupInfo + "." + BarrierSetupInfoKeys.stages;
    public static final String stagesIdentifier =
        BarrierExecutionInstanceKeys.setupInfo + "." + BarrierSetupInfoKeys.stages + "." + StageDetailKeys.identifier;
  }

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("planExecutionId_barrierState_stagesIdentifier_idx")
                 .field(BarrierExecutionInstanceKeys.planExecutionId)
                 .field(BarrierExecutionInstanceKeys.barrierState)
                 .field(BarrierExecutionInstanceKeys.stagesIdentifier)
                 .build())
        .build();
  }
}
