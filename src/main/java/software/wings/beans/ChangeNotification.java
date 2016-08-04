package software.wings.beans;

/**
 * Created by anubhaw on 7/25/16.
 */
public class ChangeNotification extends Notification {
  private long scheduledOn;

  /**
   * Instantiates a new Change notification.
   */
  public ChangeNotification() {
    super(NotificationType.CHANGE);
  }

  /**
   * Gets scheduled on.
   *
   * @return the scheduled on
   */
  public long getScheduledOn() {
    return scheduledOn;
  }

  /**
   * Sets scheduled on.
   *
   * @param scheduledOn the scheduled on
   */
  public void setScheduledOn(long scheduledOn) {
    this.scheduledOn = scheduledOn;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private long scheduledOn;
    private String environmentId;
    private String entityId;
    private EntityType entityType;
    private boolean complete = true;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    /**
     * A change notification builder.
     *
     * @return the builder
     */
    public static Builder aChangeNotification() {
      return new Builder();
    }

    /**
     * With scheduled on builder.
     *
     * @param scheduledOn the scheduled on
     * @return the builder
     */
    public Builder withScheduledOn(long scheduledOn) {
      this.scheduledOn = scheduledOn;
      return this;
    }

    /**
     * With environment id builder.
     *
     * @param environmentId the environment id
     * @return the builder
     */
    public Builder withEnvironmentId(String environmentId) {
      this.environmentId = environmentId;
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
     * With entity type builder.
     *
     * @param entityType the entity type
     * @return the builder
     */
    public Builder withEntityType(EntityType entityType) {
      this.entityType = entityType;
      return this;
    }

    /**
     * With complete builder.
     *
     * @param complete the complete
     * @return the builder
     */
    public Builder withComplete(boolean complete) {
      this.complete = complete;
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
      return aChangeNotification()
          .withScheduledOn(scheduledOn)
          .withEnvironmentId(environmentId)
          .withEntityId(entityId)
          .withEntityType(entityType)
          .withComplete(complete)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    /**
     * Build change notification.
     *
     * @return the change notification
     */
    public ChangeNotification build() {
      ChangeNotification changeNotification = new ChangeNotification();
      changeNotification.setScheduledOn(scheduledOn);
      changeNotification.setEnvironmentId(environmentId);
      changeNotification.setEntityId(entityId);
      changeNotification.setEntityType(entityType);
      changeNotification.setComplete(complete);
      changeNotification.setUuid(uuid);
      changeNotification.setAppId(appId);
      changeNotification.setCreatedBy(createdBy);
      changeNotification.setCreatedAt(createdAt);
      changeNotification.setLastUpdatedBy(lastUpdatedBy);
      changeNotification.setLastUpdatedAt(lastUpdatedAt);
      changeNotification.setActive(active);
      return changeNotification;
    }
  }
}
