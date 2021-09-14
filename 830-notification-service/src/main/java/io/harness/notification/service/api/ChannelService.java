/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification.service.api;

import io.harness.NotificationRequest;
import io.harness.notification.beans.NotificationProcessingResponse;
import io.harness.notification.remote.dto.NotificationSettingDTO;

public interface ChannelService {
  NotificationProcessingResponse send(NotificationRequest notificationRequest);
  boolean sendTestNotification(NotificationSettingDTO notificationSettingDTO);
}
