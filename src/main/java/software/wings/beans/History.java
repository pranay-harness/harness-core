package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;

/**
 * Created by peeyushaggarwal on 6/20/16.
 */
@Entity(value = "history", noClassnameStored = true)
public class History extends Base {
  private EventType eventType;
  private EntityType entityType;
  private String entityId;
  private String entityName;
  private Base entityOldValue;
  private Base entityNewValue;

  private String title;
  private String shortDescription;

  /**
   * Gets activity type.
   *
   * @return the activity type
   */
  public EventType getEventType() {
    return eventType;
  }

  /**
   * Sets activity type.
   *
   * @param eventType the activity type
   */
  public void setEventType(EventType eventType) {
    this.eventType = eventType;
  }

  /**
   * Gets entity type.
   *
   * @return the entity type
   */
  public EntityType getEntityType() {
    return entityType;
  }

  /**
   * Sets entity type.
   *
   * @param entityType the entity type
   */
  public void setEntityType(EntityType entityType) {
    this.entityType = entityType;
  }

  /**
   * Gets entity id.
   *
   * @return the entity id
   */
  public String getEntityId() {
    return entityId;
  }

  /**
   * Sets entity id.
   *
   * @param entityId the entity id
   */
  public void setEntityId(String entityId) {
    this.entityId = entityId;
  }

  /**
   * Gets entity name.
   *
   * @return the entity name
   */
  public String getEntityName() {
    return entityName;
  }

  /**
   * Sets entity name.
   *
   * @param entityName the entity name
   */
  public void setEntityName(String entityName) {
    this.entityName = entityName;
  }

  /**
   * Gets entity old value.
   *
   * @return the entity old value
   */
  public Base getEntityOldValue() {
    return entityOldValue;
  }

  /**
   * Sets entity old value.
   *
   * @param entityOldValue the entity old value
   */
  public void setEntityOldValue(Base entityOldValue) {
    this.entityOldValue = entityOldValue;
  }

  /**
   * Gets entity new value.
   *
   * @return the entity new value
   */
  public Base getEntityNewValue() {
    return entityNewValue;
  }

  /**
   * Sets entity new value.
   *
   * @param entityNewValue the entity new value
   */
  public void setEntityNewValue(Base entityNewValue) {
    this.entityNewValue = entityNewValue;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getShortDescription() {
    return shortDescription;
  }

  public void setShortDescription(String shortDescription) {
    this.shortDescription = shortDescription;
  }

  @Override
  public String toString() {
    return "History{"
        + "eventType=" + eventType + ", entityType='" + entityType + '\'' + ", entityId='" + entityId + '\''
        + ", entityName='" + entityName + '\'' + ", entityOldValue=" + entityOldValue + ", entityNewValue="
        + entityNewValue + ", title='" + title + '\'' + ", shortDescription='" + shortDescription + '\'' + '}';
  }

  public static final class Builder {
    private EventType eventType;
    private EntityType entityType;
    private String entityId;
    private String entityName;
    private Base entityOldValue;
    private Base entityNewValue;
    private String title;
    private String shortDescription;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    public static Builder aHistory() {
      return new Builder();
    }

    public Builder withEventType(EventType eventType) {
      this.eventType = eventType;
      return this;
    }

    public Builder withEntityType(EntityType entityType) {
      this.entityType = entityType;
      return this;
    }

    public Builder withEntityId(String entityId) {
      this.entityId = entityId;
      return this;
    }

    public Builder withEntityName(String entityName) {
      this.entityName = entityName;
      return this;
    }

    public Builder withEntityOldValue(Base entityOldValue) {
      this.entityOldValue = entityOldValue;
      return this;
    }

    public Builder withEntityNewValue(Base entityNewValue) {
      this.entityNewValue = entityNewValue;
      return this;
    }

    public Builder withTitle(String title) {
      this.title = title;
      return this;
    }

    public Builder withShortDescription(String shortDescription) {
      this.shortDescription = shortDescription;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    public History build() {
      History history = new History();
      history.setEventType(eventType);
      history.setEntityType(entityType);
      history.setEntityId(entityId);
      history.setEntityName(entityName);
      history.setEntityOldValue(entityOldValue);
      history.setEntityNewValue(entityNewValue);
      history.setTitle(title);
      history.setShortDescription(shortDescription);
      history.setUuid(uuid);
      history.setAppId(appId);
      history.setCreatedBy(createdBy);
      history.setCreatedAt(createdAt);
      history.setLastUpdatedBy(lastUpdatedBy);
      history.setLastUpdatedAt(lastUpdatedAt);
      history.setActive(active);
      return history;
    }
  }
}
