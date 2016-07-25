package software.wings.beans;

import java.util.List;

/**
 * Created by anubhaw on 7/25/16.
 */
public class FailureNotification extends Notification {
  private String entityName;

  /**
   * Instantiates a new Failure notification.
   */
  public FailureNotification() {
    super(NotificationType.FAILURE, true);
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

  @Override
  public void setDisplayText() {
    this.displayText = String.format("There are failures in the orchestrated workflow, <a href=%s>%s</a>", detailsUrl,
        entityName); // TODO: extract out in some template
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String entityName;
    private String detailsUrl;
    private NotificationType notificationType;
    private List<NotificationAction> notificationActions;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    /**
     * A failure notification builder.
     *
     * @return the builder
     */
    public static Builder aFailureNotification() {
      return new Builder();
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
     * With details url builder.
     *
     * @param detailsUrl the details url
     * @return the builder
     */
    public Builder withDetailsUrl(String detailsUrl) {
      this.detailsUrl = detailsUrl;
      return this;
    }

    /**
     * With notification type builder.
     *
     * @param notificationType the notification type
     * @return the builder
     */
    public Builder withNotificationType(NotificationType notificationType) {
      this.notificationType = notificationType;
      return this;
    }

    /**
     * With notification actions builder.
     *
     * @param notificationActions the notification actions
     * @return the builder
     */
    public Builder withNotificationActions(List<NotificationAction> notificationActions) {
      this.notificationActions = notificationActions;
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
     * Build failure notification.
     *
     * @return the failure notification
     */
    public FailureNotification build() {
      FailureNotification failureNotification = new FailureNotification();
      failureNotification.setEntityName(entityName);
      failureNotification.setDetailsUrl(detailsUrl);
      failureNotification.setNotificationType(notificationType);
      failureNotification.setNotificationActions(notificationActions);
      failureNotification.setUuid(uuid);
      failureNotification.setAppId(appId);
      failureNotification.setCreatedBy(createdBy);
      failureNotification.setCreatedAt(createdAt);
      failureNotification.setLastUpdatedBy(lastUpdatedBy);
      failureNotification.setLastUpdatedAt(lastUpdatedAt);
      failureNotification.setActive(active);
      return failureNotification;
    }
  }
}
