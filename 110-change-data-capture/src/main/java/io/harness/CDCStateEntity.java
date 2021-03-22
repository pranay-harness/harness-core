package io.harness;

import io.harness.annotation.StoreIn;
import io.harness.persistence.PersistentEntity;

import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Value
@Slf4j
@StoreIn("change-data-capture")
@Entity(value = "cdcStateEntity", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "cdcStateEntityKeys")
public class CDCStateEntity implements PersistentEntity {
  @Id private String sourceEntityClass;
  private String lastSyncedToken;
}
