package io.harness.waiter;

import io.harness.beans.ExecutionStatus;
import io.harness.persistence.CreatedAtAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import lombok.Builder;
import lombok.Value;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;

/**
 * Represents which waiter is waiting on which correlation Ids and callback to execute when done.
 */
@Entity(value = "waitInstances", noClassnameStored = true)
@Value
@Builder
public class WaitInstance extends PersistentEntity implements UuidAccess, CreatedAtAccess {
  public static final Duration TTL = WaitQueue.TTL.plusDays(7);

  @Id private String uuid;
  private long createdAt;

  private List<String> correlationIds;

  private NotifyCallback callback;

  private long timeoutMsec;

  private ExecutionStatus status = ExecutionStatus.NEW;

  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plus(TTL).toInstant());
}
