package io.harness.dashboard;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.EmbeddedUser;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.NameAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Id;

import javax.validation.constraints.NotNull;

@Data
@Builder
@FieldNameConstants(innerTypeName = "keys")
public class DashboardSettings
    implements NameAccess, PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware, UpdatedAtAware, UpdatedByAware {
  private EmbeddedUser createdBy;
  private EmbeddedUser lastUpdatedBy;
  private long createdAt;
  private long lastUpdatedAt;
  private String accountId;
  private String data;
  private String description;
  private String name;
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
}
