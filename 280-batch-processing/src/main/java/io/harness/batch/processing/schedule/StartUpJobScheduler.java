/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.schedule;

import static io.harness.batch.processing.ApplicationReadyListener.createLivenessMarker;

import io.harness.timescaledb.metrics.QueryStatsPrinter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
public class StartUpJobScheduler {
  private static final long oneHour = 60L * 60 * 1000;

  @Autowired private QueryStatsPrinter queryStatsPrinter;

  /**
   * Created this job because while running functional test
   * ApplicationReadyListener#createLivenessMarkerOnReadyEvent() is not executed.
   */
  @Scheduled(fixedDelay = Long.MAX_VALUE)
  public void createLivenessMarkerJob() {
    log.info("Inside createLivenessMarker");

    try {
      createLivenessMarker();
    } catch (Exception ex) {
      log.error("StartUpJobs: Failed to create liveness marker");
    }
  }

  @Scheduled(fixedRate = oneHour * 24, initialDelay = oneHour)
  public void printTimescaleDBMetrics() {
    StringBuilder allQueries = new StringBuilder("\n");
    queryStatsPrinter.get().forEach(
        (k, v) -> allQueries.append(String.format("Query: [%s], Stats: [%s]", k, v)).append("\n"));

    log.info("PSQL query stats: {}", allQueries);
  }
}
