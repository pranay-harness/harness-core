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
  private Class klass;
  private CrudType crudType;

  public String getEntityId() {
    return entityId;
  }

  public void setEntityId(String entityId) {
    this.entityId = entityId;
  }

  public Class getKlass() {
    return klass;
  }

  public void setKlass(Class klass) {
    this.klass = klass;
  }

  public CrudType getCrudType() {
    return crudType;
  }

  public void setCrudType(CrudType crudType) {
    this.crudType = crudType;
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
    return Objects.equals(klass, eue.klass) && Objects.equals(entityId, eue.entityId);
  }

  @Override
  public String toString() {
    MoreObjects.toStringHelper(this);
    return MoreObjects.toStringHelper(this)
        .add("class", getKlass())
        .add("entityId", getEntityId())
        .add("crudType", getCrudType().name())
        .toString();
  }

  public enum CrudType { CREATE, UPDATE, DELETE }

  public static final class Builder {
    private String entityId;
    private Class klass;
    private CrudType crudType;

    private Builder() {}

    public static EntityUpdateEvent.Builder anEntityUpdateEvent() {
      return new EntityUpdateEvent.Builder();
    }

    public EntityUpdateEvent.Builder withEntityId(String entityId) {
      this.entityId = entityId;
      return this;
    }

    public EntityUpdateEvent.Builder withClass(Class klass) {
      this.klass = klass;
      return this;
    }

    public EntityUpdateEvent.Builder withCrudType(CrudType crudType) {
      this.crudType = crudType;
      return this;
    }

    public EntityUpdateEvent.Builder but() {
      return anEntityUpdateEvent().withEntityId(entityId).withClass(klass).withCrudType(crudType);
    }

    public EntityUpdateEvent build() {
      EntityUpdateEvent entityUpdateEvent = new EntityUpdateEvent();
      entityUpdateEvent.setEntityId(entityId);
      entityUpdateEvent.setKlass(klass);
      entityUpdateEvent.setCrudType(crudType);
      return entityUpdateEvent;
    }
  }
}
