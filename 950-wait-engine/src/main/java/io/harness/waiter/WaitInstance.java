package io.harness.waiter;

import static java.time.Duration.ofDays;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

/**
 * Represents which waiter is waiting on which correlation Ids and callback to execute when done.
 */
@Value
@Builder
@FieldNameConstants(innerTypeName = "WaitInstanceKeys")
@Entity(value = "waitInstances", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class WaitInstance implements PersistentEntity, UuidAccess {
  public static final Duration TTL = ofDays(30);

  @Id private String uuid;
  @FdIndex private List<String> correlationIds;
  @FdIndex private List<String> waitingOnCorrelationIds;
  private String publisher;

  private NotifyCallback callback;
  private long callbackProcessingAt;

  private ProgressCallback progressCallback;

  @Default @FdTtlIndex private Date validUntil = Date.from(OffsetDateTime.now().plus(TTL).toInstant());
}
