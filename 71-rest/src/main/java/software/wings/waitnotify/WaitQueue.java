package software.wings.waitnotify;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;

/**
 * Represents WaitQueue.
 *
 * @author Rishi
 */
@Entity(value = "waitQueues", noClassnameStored = true)
@EqualsAndHashCode(callSuper = true)
public class WaitQueue extends Base {
  public static final String CORRELATION_ID_KEY = "correlationId";
  public static final String WAIT_INSTANCE_ID_KEY = "waitInstanceId";

  public static final Duration TTL = Duration.ofDays(14);

  @Indexed @Getter private String waitInstanceId;

  @Indexed @Getter private String correlationId;

  @SchemaIgnore
  @JsonIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plus(TTL).toInstant());

  /**
   * Instantiates a new wait queue.
   */
  public WaitQueue() {}

  /**
   * creates a WaitQueue object.
   *
   * @param waitInstanceId id of WaitInstance.
   * @param correlationId  correlationId WaitInstance is waiting on.
   */
  public WaitQueue(String waitInstanceId, String correlationId) {
    this.waitInstanceId = waitInstanceId;
    this.correlationId = correlationId;
  }
}
