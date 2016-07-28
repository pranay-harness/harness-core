package software.wings.beans;

/**
 * Created by anubhaw on 7/28/16.
 */
public abstract class ActionableNotification extends Notification {
  private boolean actionable = true;

  public ActionableNotification(NotificationType notificationType) {
    super(notificationType, false);
  }

  public boolean isActionable() {
    return actionable;
  }
}
