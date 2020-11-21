package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.SweepingOutput;
import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.mongo.index.SortCompoundMongoIndex;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;

import com.google.common.collect.ImmutableList;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.NonFinal;
import lombok.experimental.Wither;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@OwnedBy(CDC)
@Value
@Builder
@Wither
@Entity(value = "sweepingOutput2", noClassnameStored = true)
@HarnessEntity(exportable = false)
@FieldNameConstants(innerTypeName = "SweepingOutputInstanceKeys")
public final class SweepingOutputInstance implements PersistentEntity, UuidAccess, CreatedAtAware {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("uniquePipelineExecution")
                 .unique(true)
                 .field(SweepingOutputInstanceKeys.appId)
                 .field(SweepingOutputInstanceKeys.name)
                 .field(SweepingOutputInstanceKeys.pipelineExecutionId)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("uniqueWorkflowExecution")
                 .unique(true)
                 .field(SweepingOutputInstanceKeys.appId)
                 .field(SweepingOutputInstanceKeys.name)
                 .field(SweepingOutputInstanceKeys.workflowExecutionIds)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("uniquePhaseExecution")
                 .unique(true)
                 .field(SweepingOutputInstanceKeys.appId)
                 .field(SweepingOutputInstanceKeys.name)
                 .field(SweepingOutputInstanceKeys.phaseExecutionId)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("uniqueStateExecution")
                 .unique(true)
                 .field(SweepingOutputInstanceKeys.appId)
                 .field(SweepingOutputInstanceKeys.name)
                 .field(SweepingOutputInstanceKeys.stateExecutionId)
                 .build())
        .add(SortCompoundMongoIndex.builder()
                 .name("workflowExecutionIdsNamePrefix")
                 .field(SweepingOutputInstanceKeys.appId)
                 .field(SweepingOutputInstanceKeys.workflowExecutionIds)
                 .field(SweepingOutputInstanceKeys.name)
                 .descSortField(SweepingOutputInstanceKeys.createdAt)
                 .build())
        .add(SortCompoundMongoIndex.builder()
                 .name("phaseExecutionIdNamePrefix")
                 .field(SweepingOutputInstanceKeys.appId)
                 .field(SweepingOutputInstanceKeys.phaseExecutionId)
                 .field(SweepingOutputInstanceKeys.name)
                 .descSortField(SweepingOutputInstanceKeys.createdAt)
                 .build())
        .build();
  }

  @Id private final String uuid;
  private final String appId;
  private final String pipelineExecutionId;
  @Singular private final List<String> workflowExecutionIds;
  private final String phaseExecutionId;
  private final String stateExecutionId;
  @NonFinal @Setter private long createdAt;

  @NotNull @Trimmed private final String name;

  @Getter private final SweepingOutput value;
  @Deprecated @Getter private final byte[] output;

  public enum Scope { PIPELINE, WORKFLOW, PHASE, STATE }

  @FdTtlIndex private final Date validUntil = Date.from(OffsetDateTime.now().plusMonths(6).toInstant());
}
