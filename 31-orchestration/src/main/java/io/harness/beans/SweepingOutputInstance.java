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
import lombok.experimental.FieldNameConstants;
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
@FieldNameConstants(innerTypeName = "SweepingOutputKeys")
public class SweepingOutputInstance implements PersistentEntity, UuidAccess, CreatedAtAware {
  @Id private String uuid;
  private String appId;
  private String pipelineExecutionId;
  @Singular private List<String> workflowExecutionIds;
  private String phaseExecutionId;
  private String stateExecutionId;
  @NonFinal @Setter private long createdAt;

  @NotNull @Trimmed private String name;

  @Getter private SweepingOutput value;
  @Deprecated @Getter private byte[] output;

  public enum Scope { PIPELINE, WORKFLOW, PHASE, STATE }

  @FdTtlIndex private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(6).toInstant());
}
