package software.wings.prune;

import io.harness.queue.Queuable;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.mongodb.morphia.annotations.Entity;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;

@Value
@EqualsAndHashCode(callSuper = true)
@Entity(value = "pruneQueue", noClassnameStored = true)
public class PruneEvent extends Queuable {
  public static final Duration DELAY = Duration.ofSeconds(5);
  public static final int MAX_RETRIES = 24;

  private String appId;
  private String entityId;
  private String entityClass;

  public PruneEvent(Class clz, String appId, String entityId) {
    this(clz.getCanonicalName(), appId, entityId);
  }

  public PruneEvent(String classCanonicalName, String appId, String entityId) {
    setEarliestGet(Date.from(OffsetDateTime.now().plus(DELAY).toInstant()));
    setRetries(MAX_RETRIES);
    this.appId = appId;
    this.entityId = entityId;
    this.entityClass = classCanonicalName;
  }
}
