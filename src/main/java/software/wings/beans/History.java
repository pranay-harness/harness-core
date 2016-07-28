package software.wings.beans;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Entity;

/**
 * Created by peeyushaggarwal on 6/20/16.
 */
@Entity(value = "history", noClassnameStored = true)
public class History extends Base {
  private EntryType entryType;
  private String entityType;
  private String entityId;
  private String entityName;
  private Base entityOldValue;
  private Base entityNewValue;

  /**
   * Gets activity type.
   *
   * @return the activity type
   */
  public EntryType getEntryType() {
    return entryType;
  }

  /**
   * Sets activity type.
   *
   * @param entryType the activity type
   */
  public void setEntryType(EntryType entryType) {
    this.entryType = entryType;
  }

  /**
   * Gets entity type.
   *
   * @return the entity type
   */
  public String getEntityType() {
    return entityType;
  }

  /**
   * Sets entity type.
   *
   * @param entityType the entity type
   */
  public void setEntityType(String entityType) {
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

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("entryType", entryType)
        .add("entityType", entityType)
        .add("entityId", entityId)
        .add("entityName", entityName)
        .add("entityOldValue", entityOldValue)
        .add("entityNewValue", entityNewValue)
        .toString();
  }

  /**
   * The enum Activity type.
   */
  public enum EntryType {
    /**
     * Started activity type.
     */
    STARTED, /**
              * Completed activity type.
              */
    COMPLETED, /**
                * Failed activity type.
                */
    FAILED, /**
             * Edited activity type.
             */
    EDITED, /**
             * Created activity type.
             */
    CREATED, /**
              * Cloned activity type.
              */
    CLONED, /**
             * Deleted activity type.
             */
    DELETED
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private EntryType entryType;
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

    /**
     * A history builder.
     *
     * @return the builder
     */
    public static Builder aHistory() {
      return new Builder();
    }

    /**
     * With activity type builder.
     *
     * @param entryType the activity type
     * @return the builder
     */
    public Builder withActivityType(EntryType entryType) {
      this.entryType = entryType;
      return this;
    }

    /**
     * With entity type builder.
     *
     * @param entityType the entity type
     * @return the builder
     */
    public Builder withEntityType(String entityType) {
      this.entityType = entityType;
      return this;
    }

    /**
     * With entity id builder.
     *
     * @param entityId the entity id
     * @return the builder
     */
    public Builder withEntityId(String entityId) {
      this.entityId = entityId;
      return this;
    }

    /**
     * With entity name builder.
     *
     * @param entityName the entity name
     * @return the builder
     */
    public Builder withEntityName(String entityName) {
      this.entityName = entityName;
      return this;
    }

    /**
     * With entity old value builder.
     *
     * @param entityOldValue the entity old value
     * @return the builder
     */
    public Builder withEntityOldValue(Base entityOldValue) {
      this.entityOldValue = entityOldValue;
      return this;
    }

    /**
     * With entity new value builder.
     *
     * @param entityNewValue the entity new value
     * @return the builder
     */
    public Builder withEntityNewValue(Base entityNewValue) {
      this.entityNewValue = entityNewValue;
      return this;
    }

    /**
     * With uuid builder.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by builder.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at builder.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by builder.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at builder.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * With active builder.
     *
     * @param active the active
     * @return the builder
     */
    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aHistory()
          .withActivityType(entryType)
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

    /**
     * Build history.
     *
     * @return the history
     */
    public History build() {
      History history = new History();
      history.setEntryType(entryType);
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
