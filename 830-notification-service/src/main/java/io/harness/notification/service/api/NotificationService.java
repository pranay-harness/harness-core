/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification.service.api;

import io.harness.NotificationRequest;
import io.harness.Team;
import io.harness.ng.beans.PageRequest;
import io.harness.notification.entities.Notification;

import java.util.Optional;
import org.springframework.data.domain.Page;

public interface NotificationService {
  boolean processNewMessage(NotificationRequest notificationRequest);

  void processRetries(Notification notification);

  Optional<Notification> getnotification(String notificationId);

  Page<Notification> list(Team team, PageRequest pageRequest);
}
