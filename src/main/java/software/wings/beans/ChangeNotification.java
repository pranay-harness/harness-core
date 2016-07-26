package software.wings.beans;

import java.util.List;

/**
 * Created by anubhaw on 7/25/16.
 */
public class ChangeNotification extends Notification {
  /**
   * Instantiates a new Change notification.
   */
  public ChangeNotification() {
    super(NotificationType.CHANGE, false);
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    protected String displayText;
    protected String detailsUrl;
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
     * A change notification builder.
     *
     * @return the builder
     */
    public static Builder aChangeNotification() {
      return new Builder();
    }

    /**
     * With display text builder.
     *
     * @param displayText the display text
     * @return the builder
     */
    public Builder withDisplayText(String displayText) {
      this.displayText = displayText;
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
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aChangeNotification()
          .withDisplayText(displayText)
          .withDetailsUrl(detailsUrl)
          .withNotificationType(notificationType)
          .withNotificationActions(notificationActions)
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
      changeNotification.setDisplayText(displayText);
      changeNotification.setDetailsUrl(detailsUrl);
      changeNotification.setNotificationType(notificationType);
      changeNotification.setNotificationActions(notificationActions);
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
