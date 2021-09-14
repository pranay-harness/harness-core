/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.waiter;

import io.harness.manage.ManagedScheduledExecutorService;

import com.google.inject.Singleton;

@Singleton
public class NotifierScheduledExecutorService extends ManagedScheduledExecutorService {
  NotifierScheduledExecutorService() {
    super("Notifier");
  }
}
