package io.harness.utils;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import org.slf4j.Logger;

@OwnedBy(PL)
public class TimeLogger implements AutoCloseable {
  long startTime;
  Logger logger;

  public TimeLogger(Logger logger) {
    this.startTime = System.currentTimeMillis();
    this.logger = logger;
  }

  @Override
  public void close() {
    long finishTime = System.currentTimeMillis();
    this.logger.info("Time taken {} ms", finishTime - startTime);
  }
}
