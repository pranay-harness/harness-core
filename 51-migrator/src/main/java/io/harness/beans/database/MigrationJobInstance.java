package io.harness.beans.database;

import static io.harness.beans.database.MigrationJobInstance.COLLECTION_NAME;

import io.harness.beans.migration.MigrationJob;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.EnumSet;
import java.util.Set;

@Data
@Builder
@Entity(value = COLLECTION_NAME, noClassnameStored = true)
@FieldNameConstants(innerTypeName = "MigrationJobInstanceKeys")
public class MigrationJobInstance implements PersistentEntity, UpdatedAtAware {
  public static final String COLLECTION_NAME = "migrationJobInstances";

  @Id private String id;
  long lastUpdatedAt;

  private MigrationJob.Metadata metadata;

  public enum Status {
    BASELINE,
    PENDING,
    FAILED,
    SUCCEEDED;

    private static Set<Status> finalStatuses = EnumSet.<Status>of(BASELINE, FAILED, SUCCEEDED);

    public static boolean isFinalStatus(Status status) {
      return status != null && finalStatuses.contains(status);
    }
  }

  private Status status;
}
