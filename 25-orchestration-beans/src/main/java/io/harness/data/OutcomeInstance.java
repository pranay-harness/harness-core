package io.harness.data;

import io.harness.ambiance.Ambiance;
import io.harness.data.validator.Trimmed;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import javax.validation.constraints.NotNull;

@Value
@Builder
@Entity(value = "outcomes", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "OutcomeInstanceKeys")
public class OutcomeInstance implements PersistentEntity, UuidAccess {
  @Id @NotNull String uuid;
  @Trimmed @NotNull String name;
  @NotNull Ambiance ambiance;
  @NotNull Outcome outcome;
}
