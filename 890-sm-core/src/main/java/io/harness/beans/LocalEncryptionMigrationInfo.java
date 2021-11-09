package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.FdUniqueIndex;
import io.harness.persistence.PersistentEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;

@OwnedBy(PL)
@Data
@SuperBuilder
@NoArgsConstructor
@FieldNameConstants(innerTypeName = "LocalEncryptionMigrationInfoKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "LocalEncryptionMigrationInfo", noClassnameStored = true)
public class LocalEncryptionMigrationInfo implements PersistentEntity {
  @Id @org.mongodb.morphia.annotations.Id private String uuid;
  @FdUniqueIndex private String accountId;
  private String lastMigratedRecordId;
  private String lastMigratedRecordCreatedAtTimestamp;
}
