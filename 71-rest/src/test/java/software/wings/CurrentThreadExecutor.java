package software.wings;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by peeyushaggarwal on 4/5/16.
 */
public class CurrentThreadExecutor extends AbstractExecutorService {
  private AtomicBoolean terminated = new AtomicBoolean(false);

  /* (non-Javadoc)
   * @see java.util.concurrent.Executor#execute(java.lang.Runnable)
   */
  @Override
  public void execute(Runnable command) {
    command.run();
  }

  /* (non-Javadoc)
   * @see java.util.concurrent.ExecutorService#shutdown()
   */
  @Override
  public void shutdown() {
    terminated.compareAndSet(false, true);
  }

  /* (non-Javadoc)
   * @see java.util.concurrent.ExecutorService#shutdownNow()
   */
  @Override
  public List<Runnable> shutdownNow() {
    return Collections.emptyList();
  }

  @Override
  public boolean isShutdown() {
    return terminated.get();
  }

  @Override
  public boolean isTerminated() {
    return terminated.get();
  }

  /* (non-Javadoc)
   * @see java.util.concurrent.ExecutorService#awaitTermination(long, java.util.concurrent.TimeUnit)
   */
  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    shutdown();
    return terminated.get();
  }
}
