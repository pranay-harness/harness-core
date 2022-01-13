/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
