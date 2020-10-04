package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SweepingOutputInstance.SweepingOutputKeys;
import io.harness.data.SweepingOutput;
import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.IndexType;
import io.harness.mongo.index.NgUniqueIndex;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.experimental.Wither;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Value
@Builder
@Wither
@NgUniqueIndex(name = "uniquePipelineExecution",
    fields =
    {
      @Field(SweepingOutputKeys.appId), @Field(SweepingOutputKeys.name), @Field(SweepingOutputKeys.pipelineExecutionId)
    })

@NgUniqueIndex(name = "uniqueWorkflowExecution",
    fields =
    {
      @Field(SweepingOutputKeys.appId), @Field(SweepingOutputKeys.name), @Field(SweepingOutputKeys.workflowExecutionIds)
    })
@NgUniqueIndex(name = "uniquePhaseExecution",
    fields =
    { @Field(SweepingOutputKeys.appId)
      , @Field(SweepingOutputKeys.name), @Field(SweepingOutputKeys.phaseExecutionId) })
@NgUniqueIndex(name = "uniqueStateExecution",
    fields =
    { @Field(SweepingOutputKeys.appId)
      , @Field(SweepingOutputKeys.name), @Field(SweepingOutputKeys.stateExecutionId) })
@CdIndex(name = "workflowExecutionIdsNamePrefix",
    fields =
    {
      @Field(SweepingOutputKeys.appId)
      , @Field(SweepingOutputKeys.workflowExecutionIds), @Field(SweepingOutputKeys.name),
          @Field(value = SweepingOutputKeys.createdAt, type = IndexType.DESC)
    })
@CdIndex(name = "phaseExecutionIdNamePrefix",
    fields =
    {
      @Field(SweepingOutputKeys.appId)
      , @Field(SweepingOutputKeys.phaseExecutionId), @Field(SweepingOutputKeys.name),
          @Field(value = SweepingOutputKeys.createdAt, type = IndexType.DESC)
    })
@Entity(value = "sweepingOutput2", noClassnameStored = true)
@HarnessEntity(exportable = false)
public final class SweepingOutputInstance implements PersistentEntity, UuidAccess, CreatedAtAware {
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

  public static final class SweepingOutputKeys {
    public static final String uuid = "uuid";
    public static final String appId = "appId";
    public static final String pipelineExecutionId = "pipelineExecutionId";
    public static final String workflowExecutionIds = "workflowExecutionIds";
    public static final String phaseExecutionId = "phaseExecutionId";
    public static final String stateExecutionId = "stateExecutionId";
    public static final String createdAt = "createdAt";
    public static final String name = "name";
    public static final String value = "value";
    public static final String output = "output";
    public static final String validUntil = "validUntil";
  }
}
