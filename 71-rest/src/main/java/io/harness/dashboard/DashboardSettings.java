package io.harness.dashboard;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
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
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Transient;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@Builder
@FieldNameConstants(innerTypeName = "keys")
@Entity(value = "dashboardSettings", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class DashboardSettings
    implements NameAccess, PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware, UpdatedAtAware, UpdatedByAware {
  private EmbeddedUser createdBy;
  private EmbeddedUser lastUpdatedBy;
  private long createdAt;
  private long lastUpdatedAt;
  @Indexed private String accountId;
  private String data;
  private String description;
  private String name;
  @Transient private boolean isOwner;
  @Transient private boolean isShared;
  @Transient private boolean canUpdate;
  @Transient private boolean canDelete;
  @Transient private boolean canManage;
  private List<DashboardAccessPermissions> permissions;
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
}
