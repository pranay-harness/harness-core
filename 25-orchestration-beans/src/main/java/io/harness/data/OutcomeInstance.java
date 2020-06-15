package io.harness.data;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Level;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.Trimmed;
import io.harness.persistence.CreatedAtAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.Wither;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@OwnedBy(CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "OutcomeInstanceKeys")
@CompoundIndexes({
  @CompoundIndex(def = "{'planExecutionId': 1, 'levelRuntimeIdIdx': 1, 'name': 1}", name = "levelRuntimeIdUniqueIdx",
      unique = true)
  ,
      @CompoundIndex(def = "{'planExecutionId': 1, 'producedBy.setupId': 1, 'name': 1}", name = "producedBySetupIdIdx"),
})
@Document("outcomeInstances")
@TypeAlias("outcomeInstances")
public class OutcomeInstance implements PersistentEntity, UuidAccess, CreatedAtAccess {
  @Wither @Id String uuid;
  @Indexed(name = "planExecutionIdIdx") @NotEmpty String planExecutionId;
  @Singular List<Level> levels;
  Level producedBy;
  @NotEmpty @Trimmed String name;
  String levelRuntimeIdIdx;

  Outcome outcome;
  @Wither @CreatedDate long createdAt;
}
