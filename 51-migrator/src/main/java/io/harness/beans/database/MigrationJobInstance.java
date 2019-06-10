package io.harness.beans.database;

import io.harness.beans.migration.MigrationJob;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import lombok.Builder;
import lombok.Data;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@Entity(value = "migrationJobInstances", noClassnameStored = true)
public class MigrationJobInstance implements PersistentEntity, UpdatedAtAware {
  @Id private String id;
  long lastUpdatedAt;

  private MigrationJob.Metadata metadata;

  enum Status { BASELINE, PENDING, FAILED, SUCCEEDED }
  private Status status;
}
