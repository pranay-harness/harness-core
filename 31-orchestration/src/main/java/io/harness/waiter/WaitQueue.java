package io.harness.waiter;

import static java.time.Duration.ofDays;

import io.harness.annotation.HarnessEntity;
import io.harness.persistence.CreatedAtAccess;
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
import javax.validation.constraints.NotNull;

/**
 * Represents WaitQueue.
 *
 * @author Rishi
 */
@Value
@Builder
@FieldNameConstants(innerTypeName = "WaitQueueKeys")
@Entity(value = "waitQueues", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class WaitQueue implements PersistentEntity, UuidAccess, CreatedAtAccess {
  public static final Duration TTL = ofDays(14);

  @Id @NotNull private String uuid;
  @Indexed @NotNull private long createdAt;

  @Indexed @NotNull private String waitInstanceId;
  @Indexed @NotNull private String correlationId;

  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plus(TTL).toInstant());
}
