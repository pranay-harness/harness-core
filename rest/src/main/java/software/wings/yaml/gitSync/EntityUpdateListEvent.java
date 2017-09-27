package software.wings.yaml.gitSync;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Entity;
import software.wings.core.queue.Queuable;

import java.util.List;
import java.util.Objects;

/**
 *
 * @author bsollish 9/26/17
 *
 */
@Entity(value = "entityUpdateQueue", noClassnameStored = true)
public class EntityUpdateListEvent extends Queuable {
  private List<EntityUpdateEvent> entityUpdateEvents;
  private String accountId;

  public List<EntityUpdateEvent> getEntityUpdateEvents() {
    return entityUpdateEvents;
  }

  public void setEntityUpdateEvents(List<EntityUpdateEvent> entityUpdateEvents) {
    this.entityUpdateEvents = entityUpdateEvents;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this)
      return true;
    if (!(o instanceof YamlGitSync)) {
      return false;
    }
    EntityUpdateListEvent eule = (EntityUpdateListEvent) o;
    return Objects.equals(accountId, eule.accountId) && Objects.equals(entityUpdateEvents, eule.entityUpdateEvents);
  }

  @Override
  public String toString() {
    MoreObjects.toStringHelper(this);
    return MoreObjects.toStringHelper(this)
        .add("accountId", getAccountId())
        .add("entityUpdateEvents", getEntityUpdateEvents())
        .toString();
  }

  public static final class Builder {
    private List<EntityUpdateEvent> entityUpdateEvents;
    private String accountId;

    private Builder() {}

    public static EntityUpdateListEvent.Builder anEntityUpdateListEvent() {
      return new EntityUpdateListEvent.Builder();
    }

    public EntityUpdateListEvent.Builder withEntityUpdateEvents(List<EntityUpdateEvent> entityUpdateEvents) {
      this.entityUpdateEvents = entityUpdateEvents;
      return this;
    }

    public EntityUpdateListEvent.Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public EntityUpdateListEvent.Builder but() {
      return anEntityUpdateListEvent().withEntityUpdateEvents(entityUpdateEvents).withAccountId(accountId);
    }

    public EntityUpdateListEvent build() {
      EntityUpdateListEvent entityUpdateListEvent = new EntityUpdateListEvent();
      entityUpdateListEvent.setEntityUpdateEvents(entityUpdateEvents);
      entityUpdateListEvent.setAccountId(accountId);
      return entityUpdateListEvent;
    }
  }
}