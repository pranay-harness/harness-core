package io.harness.waiter;

import static java.time.Duration.ofDays;

import io.harness.annotation.HarnessEntity;
import io.harness.beans.ExecutionStatus;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import io.harness.waiter.WaitInstance.WaitInstanceKeys;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;

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
@Indexes(@Index(options = @IndexOptions(name = "index"),
    fields = { @Field(WaitInstanceKeys.status)
               , @Field(WaitInstanceKeys.correlationIds) }))
@HarnessEntity(exportable = false)
public class WaitInstance implements PersistentEntity, UuidAccess {
  public static final Duration TTL = ofDays(21);
  public static final Duration AfterFinishTTL = Duration.ofHours(1);

  @Id private String uuid;
  private List<String> correlationIds;
  @Indexed private List<String> waitingOnCorrelationIds;
  private NotifyCallback callback;

  private ExecutionStatus status = ExecutionStatus.NEW;
  private long callbackProcessingAt;

  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plus(TTL).toInstant());
}
