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
  /**
   * The Display text.
   */
  @NotEmpty protected String displayText;
  /**
   * The Details url.
   */
  @NotEmpty protected String detailsUrl;
  @NotNull private NotificationType notificationType;
  @NotNull private boolean actionable;
  private List<NotificationAction> notificationActions;

  /**
   * Instantiates a new Notification.
   */
  public Notification() {}

  /**
   * Instantiates a new Notification.
   *
   * @param notificationType the notification type
   * @param actionable       the actionable
   */
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

  /**
   * Sets display text.
   */
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

  /**
   * Gets notification actions.
   *
   * @return the notification actions
   */
  public List<NotificationAction> getNotificationActions() {
    return notificationActions;
  }

  /**
   * Sets notification actions.
   *
   * @param notificationActions the notification actions
   */
  public void setNotificationActions(List<NotificationAction> notificationActions) {
    this.notificationActions = notificationActions;
  }

  /**
   * The enum Notification type.
   */
  public enum NotificationType {
    /**
     * Approval notification type.
     */
    APPROVAL, /**
               * Change notification type.
               */
    CHANGE, /**
             * Failure notification type.
             */
    FAILURE
  }
}
