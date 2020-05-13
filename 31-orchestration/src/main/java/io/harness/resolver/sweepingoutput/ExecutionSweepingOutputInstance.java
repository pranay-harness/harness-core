package io.harness.resolver.sweepingoutput;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.ambiance.Ambiance.AmbianceKeys;
import io.harness.ambiance.LevelExecution;
import io.harness.annotation.HarnessEntity;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SweepingOutput;
import io.harness.beans.SweepingOutputInstance.SweepingOutputConverter;
import io.harness.data.structure.EmptyPredicate;
import io.harness.data.validator.Trimmed;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import io.harness.resolver.sweepingoutput.ExecutionSweepingOutputInstance.ExecutionSweepingOutputKeys;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
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
import java.util.stream.Collectors;

@OwnedBy(CDC)
@Redesign
@Value
@Builder
@Indexes({
  @Index(options = @IndexOptions(name = "levelRuntimeIdUniqueIdx", unique = true), fields = {
    @Field(ExecutionSweepingOutputKeys.ambiance + "." + AmbianceKeys.planExecutionId)
    , @Field(ExecutionSweepingOutputKeys.name), @Field(ExecutionSweepingOutputKeys.levelRuntimeIdIdx)
  })
})
@Entity(value = "executionSweepingOutput", noClassnameStored = true)
@HarnessEntity(exportable = false)
@Converters({SweepingOutputConverter.class})
@FieldNameConstants(innerTypeName = "ExecutionSweepingOutputKeys")
public class ExecutionSweepingOutputInstance implements PersistentEntity, UuidAccess, CreatedAtAware {
  @Id String uuid;
  @NonNull Ambiance ambiance;
  @NonNull @Trimmed String name;
  @NonFinal String levelRuntimeIdIdx;

  @Getter SweepingOutput value;

  @NonFinal @Setter long createdAt;

  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  @Builder.Default
  Date validUntil = Date.from(OffsetDateTime.now().plusMonths(6).toInstant());

  @PrePersist
  void populateLevelRuntimeIdIdx() {
    levelRuntimeIdIdx = prepareLevelRuntimeIdIdx(ambiance.getLevelExecutions());
  }

  public static String prepareLevelRuntimeIdIdx(List<LevelExecution> levelExecutions) {
    return EmptyPredicate.isEmpty(levelExecutions)
        ? ""
        : levelExecutions.stream().map(LevelExecution::getRuntimeId).collect(Collectors.joining("|"));
  }
}
