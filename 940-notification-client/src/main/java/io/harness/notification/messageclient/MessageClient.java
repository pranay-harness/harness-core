/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification.messageclient;

import io.harness.NotificationRequest;

public interface MessageClient {
  void send(NotificationRequest notificationRequest, String accountId);
}
