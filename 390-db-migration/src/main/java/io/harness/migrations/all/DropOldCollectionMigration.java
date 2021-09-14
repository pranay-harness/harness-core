/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migrations.all;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;

import static java.util.Arrays.asList;

import io.harness.migrations.Migration;

import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DropOldCollectionMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    for (String collection : asList("appdynamicsMetricDefinitions", "appdynamicsMetrics", "completedMetricsSummary",
             "containerDeploymentQueue", "entityUpdateListQueue", "history", "metricSummary", "newRelicMetricNames",
             "splunkAnalysisRecords", "splunkLogs")) {
      try {
        wingsPersistence.getCollection(DEFAULT_STORE, collection).drop();
      } catch (RuntimeException ex) {
        log.error("Drop collection {} error", collection, ex);
      }
    }
  }
}
