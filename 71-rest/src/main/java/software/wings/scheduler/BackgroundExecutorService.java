package software.wings.scheduler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.threading.CurrentThreadExecutor;
import software.wings.app.MainConfiguration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Singleton
public class BackgroundExecutorService {
  private ExecutorService executorService;

  @Inject
  public BackgroundExecutorService(ExecutorService executorService, MainConfiguration configuration) {
    this.executorService =
        configuration.getBackgroundSchedulerConfig().isClustered() ? new CurrentThreadExecutor() : executorService;
  }

  Future<?> submit(Runnable task) {
    return executorService.submit(task);
  }
}
