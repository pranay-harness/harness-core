package software.wings.beans;

import java.util.List;

/**
 * Created by anubhaw on 7/25/16.
 */
public class FailureNotification extends Notification {
  /**
   * Instantiates a new Failure notification.
   */
  public FailureNotification() {
    super(NotificationType.FAILURE, true);
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

    public static Builder aFailureNotification() {
      return new Builder();
    }

    public Builder withDisplayText(String displayText) {
      this.displayText = displayText;
      return this;
    }

    public Builder withDetailsUrl(String detailsUrl) {
      this.detailsUrl = detailsUrl;
      return this;
    }

    public Builder withNotificationType(NotificationType notificationType) {
      this.notificationType = notificationType;
      return this;
    }

    public Builder withNotificationActions(List<NotificationAction> notificationActions) {
      this.notificationActions = notificationActions;
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
      return aFailureNotification()
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

    public FailureNotification build() {
      FailureNotification failureNotification = new FailureNotification();
      failureNotification.setDisplayText(displayText);
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
