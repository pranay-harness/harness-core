package io.harness.data;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Level;
import io.harness.ambiance.Level.LevelKeys;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.OutcomeInstance.OutcomeInstanceKeys;
import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.CdUniqueIndex;
import io.harness.mongo.index.Field;
import io.harness.persistence.CreatedAtAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.Wither;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@OwnedBy(CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "OutcomeInstanceKeys")

@CdUniqueIndex(name = "levelRuntimeIdUniqueIdx",
    fields =
    {
      @Field(OutcomeInstanceKeys.planExecutionId)
      , @Field(OutcomeInstanceKeys.levelRuntimeIdIdx), @Field(OutcomeInstanceKeys.name)
    })
@CdIndex(name = "producedBySetupIdIdx",
    fields =
    {
      @Field(OutcomeInstanceKeys.planExecutionId)
      , @Field(OutcomeInstanceKeys.producedBy + "." + LevelKeys.setupId), @Field(OutcomeInstanceKeys.name)
    })
@CdIndex(name = "planExecutionIdIdx", fields = { @Field(OutcomeInstanceKeys.planExecutionId) })
@Entity(value = "outcomeInstances")
@Document("outcomeInstances")
@TypeAlias("outcomeInstances")
public class OutcomeInstance implements PersistentEntity, UuidAccess, CreatedAtAccess {
  @Wither @Id @org.mongodb.morphia.annotations.Id String uuid;
  @NotEmpty String planExecutionId;
  @Singular List<Level> levels;
  Level producedBy;
  @NotEmpty @Trimmed String name;
  String levelRuntimeIdIdx;

  Outcome outcome;
  @Wither @CreatedDate long createdAt;
}
