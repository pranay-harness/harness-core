package io.harness;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.PersistentEntity;

import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Value
@Entity(value = "cdcStateEntity", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "cdcStateEntityKeys")
@StoreIn("change-data-capture")
@OwnedBy(HarnessTeam.CE)
public class CDCStateEntity implements PersistentEntity {
  @Id private String sourceEntityClass;
  private String lastSyncedToken;
}
