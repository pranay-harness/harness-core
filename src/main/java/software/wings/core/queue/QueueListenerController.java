package software.wings.core.queue;

import com.google.common.collect.Lists;

import io.dropwizard.lifecycle.Managed;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import javax.inject.Singleton;

// TODO: Auto-generated Javadoc

/**
 * Created by peeyushaggarwal on 4/14/16.
 */
@Singleton
public class QueueListenerController implements Managed {
  private ExecutorService executorService = Executors.newCachedThreadPool();
  private List<AbstractQueueListener<?>> abstractQueueListeners = Lists.newArrayList();

  /**
   * Register.
   *
   * @param listener the listener
   * @param threads  the threads
   */
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
    abstractQueueListeners.forEach(abstractQueueListener -> abstractQueueListener.shutDown());
    executorService.shutdownNow();
    while (!executorService.awaitTermination(1, TimeUnit.SECONDS))
      ;
  }
}
