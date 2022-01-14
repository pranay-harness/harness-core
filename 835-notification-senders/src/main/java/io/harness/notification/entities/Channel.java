package io.harness.notification.entities;

import io.harness.notification.NotificationChannelType;

public interface Channel {
  Object toObjectofProtoSchema();

  NotificationChannelType getChannelType();
}