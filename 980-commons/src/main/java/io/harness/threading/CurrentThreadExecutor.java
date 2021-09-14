/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.threading;

import io.harness.logging.AutoLogRemoveAllContext;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class CurrentThreadExecutor extends AbstractExecutorService {
  private AtomicBoolean terminated = new AtomicBoolean(false);

  @Override
  public void execute(Runnable command) {
    try (AutoLogRemoveAllContext ignore = new AutoLogRemoveAllContext()) {
      command.run();
    }
  }

  @Override
  public void shutdown() {
    terminated.compareAndSet(false, true);
  }

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

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    shutdown();
    return terminated.get();
  }
}
