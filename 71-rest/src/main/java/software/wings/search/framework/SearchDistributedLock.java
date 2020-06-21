package software.wings.search.framework;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.IndexOptions;
import io.harness.mongo.index.Indexed;
import io.harness.persistence.CreatedAtAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.validation.Update;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.Date;
import javax.validation.constraints.NotNull;

/**
 * The search lock bean based on
 * Mongo TTL indexes
 *
 * @author utkarsh
 */
@Value
@Builder
@Entity(value = "searchDistributedLocks", noClassnameStored = true)
@HarnessEntity(exportable = false)
@FieldNameConstants(innerTypeName = "SearchDistributedLockKeys")
public class SearchDistributedLock implements PersistentEntity, CreatedAtAccess {
  @Id @NotNull(groups = {Update.class}) private String name;
  @Indexed @NotNull private String uuid;
  @Indexed(options = @IndexOptions(expireAfterSeconds = 70)) @NotNull private Date heartbeat;
  @NotNull private long createdAt;
}
