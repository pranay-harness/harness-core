/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
