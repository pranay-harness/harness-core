/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.scheduler;

import io.harness.threading.CurrentThreadExecutor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Singleton
public class BackgroundExecutorService {
  private ExecutorService executorService;

  @Inject
  public BackgroundExecutorService(
      ExecutorService executorService, @Named("BackgroundSchedule") SchedulerConfig configuration) {
    this.executorService = configuration.isClustered() ? new CurrentThreadExecutor() : executorService;
  }

  public Future submit(Runnable task) {
    return executorService.submit(task);
  }
}
