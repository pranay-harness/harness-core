package software.wings.beans;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by anubhaw on 4/13/17.
 */
@Entity(value = "notificationBatch", noClassnameStored = true)
public class NotificationBatch extends Base {
  private String batchId;
  private NotificationRule notificationRule;
  @Reference(idOnly = true, ignoreMissing = true) private List<Notification> notifications = new ArrayList<>();

  /**
   * Gets batch id.
   *
   * @return the batch id
   */
  public String getBatchId() {
    return batchId;
  }

  /**
   * Sets batch id.
   *
   * @param batchId the batch id
   */
  public void setBatchId(String batchId) {
    this.batchId = batchId;
  }

  /**
   * Gets notification rule.
   *
   * @return the notification rule
   */
  public NotificationRule getNotificationRule() {
    return notificationRule;
  }

  /**
   * Sets notification rule.
   *
   * @param notificationRule the notification rule
   */
  public void setNotificationRule(NotificationRule notificationRule) {
    this.notificationRule = notificationRule;
  }

  /**
   * Gets pending notifications.
   *
   * @return the pending notifications
   */
  public List<Notification> getNotifications() {
    return notifications;
  }

  /**
   * Sets pending notifications.
   *
   * @param notifications the pending notifications
   */
  public void setNotifications(List<Notification> notifications) {
    this.notifications = notifications;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("batchId", batchId)
        .add("notificationRule", notificationRule)
        .add("notifications", notifications)
        .toString();
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(batchId, notificationRule, notifications);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    final NotificationBatch other = (NotificationBatch) obj;
    return Objects.equals(this.batchId, other.batchId) && Objects.equals(this.notificationRule, other.notificationRule)
        && Objects.equals(this.notifications, other.notifications);
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String batchId;
    private NotificationRule notificationRule;
    private List<Notification> notifications = new ArrayList<>();
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    /**
     * A notification batch builder.
     *
     * @return the builder
     */
    public static Builder aNotificationBatch() {
      return new Builder();
    }

    /**
     * With batch id builder.
     *
     * @param batchId the batch id
     * @return the builder
     */
    public Builder withBatchId(String batchId) {
      this.batchId = batchId;
      return this;
    }

    /**
     * With notification rule builder.
     *
     * @param notificationRule the notification rule
     * @return the builder
     */
    public Builder withNotificationRule(NotificationRule notificationRule) {
      this.notificationRule = notificationRule;
      return this;
    }

    /**
     * With notifications builder.
     *
     * @param notifications the notifications
     * @return the builder
     */
    public Builder withNotifications(List<Notification> notifications) {
      this.notifications = notifications;
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
    public Builder withCreatedBy(EmbeddedUser createdBy) {
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
    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
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
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aNotificationBatch()
          .withBatchId(batchId)
          .withNotificationRule(notificationRule)
          .withNotifications(notifications)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    /**
     * Build notification batch.
     *
     * @return the notification batch
     */
    public NotificationBatch build() {
      NotificationBatch notificationBatch = new NotificationBatch();
      notificationBatch.setBatchId(batchId);
      notificationBatch.setNotificationRule(notificationRule);
      notificationBatch.setNotifications(notifications);
      notificationBatch.setUuid(uuid);
      notificationBatch.setAppId(appId);
      notificationBatch.setCreatedBy(createdBy);
      notificationBatch.setCreatedAt(createdAt);
      notificationBatch.setLastUpdatedBy(lastUpdatedBy);
      notificationBatch.setLastUpdatedAt(lastUpdatedAt);
      return notificationBatch;
    }
  }
}
