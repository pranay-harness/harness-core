/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.threading;

import io.harness.manage.ManagedExecutorService;

import com.google.inject.AbstractModule;
import java.util.concurrent.ExecutorService;
import lombok.Getter;
import lombok.Setter;

public class ExecutorModule extends AbstractModule {
  private static volatile ExecutorModule instance;

  public static ExecutorModule getInstance() {
    if (instance == null) {
      instance = new ExecutorModule();
    }
    return instance;
  }

  @Getter @Setter private ExecutorService executorService;

  @Override
  protected void configure() {
    // that's noop check to trigger PMD friendly NullPointerException
    executorService.isShutdown();

    bind(ExecutorService.class).toInstance(new ManagedExecutorService(executorService));
  }
}
