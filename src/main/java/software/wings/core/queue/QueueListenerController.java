package software.wings.core.queue;

import io.dropwizard.lifecycle.Managed;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;
import javax.inject.Singleton;

// TODO: Auto-generated Javadoc

/**
 * Created by peeyushaggarwal on 4/14/16.
 */
@Singleton
public class QueueListenerController implements Managed {
  private ExecutorService executorService = Executors.newCachedThreadPool();

  /**
   * Register.
   *
   * @param listener the listener
   * @param threads  the threads
   */
  public void register(AbstractQueueListener<?> listener, int threads) {
    IntStream.rangeClosed(1, threads).forEach(value -> executorService.submit(listener));
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
    for (Runnable runnable : executorService.shutdownNow()) {
      FutureTask<?> futureTask = (FutureTask<?>) runnable;
      boolean keepTrying = true;
      while (keepTrying) {
        try {
          futureTask.cancel(true);
          futureTask.get(1, TimeUnit.NANOSECONDS);
        } catch (TimeoutException e) {
          // need to retry
        } catch (Exception e) {
          keepTrying = false;
        }
      }
    }
  }
}
