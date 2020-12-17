package io.harness.cvng.migration.beans;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;

import com.github.reinert.jjschema.SchemaIgnore;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(value = "cvngSchema", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class CVNGSchema implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  public static final String SCHEMA_ID = "schema";
  public static final String VERSION = "version";
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
  @SchemaIgnore @FdIndex private long createdAt;
  @SchemaIgnore @NotNull private long lastUpdatedAt;

  private int version;

  private CVNGMigrationStatus cvngMigrationStatus;

  @Override
  public String getUuid() {
    return SCHEMA_ID;
  }

  public enum CVNGMigrationStatus { RUNNING, SUCCESS, PENDING, ERROR }
}
