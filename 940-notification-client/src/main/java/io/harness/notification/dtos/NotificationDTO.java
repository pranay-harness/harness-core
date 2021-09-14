/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification.dtos;

import io.harness.Team;
import io.harness.notification.NotificationChannelType;

import java.util.List;
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
  List<Boolean> processingResponses;
  int retries;
}
