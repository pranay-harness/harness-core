package io.harness.queue;

import com.google.inject.Singleton;

import io.harness.manage.ManagedScheduledExecutorService;

@Singleton
public class TimerScheduledExecutorService extends ManagedScheduledExecutorService {
  public TimerScheduledExecutorService() {
    super("Timer");
  }
}
