package io.harness.data;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Level;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.NgUniqueIndex;
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
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@OwnedBy(CDC)
@Value
@Builder
@NgUniqueIndex(name = "levelRuntimeIdUniqueIdx",
    fields = { @Field("planExecutionId")
               , @Field("levelRuntimeIdIdx"), @Field("name") })
@CdIndex(
    name = "producedBySetupIdIdx", fields = { @Field("planExecutionId")
                                              , @Field("producedBy.setupId"), @Field("name") })
@CdIndex(name = "planExecutionIdIdx", fields = { @Field("planExecutionId") })
@Entity(value = "outcomeInstances")
@Document("outcomeInstances")
@FieldNameConstants(innerTypeName = "OutcomeInstanceKeys")
public class OutcomeInstance implements PersistentEntity, UuidAccess {
  @Wither @Id @org.mongodb.morphia.annotations.Id String uuid;
  @NotEmpty String planExecutionId;
  @Singular List<Level> levels;
  Level producedBy;
  @NotEmpty @Trimmed String name;
  String levelRuntimeIdIdx;

  Outcome outcome;
  @Wither @CreatedDate Long createdAt;
  @Wither @Version Long version;
}
