package software.wings.app;

import static software.wings.common.thread.ThreadPool.create;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import org.mongodb.morphia.Datastore;
import software.wings.utils.ManagedExecutorService;
import software.wings.utils.ManagedScheduledExecutorService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by peeyushaggarwal on 5/25/16.
 */
public class ExecutorModule extends AbstractModule {
  private ExecutorService executorService;

  public ExecutorModule() {
    executorService = create(20, 1000, 500L, TimeUnit.MILLISECONDS);
  }

  public ExecutorModule(ExecutorService executorService) {
    this.executorService = executorService;
  }
  @Override
  protected void configure() {
    bind(ExecutorService.class).toInstance(new ManagedExecutorService(executorService));
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("timer"))
        .toInstance(new ManagedScheduledExecutorService(new ScheduledThreadPoolExecutor(1)));
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("notifier"))
        .toInstance(new ManagedScheduledExecutorService(new ScheduledThreadPoolExecutor(1)));
  }
}
