package io.harness.data;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Level;
import io.harness.ambiance.Level.LevelKeys;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.OutcomeInstance.OutcomeInstanceKeys;
import io.harness.data.validator.Trimmed;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import io.harness.resolvers.ResolverUtils;
import lombok.Builder;
import lombok.NonNull;
import lombok.Setter;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.NonFinal;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.PrePersist;

import java.util.List;

@OwnedBy(CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "OutcomeInstanceKeys")
@Indexes({
  @Index(options = @IndexOptions(name = "levelRuntimeIdUniqueIdx", unique = true),
      fields =
      {
        @Field(OutcomeInstanceKeys.planExecutionId)
        , @Field(OutcomeInstanceKeys.levelRuntimeIdIdx), @Field(OutcomeInstanceKeys.name)
      })
  ,
      @Index(options = @IndexOptions(name = "producedBySetupIdIdx"), fields = {
        @Field(OutcomeInstanceKeys.planExecutionId)
        , @Field(OutcomeInstanceKeys.producedBy + "." + LevelKeys.setupId), @Field(OutcomeInstanceKeys.name)
      }), @Index(options = @IndexOptions(name = "planExecutionIdIdx"), fields = {
        @Field(OutcomeInstanceKeys.planExecutionId)
      })
})
@Entity(value = "outcomeInstances", noClassnameStored = true)
public class OutcomeInstance implements PersistentEntity, UuidAware, CreatedAtAware {
  @Id @NonFinal @Setter String uuid;
  @NonNull String planExecutionId;
  @Singular List<Level> levels;
  Level producedBy;
  @NonNull @Trimmed String name;
  @NonFinal String levelRuntimeIdIdx;

  Outcome outcome;
  @NonFinal @Setter long createdAt;

  @PrePersist
  void populateLevelRuntimeIdIdx() {
    levelRuntimeIdIdx = ResolverUtils.prepareLevelRuntimeIdIdx(levels);
  }
}
