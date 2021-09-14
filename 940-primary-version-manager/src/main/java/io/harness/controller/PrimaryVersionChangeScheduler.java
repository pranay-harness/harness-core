/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.controller;

import io.harness.persistence.HPersistence;
import io.harness.threading.Schedulable;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Singleton
public class PrimaryVersionChangeScheduler {
  @Inject private PrimaryVersionController primaryVersionController;
  @Inject private HPersistence hPersistence;

  public void registerExecutors() {
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
        new ThreadFactoryBuilder().setNameFormat("primary-version-change-scheduler").build());
    scheduledExecutorService.scheduleWithFixedDelay(
        new Schedulable(
            "Exception occurred while running primary version change scheduler", () -> primaryVersionController.run()),
        0, 5, TimeUnit.SECONDS);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> scheduledExecutorService.shutdownNow()));
  }
}
