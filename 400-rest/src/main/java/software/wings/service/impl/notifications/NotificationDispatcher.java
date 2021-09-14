/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.notifications;

import io.harness.exception.ExceptionUtils;
import io.harness.notifications.NotificationReceiverInfo;

import software.wings.beans.Notification;
import software.wings.beans.NotificationChannelType;

import java.util.List;
import java.util.Map.Entry;
import org.slf4j.Logger;

/**
 * Based on receiver info, sends the message to right set of services.
 */
public interface NotificationDispatcher<T extends NotificationReceiverInfo> {
  void dispatch(List<Notification> notifications, T receiver);

  default void iterateOverAddressesAndNotifiy(
      List<Notification> notifications, NotificationReceiverInfo notificationReceiverInfo) {
    if (null == notificationReceiverInfo || null == notificationReceiverInfo.getAddressesByChannelType()) {
      return;
    }

    for (Entry<NotificationChannelType, List<String>> entry :
        notificationReceiverInfo.getAddressesByChannelType().entrySet()) {
      if (entry.getKey() == NotificationChannelType.EMAIL) {
        try {
          getEmailDispatcher().dispatch(notifications, entry.getValue());
        } catch (Exception e) {
          logger().warn(ExceptionUtils.getMessage(e));
        }
      }
      if (entry.getKey() == NotificationChannelType.SLACK) {
        try {
          getSlackDispatcher().dispatch(notifications, entry.getValue());
        } catch (Exception e) {
          logger().warn(ExceptionUtils.getMessage(e));
        }
      }
    }
  }

  EmailDispatcher getEmailDispatcher();

  SlackMessageDispatcher getSlackDispatcher();

  Logger logger();
}
