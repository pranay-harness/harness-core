package io.harness.migration;

import static io.harness.migration.MigrationJobInstance.COLLECTION_NAME;

import io.harness.annotation.HarnessEntity;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;

import java.util.EnumSet;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@Entity(value = COLLECTION_NAME, noClassnameStored = true)
@HarnessEntity(exportable = false)
@FieldNameConstants(innerTypeName = "MigrationJobInstanceKeys")
public final class MigrationJobInstance implements PersistentEntity, UpdatedAtAware {
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
