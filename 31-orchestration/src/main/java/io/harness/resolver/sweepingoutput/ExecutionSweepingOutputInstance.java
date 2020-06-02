package io.harness.resolver.sweepingoutput;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Level;
import io.harness.annotation.HarnessEntity;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SweepingOutput;
import io.harness.beans.SweepingOutputInstance.SweepingOutputConverter;
import io.harness.data.validator.Trimmed;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import io.harness.resolver.sweepingoutput.ExecutionSweepingOutputInstance.ExecutionSweepingOutputKeys;
import io.harness.resolvers.ResolverUtils;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
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
import org.mongodb.morphia.annotations.PrePersist;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;

@OwnedBy(CDC)
@Redesign
@Value
@Builder
@Indexes({
  @Index(options = @IndexOptions(name = "levelRuntimeIdUniqueIdx", unique = true), fields = {
    @Field(ExecutionSweepingOutputKeys.planExecutionId)
    , @Field(ExecutionSweepingOutputKeys.levelRuntimeIdIdx), @Field(ExecutionSweepingOutputKeys.name)
  })
})
@Entity(value = "executionSweepingOutput", noClassnameStored = true)
@HarnessEntity(exportable = false)
@Converters({SweepingOutputConverter.class})
@FieldNameConstants(innerTypeName = "ExecutionSweepingOutputKeys")
public class ExecutionSweepingOutputInstance implements PersistentEntity, UuidAware, CreatedAtAware {
  @Id @NonFinal @Setter String uuid;
  @NonNull String planExecutionId;
  @Singular List<Level> levels;
  @NonNull @Trimmed String name;
  @NonFinal String levelRuntimeIdIdx;

  @Getter SweepingOutput value;
  @NonFinal @Setter long createdAt;

  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  @Builder.Default
  Date validUntil = Date.from(OffsetDateTime.now().plusMonths(6).toInstant());

  @PrePersist
  void populateLevelRuntimeIdIdx() {
    levelRuntimeIdIdx = ResolverUtils.prepareLevelRuntimeIdIdx(levels);
  }
}
