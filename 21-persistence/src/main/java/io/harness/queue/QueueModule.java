package io.harness.queue;

import static java.util.Arrays.asList;

import com.google.inject.Injector;

import io.harness.govern.DependencyModule;
import io.harness.govern.ServersModule;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.util.List;
import java.util.Set;

@Slf4j
public class QueueModule extends DependencyModule implements ServersModule {
  private static QueueModule instance;

  public static QueueModule getInstance() {
    if (instance == null) {
      instance = new QueueModule();
    }
    return instance;
  }

  @Override
  protected void configure() {}

  @Override
  public Set<DependencyModule> dependencies() {
    return null;
  }

  @Override
  public List<Closeable> servers(Injector injector) {
    final QueueListenerController queueListenerController = injector.getInstance(QueueListenerController.class);

    return asList(() -> {
      try {
        queueListenerController.stop();
      } catch (Exception exception) {
        logger.error("", exception);
      }
    }, () -> injector.getInstance(TimerScheduledExecutorService.class).shutdownNow());
  }
}
