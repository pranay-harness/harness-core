package io.harness.queue;

import com.google.inject.name.Named;
import com.google.inject.name.Names;

import io.harness.govern.DependencyModule;
import io.harness.manage.ManagedScheduledExecutorService;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

public class QueueModule extends DependencyModule {
  public static final Named EXECUTOR_NAME = Names.named("timer");

  private static QueueModule instance;

  public static QueueModule getInstance() {
    if (instance == null) {
      instance = new QueueModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(ScheduledExecutorService.class)
        .annotatedWith(EXECUTOR_NAME)
        .toInstance(new ManagedScheduledExecutorService("Timer"));
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return null;
  }
}
