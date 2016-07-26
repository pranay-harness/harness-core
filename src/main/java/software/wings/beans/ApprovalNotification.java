package software.wings.beans;

import static software.wings.beans.Notification.NotificationType.APPROVAL;

import java.util.List;

/**
 * Created by anubhaw on 7/25/16.
 */
public class ApprovalNotification extends Notification {
  private String entityTitle;
  private String entityName;

  /**
   * Instantiates a new Approval notification.
   */
  public ApprovalNotification() {
    super(APPROVAL, true);
  }

  /**
   * Gets entity title.
   *
   * @return the entity title
   */
  public String getEntityTitle() {
    return entityTitle;
  }

  /**
   * Sets entity title.
   *
   * @param entityTitle the entity title
   */
  public void setEntityTitle(String entityTitle) {
    this.entityTitle = entityTitle;
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
    this.displayText = String.format("%s <a href=%s>%s</a> is waiting for approval", entityTitle, detailsUrl,
        entityName); // TODO: extract out in some template
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    /**
     * The Details url.
     */
    protected String detailsUrl;
    private String entityTitle;
    private String entityName;
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
     * An approval notification builder.
     *
     * @return the builder
     */
    public static Builder anApprovalNotification() {
      return new Builder();
    }

    /**
     * With entity title builder.
     *
     * @param entityTitle the entity title
     * @return the builder
     */
    public Builder withEntityTitle(String entityTitle) {
      this.entityTitle = entityTitle;
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
      return anApprovalNotification()
          .withEntityTitle(entityTitle)
          .withEntityName(entityName)
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
     * Build approval notification.
     *
     * @return the approval notification
     */
    public ApprovalNotification build() {
      ApprovalNotification approvalNotification = new ApprovalNotification();
      approvalNotification.setEntityTitle(entityTitle);
      approvalNotification.setEntityName(entityName);
      approvalNotification.setDetailsUrl(detailsUrl);
      approvalNotification.setNotificationType(notificationType);
      approvalNotification.setNotificationActions(notificationActions);
      approvalNotification.setUuid(uuid);
      approvalNotification.setAppId(appId);
      approvalNotification.setCreatedBy(createdBy);
      approvalNotification.setCreatedAt(createdAt);
      approvalNotification.setLastUpdatedBy(lastUpdatedBy);
      approvalNotification.setLastUpdatedAt(lastUpdatedAt);
      approvalNotification.setActive(active);
      return approvalNotification;
    }
  }
}
