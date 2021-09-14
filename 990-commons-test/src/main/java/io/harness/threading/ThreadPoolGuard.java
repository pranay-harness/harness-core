/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.threading;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import lombok.Getter;

public class ThreadPoolGuard implements Closeable {
  @Getter private ExecutorService executorService;

  public ThreadPoolGuard(ExecutorService executorService) {
    this.executorService = executorService;
  }

  @Override
  public void close() throws IOException {
    if (executorService != null) {
      executorService.shutdownNow();
    }
  }
}
