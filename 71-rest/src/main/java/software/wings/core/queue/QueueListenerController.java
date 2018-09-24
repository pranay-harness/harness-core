package software.wings.core.queue;

import com.google.inject.Singleton;

import io.dropwizard.lifecycle.Managed;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Created by peeyushaggarwal on 4/14/16.
 */
@Singleton
public class QueueListenerController implements Managed {
  private ExecutorService executorService = Executors.newCachedThreadPool();
  private List<AbstractQueueListener<?>> abstractQueueListeners = new ArrayList<>();

  public void register(AbstractQueueListener<?> listener, int threads) {
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
    abstractQueueListeners.forEach(AbstractQueueListener::shutDown);
    executorService.shutdownNow();
    executorService.awaitTermination(1, TimeUnit.HOURS);
  }
}
