package software.wings.yaml.gitSync;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Entity;
import software.wings.core.queue.Queuable;

import java.util.Objects;

/**
 *
 * @author bsollish 9/20/17
 *
 */
@Entity(value = "entityUpdateQueue", noClassnameStored = true)
public class EntityUpdateEvent extends Queuable {
  private String entityId;
  private String name;
  private String accountId;
  private String appId;
  private Class klass;
  private SourceType type;

  public String getEntityId() {
    return entityId;
  }

  public void setEntityId(String entityId) {
    this.entityId = entityId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public Class getKlass() {
    return klass;
  }

  public void setKlass(Class klass) {
    this.klass = klass;
  }

  public SourceType getSourceType() {
    return type;
  }

  public void setSourceType(SourceType type) {
    this.type = type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(klass, entityId);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this)
      return true;
    if (!(o instanceof YamlGitSync)) {
      return false;
    }
    EntityUpdateEvent eue = (EntityUpdateEvent) o;
    return Objects.equals(klass, eue.klass) && Objects.equals(entityId, eue.entityId)
        && Objects.equals(accountId, eue.accountId);
  }

  @Override
  public String toString() {
    MoreObjects.toStringHelper(this);
    return MoreObjects.toStringHelper(this)
        .add("class", getKlass())
        .add("entityId", getEntityId())
        .add("accountId", getAccountId())
        .add("type", getSourceType().name())
        .toString();
  }

  public enum SourceType {
    ENTITY_CREATE,
    ENTITY_UPDATE,
    ENTITY_DELETE,
    GIT_SYNC_CREATE,
    GIT_SYNC_UPDATE,
    GIT_SYNC_DELETE
  }

  public static final class Builder {
    private String entityId;
    private String name;
    private String accountId;
    private String appId;
    private Class klass;
    private SourceType type;

    private Builder() {}

    public static EntityUpdateEvent.Builder anEntityUpdateEvent() {
      return new EntityUpdateEvent.Builder();
    }

    public EntityUpdateEvent.Builder withEntityId(String entityId) {
      this.entityId = entityId;
      return this;
    }

    public EntityUpdateEvent.Builder withName(String name) {
      this.name = name;
      return this;
    }

    public EntityUpdateEvent.Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public EntityUpdateEvent.Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public EntityUpdateEvent.Builder withClass(Class klass) {
      this.klass = klass;
      return this;
    }

    public EntityUpdateEvent.Builder withSourceType(SourceType type) {
      this.type = type;
      return this;
    }

    public EntityUpdateEvent.Builder but() {
      return anEntityUpdateEvent()
          .withEntityId(entityId)
          .withName(name)
          .withAccountId(accountId)
          .withAppId(appId)
          .withClass(klass)
          .withSourceType(type);
    }

    public EntityUpdateEvent build() {
      EntityUpdateEvent entityUpdateEvent = new EntityUpdateEvent();
      entityUpdateEvent.setEntityId(entityId);
      entityUpdateEvent.setName(name);
      entityUpdateEvent.setAccountId(accountId);
      entityUpdateEvent.setAppId(appId);
      entityUpdateEvent.setKlass(klass);
      entityUpdateEvent.setSourceType(type);
      return entityUpdateEvent;
    }
  }
}
