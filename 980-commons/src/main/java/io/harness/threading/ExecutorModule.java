/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
