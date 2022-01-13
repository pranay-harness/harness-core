/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import software.wings.beans.NotificationAction.NotificationActionType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anubhaw on 7/28/16.
 */
public abstract class ActionableNotification extends Notification {
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
   * Instantiates a new Actionable notification.
   *
   * @param notificationType    the notification type
   * @param notificationActions the notification actions
   */
  public ActionableNotification(NotificationType notificationType, List<NotificationAction> notificationActions) {
    super(notificationType, true);
    if (notificationActions != null) {
      this.notificationActions = notificationActions;
    }
  }

  /**
   * Perform action boolean.
   *
   * @param actionType the action type
   * @return the boolean
   */
  public abstract boolean performAction(NotificationActionType actionType);

  /**
   * Gets notification actions.
   *
   * @return the notification actions
   */
  public List<NotificationAction> getNotificationActions() {
    return notificationActions;
  }
}
