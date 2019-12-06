package io.harness.waiter;

import static java.time.Duration.ofDays;

import io.harness.annotation.HarnessEntity;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import lombok.Builder;
import lombok.Builder.Default;
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
@Value
@Builder
@FieldNameConstants(innerTypeName = "WaitInstanceKeys")
@Entity(value = "waitInstances", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class WaitInstance implements PersistentEntity, UuidAccess {
  public static final Duration TTL = ofDays(21);

  @Id private String uuid;
  @Indexed private List<String> correlationIds;
  @Indexed private List<String> waitingOnCorrelationIds;
  private String publisher;
  private NotifyCallback callback;
  private long callbackProcessingAt;

  @Default
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plus(TTL).toInstant());
}
