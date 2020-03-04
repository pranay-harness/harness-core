package software.wings.app;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import io.harness.manage.ManagedScheduledExecutorService;
import io.harness.threading.ThreadPool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("perpetualTaskAssignor"))
        .toInstance(new ManagedScheduledExecutorService("perpetualTaskAssignor"));
    bind(ExecutorService.class)
        .annotatedWith(Names.named("asyncExecutor"))
        .toInstance(ThreadPool.create(10, 40, 1, TimeUnit.SECONDS,
            new ThreadFactoryBuilder().setNameFormat("async-task-%d").setPriority(Thread.MIN_PRIORITY).build()));
    bind(ExecutorService.class)
        .annotatedWith(Names.named("alternativeExecutor"))
        .toInstance(ThreadPool.create(10, 40, 1, TimeUnit.SECONDS,
            new ThreadFactoryBuilder()
                .setNameFormat("alternative-validation-task-%d")
                .setPriority(Thread.MIN_PRIORITY)
                .build()));
  }
}
