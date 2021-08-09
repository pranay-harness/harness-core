package io.harness.queue;

import io.harness.manage.ManagedScheduledExecutorService;

import com.google.inject.Singleton;

@Singleton
public class TimerScheduledExecutorService extends ManagedScheduledExecutorService {
  public TimerScheduledExecutorService() {
    super("Timer");
  }
}
