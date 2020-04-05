package io.harness.beans;

import io.harness.annotation.HarnessEntity;
import io.harness.beans.SweepingOutputInstance.SweepingOutputConverter;
import io.harness.beans.SweepingOutputInstance.SweepingOutputKeys;
import io.harness.data.validator.Trimmed;
import io.harness.mongo.KryoConverter;
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
import org.mongodb.morphia.annotations.Converters;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.converters.SimpleValueConverter;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import javax.validation.constraints.NotNull;

@Value
@Builder
@Indexes({
  @Index(options = @IndexOptions(name = "uniquePipelineExecution", unique = true),
      fields =
      {
        @Field(SweepingOutputKeys.appId)
        , @Field(SweepingOutputKeys.name), @Field(SweepingOutputKeys.pipelineExecutionId)
      })
  ,
      @Index(options = @IndexOptions(name = "uniqueWorkflowExecution", unique = true), fields = {
        @Field(SweepingOutputKeys.appId)
        , @Field(SweepingOutputKeys.name), @Field(SweepingOutputKeys.workflowExecutionIds)
      }), @Index(options = @IndexOptions(name = "uniquePhaseExecution", unique = true), fields = {
        @Field(SweepingOutputKeys.appId), @Field(SweepingOutputKeys.name), @Field(SweepingOutputKeys.phaseExecutionId)
      }), @Index(options = @IndexOptions(name = "uniqueStateExecution", unique = true), fields = {
        @Field(SweepingOutputKeys.appId), @Field(SweepingOutputKeys.name), @Field(SweepingOutputKeys.stateExecutionId)
      })
})
@Entity(value = "sweepingOutput2", noClassnameStored = true)
@HarnessEntity(exportable = false)
@Converters({SweepingOutputConverter.class})
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

  public static class SweepingOutputConverter extends KryoConverter implements SimpleValueConverter {
    public SweepingOutputConverter() {
      super(SweepingOutput.class);
    }
  }

  @Getter private SweepingOutput value;
  @Deprecated @Getter private byte[] output;

  public enum Scope { PIPELINE, WORKFLOW, PHASE, STATE }

  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(6).toInstant());
}
