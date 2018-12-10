package io.harness.queue;

import com.google.inject.Singleton;

import io.dropwizard.lifecycle.Managed;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Singleton
public class QueueListenerController implements Managed {
  private ExecutorService executorService = Executors.newCachedThreadPool();
  private List<QueueListener<?>> abstractQueueListeners = new ArrayList<>();

  public void register(QueueListener<?> listener, int threads) {
    IntStream.rangeClosed(1, threads).forEach(value -> {
      abstractQueueListeners.add(listener);
      executorService.submit(listener);
    });
  }

  /* (non-Javadoc)
   * @see io.dropwizard.lifecycle.Managed#start()
   */
  @Override
  public void start() throws Exception {
    // Do nothing
  }

  /* (non-Javadoc)
   * @see io.dropwizard.lifecycle.Managed#stop()
   */
  @Override
  public void stop() throws Exception {
    abstractQueueListeners.forEach(QueueListener::shutDown);
    executorService.shutdownNow();
    executorService.awaitTermination(1, TimeUnit.HOURS);
  }
}
