package io.harness;

import io.harness.persistence.PersistentEntity;

import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Id;

@Value
@Slf4j
@FieldNameConstants(innerTypeName = "cdcStateEntityKeys")
public class CDCStateEntity implements PersistentEntity {
  @Id private String sourceEntityClass;
  private String lastSyncedToken;
}
