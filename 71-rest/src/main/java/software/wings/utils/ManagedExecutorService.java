package software.wings.utils;

import io.dropwizard.lifecycle.Managed;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by peeyushaggarwal on 4/18/16.
 */
public class ManagedExecutorService implements ExecutorService, Managed {
  private ExecutorService executorService;

  /**
   * Instantiates a new managed executor service.
   *
   * @param executorService the executor service
   */
  public ManagedExecutorService(ExecutorService executorService) {
    this.executorService = executorService;
  }

  /* (non-Javadoc)
   * @see java.util.concurrent.ExecutorService#shutdown()
   */
  @Override
  public void shutdown() {
    executorService.shutdown();
  }

  /* (non-Javadoc)
   * @see java.util.concurrent.ExecutorService#shutdownNow()
   */
  @Override
  public List<Runnable> shutdownNow() {
    return executorService.shutdownNow();
  }

  @Override
  public boolean isShutdown() {
    return executorService.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return executorService.isTerminated();
  }

  /* (non-Javadoc)
   * @see java.util.concurrent.ExecutorService#awaitTermination(long, java.util.concurrent.TimeUnit)
   */
  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return executorService.awaitTermination(timeout, unit);
  }

  /* (non-Javadoc)
   * @see java.util.concurrent.ExecutorService#submit(java.util.concurrent.Callable)
   */
  @Override
  public <T> Future<T> submit(Callable<T> task) {
    return executorService.submit(task);
  }

  /* (non-Javadoc)
   * @see java.util.concurrent.ExecutorService#submit(java.lang.Runnable, java.lang.Object)
   */
  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    return executorService.submit(task, result);
  }

  /* (non-Javadoc)
   * @see java.util.concurrent.ExecutorService#submit(java.lang.Runnable)
   */
  @Override
  public Future<?> submit(Runnable task) {
    return executorService.submit(task);
  }

  /* (non-Javadoc)
   * @see java.util.concurrent.ExecutorService#invokeAll(java.util.Collection)
   */
  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
    return executorService.invokeAll(tasks);
  }

  /* (non-Javadoc)
   * @see java.util.concurrent.ExecutorService#invokeAll(java.util.Collection, long, java.util.concurrent.TimeUnit)
   */
  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException {
    return executorService.invokeAll(tasks, timeout, unit);
  }

  /* (non-Javadoc)
   * @see java.util.concurrent.ExecutorService#invokeAny(java.util.Collection)
   */
  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
    return executorService.invokeAny(tasks);
  }

  /* (non-Javadoc)
   * @see java.util.concurrent.ExecutorService#invokeAny(java.util.Collection, long, java.util.concurrent.TimeUnit)
   */
  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return executorService.invokeAny(tasks, timeout, unit);
  }

  /* (non-Javadoc)
   * @see java.util.concurrent.Executor#execute(java.lang.Runnable)
   */
  @Override
  public void execute(Runnable command) {
    executorService.execute(command);
  }

  /**
   * Gets executor service.
   *
   * @return the executor service
   */
  protected ExecutorService getExecutorService() {
    return executorService;
  }

  /* (non-Javadoc)
   * @see io.dropwizard.lifecycle.Managed#start()
   */
  @Override
  public void start() throws Exception {
    // do nothing
  }

  /* (non-Javadoc)
   * @see io.dropwizard.lifecycle.Managed#stop()
   */
  @Override
  public void stop() throws Exception {
    for (Runnable runnable : executorService.shutdownNow()) {
      ((Future<?>) runnable).get();
    }
  }
}
