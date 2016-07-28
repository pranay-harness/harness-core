package software.wings.beans;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anubhaw on 7/28/16.
 */
public abstract class ActionableNotification extends Notification {
  private boolean actionable = true;
  private List<NotificationAction> notificationActions = new ArrayList<>();

  /**
   * Instantiates a new Actionable notification.
   *
   * @param notificationType the notification type
   */
  public ActionableNotification(NotificationType notificationType) {
    this(notificationType, null);
  }

  /**
   * Perform action boolean.
   *
   * @return the boolean
   */
  public abstract boolean performAction();

  /**
   * Instantiates a new Actionable notification.
   *
   * @param notificationType    the notification type
   * @param notificationActions the notification actions
   */
  public ActionableNotification(NotificationType notificationType, List<NotificationAction> notificationActions) {
    super(notificationType, false);
    if (notificationActions != null) {
      this.notificationActions = notificationActions;
    }
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
   * Gets notification actions.
   *
   * @return the notification actions
   */
  public List<NotificationAction> getNotificationActions() {
    return notificationActions;
  }
}
