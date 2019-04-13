package io.harness.waiter;

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
@Entity(value = "waitQueues", noClassnameStored = true)
@Value
@Builder
@FieldNameConstants(innerTypeName = "WaitQueueKeys")
public class WaitQueue implements PersistentEntity, UuidAccess, CreatedAtAccess {
  public static final String CORRELATION_ID_KEY = "correlationId";
  public static final String WAIT_INSTANCE_ID_KEY = "waitInstanceId";

  public static final Duration TTL = Duration.ofDays(14);

  @Id @NotNull private String uuid;
  @Indexed @NotNull private long createdAt;

  @Indexed @NotNull private String waitInstanceId;
  @Indexed @NotNull private String correlationId;

  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plus(TTL).toInstant());
}
