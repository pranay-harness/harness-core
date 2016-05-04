package software.wings.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by peeyushaggarwal on 4/18/16.
 */
public class ManagedScheduledExecutorService extends ManagedExecutorService implements ScheduledExecutorService {
  public ManagedScheduledExecutorService(ScheduledExecutorService executorService) {
    super(executorService);
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    return ((ScheduledExecutorService) getExecutorService()).schedule(command, delay, unit);
  }

  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
    return ((ScheduledExecutorService) getExecutorService()).schedule(callable, delay, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
    return ((ScheduledExecutorService) getExecutorService()).scheduleAtFixedRate(command, initialDelay, period, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
    return ((ScheduledExecutorService) getExecutorService()).scheduleWithFixedDelay(command, initialDelay, delay, unit);
  }

  @Override
  public void stop() throws Exception {
    shutdown();
    awaitTermination(10000, TimeUnit.MILLISECONDS);
  }
}
