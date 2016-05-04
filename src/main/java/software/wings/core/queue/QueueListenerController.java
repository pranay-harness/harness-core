package software.wings.core.queue;

import io.dropwizard.lifecycle.Managed;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.stream.IntStream;
import javax.inject.Singleton;

/**
 * Created by peeyushaggarwal on 4/14/16.
 */
@Singleton
public class QueueListenerController implements Managed {
  private ExecutorService executorService = Executors.newCachedThreadPool();

  public void register(AbstractQueueListener<?> listener, int threads) {
    IntStream.rangeClosed(1, threads).forEach(value -> executorService.submit(listener));
  }

  @Override
  public void start() throws Exception {
    // Do nothing
  }

  @Override
  public void stop() throws Exception {
    for (Runnable runnable : executorService.shutdownNow()) {
      ((FutureTask<?>) runnable).get();
    }
  }
}
