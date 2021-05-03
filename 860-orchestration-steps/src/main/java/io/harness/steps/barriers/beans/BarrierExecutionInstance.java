package io.harness.steps.barriers.beans;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.distribution.barrier.Barrier.State;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import io.harness.steps.barriers.beans.BarrierPositionInfo.BarrierPosition.BarrierPositionKeys;
import io.harness.steps.barriers.beans.BarrierPositionInfo.BarrierPositionInfoKeys;
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

@OwnedBy(PIPELINE)
@Data
@Builder
@FieldNameConstants(innerTypeName = "BarrierExecutionInstanceKeys")
@Entity(value = "barrierExecutionInstances")
@Document("barrierExecutionInstances")
@TypeAlias("barrierExecutionInstance")
@StoreIn(DbAliases.PMS)
public final class BarrierExecutionInstance implements PersistentEntity, UuidAware, PersistentRegularIterable {
  @Id @org.mongodb.morphia.annotations.Id private String uuid;

  @NotNull private String name;
  @NotNull private String identifier;
  @NotNull private String planExecutionId;
  @NotNull private State barrierState;
  @NotNull private BarrierSetupInfo setupInfo;
  private BarrierPositionInfo positionInfo;

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
    public static final String positions =
        BarrierExecutionInstanceKeys.positionInfo + "." + BarrierPositionInfoKeys.barrierPositionList;

    public static final String stagePositionSetupId = positions + "." + BarrierPositionKeys.stageSetupId;
    public static final String stepGroupPositionSetupId = positions + "." + BarrierPositionKeys.stepGroupSetupId;
    public static final String stepPositionSetupId = positions + "." + BarrierPositionKeys.stepSetupId;
  }

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("planExecutionId_barrierState_stagesIdentifier_idx")
                 .field(BarrierExecutionInstanceKeys.planExecutionId)
                 .field(BarrierExecutionInstanceKeys.barrierState)
                 .field(BarrierExecutionInstanceKeys.stagesIdentifier)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("unique_identifier_planExecutionId_idx")
                 .field(BarrierExecutionInstanceKeys.identifier)
                 .field(BarrierExecutionInstanceKeys.planExecutionId)
                 .unique(true)
                 .build())
        .build();
  }
}
