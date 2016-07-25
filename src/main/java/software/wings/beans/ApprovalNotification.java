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

    public static Builder anApprovalNotification() {
      return new Builder();
    }

    public Builder withEntityTitle(String entityTitle) {
      this.entityTitle = entityTitle;
      return this;
    }

    public Builder withEntityName(String entityName) {
      this.entityName = entityName;
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
