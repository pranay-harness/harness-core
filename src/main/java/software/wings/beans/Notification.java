package software.wings.beans;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;

import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * Created by anubhaw on 7/22/16.
 */
@Entity(value = "notifications")
public abstract class Notification extends Base {
  @NotEmpty protected String displayText;
  @NotEmpty protected String detailsUrl;
  @NotNull private NotificationType notificationType;
  @NotNull private boolean actionable;
  private List<NotificationAction> notificationActions;

  /**
   * The enum Notification type.
   */
  public enum NotificationType { APPROVAL, CHANGE, FAILURE }

  /**
   * Instantiates a new Notification.
   */
  public Notification() {}

  public Notification(NotificationType notificationType, boolean actionable) {
    this.notificationType = notificationType;
    this.actionable = actionable;
  }

  /**
   * Gets display text.
   *
   * @return the display text
   */
  public String getDisplayText() {
    return displayText;
  }

  public abstract void setDisplayText();

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

  /**
   * Is actionable boolean.
   *
   * @return the boolean
   */
  public boolean isActionable() {
    return actionable;
  }

  /**
   * Sets actionable.
   *
   * @param actionable the actionable
   */
  public void setActionable(boolean actionable) {
    this.actionable = actionable;
  }

  public List<NotificationAction> getNotificationActions() {
    return notificationActions;
  }

  public void setNotificationActions(List<NotificationAction> notificationActions) {
    this.notificationActions = notificationActions;
  }
}
