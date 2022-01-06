/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.manage;

import static java.util.stream.Collectors.toList;

import io.dropwizard.lifecycle.Managed;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

  @Override
  public void shutdown() {
    executorService.shutdown();
  }

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

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return executorService.awaitTermination(timeout, unit);
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    return executorService.submit(GlobalContextManager.generateExecutorTask(task));
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    return executorService.submit(GlobalContextManager.generateExecutorTask(task), result);
  }

  @Override
  public Future<?> submit(Runnable task) {
    return executorService.submit(GlobalContextManager.generateExecutorTask(task));
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
    return executorService.invokeAll(tasks.stream().map(GlobalContextManager::generateExecutorTask).collect(toList()));
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException {
    return executorService.invokeAll(
        tasks.stream().map(GlobalContextManager::generateExecutorTask).collect(toList()), timeout, unit);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
    return executorService.invokeAny(tasks.stream().map(GlobalContextManager::generateExecutorTask).collect(toList()));
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return executorService.invokeAny(
        tasks.stream().map(GlobalContextManager::generateExecutorTask).collect(toList()), timeout, unit);
  }

  @Override
  public void execute(Runnable command) {
    executorService.execute(GlobalContextManager.generateExecutorTask(command));
  }

  protected ExecutorService getExecutorService() {
    return executorService;
  }

  @Override
  public void start() throws Exception {
    // do nothing
  }

  @Override
  public void stop() throws Exception {
    for (Runnable runnable : executorService.shutdownNow()) {
      ((Future<?>) runnable).get();
    }
  }
}
