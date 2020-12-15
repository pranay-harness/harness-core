package io.harness.notification.dtos;

import io.harness.Team;
import io.harness.notification.NotificationChannelType;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationDTO {
  String id;
  String accountIdentifier;
  Team team;
  NotificationChannelType channelType;
  boolean sent;
  int retries;
}
