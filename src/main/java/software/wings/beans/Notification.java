package software.wings.beans;

import static software.wings.beans.NotificationAction.APPROVE;
import static software.wings.beans.NotificationAction.REJECT;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Created by anubhaw on 7/22/16.
 */
@Entity(value = "notifications")
public class Notification extends Base {
  private String displayText;
  private String detailsUrl;
  private String styledText;
  private NotificationType notificationType;

  /**
   * The enum Notification type.
   */
  public enum NotificationType {
    /**
     * Information notification type.
     */
    INFORMATION(false),
    /**
     * Approval notification type.
     */
    APPROVAL(true, APPROVE, REJECT),
    /**
     * Resume notification type.
     */
    RESUME(true, NotificationAction.RESUME);

    private boolean actionable;
    private List<NotificationAction> actions = new ArrayList<>();

    NotificationType(boolean actionable, NotificationAction... notificationActions) {
      this.actionable = actionable;
      Collections.addAll(actions, notificationActions);
    }

    /**
     * Is actionable boolean.
     *
     * @return the boolean
     */
    public boolean isActionable() {
      return actionable;
    }

    /**
     * Gets actions.
     *
     * @return the actions
     */
    public List<NotificationAction> getActions() {
      return actions;
    }
  }

  /**
   * Gets display text.
   *
   * @return the display text
   */
  public String getDisplayText() {
    return displayText;
  }

  /**
   * Sets display text.
   *
   * @param displayText the display text
   */
  public void setDisplayText(String displayText) {
    this.displayText = displayText;
  }

  /**
   * Gets details url.
   *
   * @return the details url
   */
  public String getDetailsUrl() {
    return detailsUrl;
  }

  /**
   * Sets details url.
   *
   * @param detailsUrl the details url
   */
  public void setDetailsUrl(String detailsUrl) {
    this.detailsUrl = detailsUrl;
  }

  /**
   * Gets styled text.
   *
   * @return the styled text
   */
  public String getStyledText() {
    return styledText;
  }

  /**
   * Sets styled text.
   *
   * @param styledText the styled text
   */
  public void setStyledText(String styledText) {
    this.styledText = styledText;
  }

  /**
   * Gets notification type.
   *
   * @return the notification type
   */
  public NotificationType getNotificationType() {
    return notificationType;
  }

  /**
   * Sets notification type.
   *
   * @param notificationType the notification type
   */
  public void setNotificationType(NotificationType notificationType) {
    this.notificationType = notificationType;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(displayText, detailsUrl, styledText, notificationType);
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
    final Notification other = (Notification) obj;
    return Objects.equals(this.displayText, other.displayText) && Objects.equals(this.detailsUrl, other.detailsUrl)
        && Objects.equals(this.styledText, other.styledText)
        && Objects.equals(this.notificationType, other.notificationType);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("displayText", displayText)
        .add("detailsUrl", detailsUrl)
        .add("styledText", styledText)
        .add("notificationType", notificationType)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String displayText;
    private String detailsUrl;
    private String styledText;
    private NotificationType notificationType;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    /**
     * A notification builder.
     *
     * @return the builder
     */
    public static Builder aNotification() {
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
     * With styled text builder.
     *
     * @param styledText the styled text
     * @return the builder
     */
    public Builder withStyledText(String styledText) {
      this.styledText = styledText;
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
      return aNotification()
          .withDisplayText(displayText)
          .withDetailsUrl(detailsUrl)
          .withStyledText(styledText)
          .withNotificationType(notificationType)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    /**
     * Build notification.
     *
     * @return the notification
     */
    public Notification build() {
      Notification notification = new Notification();
      notification.setDisplayText(displayText);
      notification.setDetailsUrl(detailsUrl);
      notification.setStyledText(styledText);
      notification.setNotificationType(notificationType);
      notification.setUuid(uuid);
      notification.setAppId(appId);
      notification.setCreatedBy(createdBy);
      notification.setCreatedAt(createdAt);
      notification.setLastUpdatedBy(lastUpdatedBy);
      notification.setLastUpdatedAt(lastUpdatedAt);
      notification.setActive(active);
      return notification;
    }
  }
}
