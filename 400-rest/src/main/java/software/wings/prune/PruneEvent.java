package software.wings.prune;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.ng.DbAliases;
import io.harness.queue.Queuable;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.mongodb.morphia.annotations.Entity;

@Value
@EqualsAndHashCode(callSuper = true)
@Entity(value = "pruneQueue2", noClassnameStored = true)
@HarnessEntity(exportable = false)
@StoreIn(DbAliases.CG_MANAGER)
public class PruneEvent extends Queuable {
  public static final Duration DELAY = Duration.ofSeconds(5);
  public static final int MAX_RETRIES = 24;

  private String appId;
  private String entityId;
  private String entityClass;
  private boolean syncFromGit;

  public PruneEvent(Class clz, String appId, String entityId) {
    this(clz, appId, entityId, false);
  }

  public PruneEvent(Class clz, String appId, String entityId, boolean syncFromGit) {
    this(clz.getCanonicalName(), appId, entityId, syncFromGit);
  }

  public PruneEvent(String classCanonicalName, String appId, String entityId, boolean syncFromGit) {
    setEarliestGet(Date.from(OffsetDateTime.now().plus(DELAY).toInstant()));
    setRetries(MAX_RETRIES);
    this.appId = appId;
    this.entityId = entityId;
    this.entityClass = classCanonicalName;
    this.syncFromGit = syncFromGit;
  }

  public PruneEvent(String classCanonicalName, String appId, String entityId) {
    this(classCanonicalName, appId, entityId, false);
  }
}
