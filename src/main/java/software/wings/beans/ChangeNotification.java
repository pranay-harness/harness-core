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

  public long getScheduledOn() {
    return scheduledOn;
  }

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
    private NotificationEntityType entityType;
    private NotificationType notificationType;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    public static Builder aChangeNotification() {
      return new Builder();
    }

    public Builder withScheduledOn(long scheduledOn) {
      this.scheduledOn = scheduledOn;
      return this;
    }

    public Builder withEnvironmentId(String environmentId) {
      this.environmentId = environmentId;
      return this;
    }

    public Builder withEntityId(String entityId) {
      this.entityId = entityId;
      return this;
    }

    public Builder withEntityType(NotificationEntityType entityType) {
      this.entityType = entityType;
      return this;
    }

    public Builder withNotificationType(NotificationType notificationType) {
      this.notificationType = notificationType;
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
      return aChangeNotification()
          .withScheduledOn(scheduledOn)
          .withEnvironmentId(environmentId)
          .withEntityId(entityId)
          .withEntityType(entityType)
          .withNotificationType(notificationType)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    public ChangeNotification build() {
      ChangeNotification changeNotification = new ChangeNotification();
      changeNotification.setScheduledOn(scheduledOn);
      changeNotification.setEnvironmentId(environmentId);
      changeNotification.setEntityId(entityId);
      changeNotification.setEntityType(entityType);
      changeNotification.setNotificationType(notificationType);
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
