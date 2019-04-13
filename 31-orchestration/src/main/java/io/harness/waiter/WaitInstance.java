package io.harness.waiter;

import io.harness.beans.ExecutionStatus;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
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
@FieldNameConstants(innerTypeName = "WaitInstanceKeys")
public class WaitInstance implements PersistentEntity, UuidAccess {
  public static final String CALLBACK_PROCESSING_AT_KEY = "callbackProcessingAt";
  public static final String STATUS_KEY = "status";

  public static final Duration TTL = WaitQueue.TTL.plusDays(7);
  public static final Duration AfterFinishTTL = Duration.ofHours(1);

  public static final String VALID_UNTIL_KEY = "validUntil";

  @Id private String uuid;
  private List<String> correlationIds;
  private NotifyCallback callback;

  // TODO: seems unused anymore
  private long timeoutMsec;

  private ExecutionStatus status = ExecutionStatus.NEW;
  private long callbackProcessingAt;

  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plus(TTL).toInstant());
}
