package software.wings.beans;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Entity;

/**
 * Created by peeyushaggarwal on 6/20/16.
 */
@Entity(value = "history", noClassnameStored = true)
public class History extends Base {
  private ActivityType activityType;
  private String entityType;
  private String entityId;
  private String entityName;
  private Base entityOldValue;
  private Base entityNewValue;

  public ActivityType getActivityType() {
    return activityType;
  }

  public void setActivityType(ActivityType activityType) {
    this.activityType = activityType;
  }

  public String getEntityType() {
    return entityType;
  }

  public void setEntityType(String entityType) {
    this.entityType = entityType;
  }

  public String getEntityId() {
    return entityId;
  }

  public void setEntityId(String entityId) {
    this.entityId = entityId;
  }

  public String getEntityName() {
    return entityName;
  }

  public void setEntityName(String entityName) {
    this.entityName = entityName;
  }

  public Base getEntityOldValue() {
    return entityOldValue;
  }

  public void setEntityOldValue(Base entityOldValue) {
    this.entityOldValue = entityOldValue;
  }

  public Base getEntityNewValue() {
    return entityNewValue;
  }

  public void setEntityNewValue(Base entityNewValue) {
    this.entityNewValue = entityNewValue;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("activityType", activityType)
        .add("entityType", entityType)
        .add("entityId", entityId)
        .add("entityName", entityName)
        .add("entityOldValue", entityOldValue)
        .add("entityNewValue", entityNewValue)
        .toString();
  }

  public enum ActivityType { STARTED, COMPLETED, FAILED, EDITED, CREATED, CLONED, DELETED }

  public static final class Builder {
    private ActivityType activityType;
    private String entityType;
    private String entityId;
    private String entityName;
    private Base entityOldValue;
    private Base entityNewValue;
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

    public Builder withActivityType(ActivityType activityType) {
      this.activityType = activityType;
      return this;
    }

    public Builder withEntityType(String entityType) {
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

    public Builder but() {
      return aHistory()
          .withActivityType(activityType)
          .withEntityType(entityType)
          .withEntityId(entityId)
          .withEntityName(entityName)
          .withEntityOldValue(entityOldValue)
          .withEntityNewValue(entityNewValue)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    public History build() {
      History history = new History();
      history.setActivityType(activityType);
      history.setEntityType(entityType);
      history.setEntityId(entityId);
      history.setEntityName(entityName);
      history.setEntityOldValue(entityOldValue);
      history.setEntityNewValue(entityNewValue);
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
