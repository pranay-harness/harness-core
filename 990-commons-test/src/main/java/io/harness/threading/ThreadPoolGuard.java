package io.harness.threading;

import lombok.Getter;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

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
