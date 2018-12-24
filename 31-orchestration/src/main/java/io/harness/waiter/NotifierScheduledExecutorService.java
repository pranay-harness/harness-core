package io.harness.waiter;

import com.google.inject.Singleton;

import io.harness.manage.ManagedScheduledExecutorService;

@Singleton
public class NotifierScheduledExecutorService extends ManagedScheduledExecutorService {
  NotifierScheduledExecutorService() {
    super("Notifier");
  }
}
