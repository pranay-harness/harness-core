package io.harness.waiter;

import io.harness.manage.ManagedScheduledExecutorService;

import com.google.inject.Singleton;

@Singleton
public class NotifierScheduledExecutorService extends ManagedScheduledExecutorService {
  NotifierScheduledExecutorService() {
    super("Notifier");
  }
}
