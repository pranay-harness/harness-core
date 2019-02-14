package software.wings.app;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import io.harness.manage.ManagedScheduledExecutorService;

import java.util.concurrent.ScheduledExecutorService;

public class ManagerExecutorModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("waitStateResumer"))
        .toInstance(new ManagedScheduledExecutorService("WaitStateResumer"));
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("notifyResponseCleaner"))
        .toInstance(new ManagedScheduledExecutorService("NotifyResponseCleaner"));
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("delegateTaskNotifier"))
        .toInstance(new ManagedScheduledExecutorService("DelegateTaskNotifier"));
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("gitChangeSet"))
        .toInstance(new ManagedScheduledExecutorService("GitChangeSet"));
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("taskPollExecutor"))
        .toInstance(new ManagedScheduledExecutorService("TaskPoll-Thread"));
  }
}
