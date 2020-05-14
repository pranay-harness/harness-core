package software.wings.service.impl.instance;

import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAccess;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAccess;
import io.harness.persistence.UuidAware;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;

import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "InstanceSyncPerpetualTaskInfoKeys")
@Entity(value = "instanceSyncPerpetualTasksInfo", noClassnameStored = true)
public class InstanceSyncPerpetualTaskInfo implements PersistentEntity, UuidAware, UuidAccess, AccountAccess,
                                                      CreatedAtAccess, CreatedAtAware, UpdatedAtAware, UpdatedAtAccess {
  @Id String uuid;
  @Indexed String accountId;
  @Indexed String infrastructureMappingId;
  @Singular List<String> perpetualTaskIds;
  long createdAt;
  long lastUpdatedAt;
}
