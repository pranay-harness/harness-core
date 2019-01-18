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
    bind(ExecutorService.class)
        .annotatedWith(Names.named("artifactCollectionExecutor"))
        .toInstance(ThreadPool.create(10, 100, 500L, TimeUnit.MILLISECONDS,
            new ThreadFactoryBuilder()
                .setNameFormat("Artifact-Collection-%d")
                .setPriority(Thread.MIN_PRIORITY)
                .build()));
  }
}
