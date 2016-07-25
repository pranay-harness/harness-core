package software.wings.beans;

import java.util.List;

/**
 * Created by anubhaw on 7/25/16.
 */
public class ChangeNotification extends Notification {
  private long changeDate;

  /**
   * Instantiates a new Change notification.
   */
  public ChangeNotification() {
    super(NotificationType.CHANGE, false);
  }

  /**
   * Gets change date.
   *
   * @return the change date
   */
  public long getChangeDate() {
    return changeDate;
  }

  /**
   * Sets change date.
   *
   * @param changeDate the change date
   */
  public void setChangeDate(long changeDate) {
    this.changeDate = changeDate;
  }

  @Override
  public void setDisplayText() {
    this.displayText = String.format("There's a <a href=%s>change</a> scheduled for %s", detailsUrl,
        changeDate); // TODO: extract out in some template
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private long changeDate;
    private String detailsUrl;
    private NotificationType notificationType;
    private boolean actionable;
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
     * With change date builder.
     *
     * @param changeDate the change date
     * @return the builder
     */
    public Builder withChangeDate(long changeDate) {
      this.changeDate = changeDate;
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
     * With actionable builder.
     *
     * @param actionable the actionable
     * @return the builder
     */
    public Builder withActionable(boolean actionable) {
      this.actionable = actionable;
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
     * Build change notification.
     *
     * @return the change notification
     */
    public ChangeNotification build() {
      ChangeNotification changeNotification = new ChangeNotification();
      changeNotification.setChangeDate(changeDate);
      changeNotification.setDetailsUrl(detailsUrl);
      changeNotification.setNotificationType(notificationType);
      changeNotification.setActionable(actionable);
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
