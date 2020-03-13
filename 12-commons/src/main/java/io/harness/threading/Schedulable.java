package io.harness.threading;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Schedulable implements Runnable {
  private Runnable runnable;
  private String message;

  public Schedulable(String message, Runnable runnable) {
    this.message = message;
    this.runnable = runnable;
  }

  // Some errors are actually recoverable, like not able to create thread because of lack of handles
  @SuppressWarnings({"PMD", "squid:S1181"})
  @Override
  public void run() {
    try {
      runnable.run();
    } catch (Throwable exception) {
      if (message != null) {
        logger.error(message, exception);
      }
    }
  }
}
