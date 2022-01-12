/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;

import io.harness.annotations.dev.BreakDependencyOn;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.DataStorageMode;
import software.wings.app.MainConfiguration;
import software.wings.beans.baseline.WorkflowExecutionBaseline;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.WorkflowExecutionBaselineServiceImpl;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.DataStoreService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@BreakDependencyOn("software.wings.app.MainConfiguration")
public class NewRelicMetricDataBaselineMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private DataStoreService dataStoreService;
  @Inject private MainConfiguration mainConfiguration;

  @Override
  public void migrate() {
    if (mainConfiguration.getExecutionLogsStorageMode() != DataStorageMode.GOOGLE_CLOUD_DATA_STORE) {
      log.info("google data store not enabled, returning....");
      return;
    }

    try (HIterator<WorkflowExecutionBaseline> iterator =
             new HIterator<>(wingsPersistence.createQuery(WorkflowExecutionBaseline.class).fetch())) {
      for (WorkflowExecutionBaseline baseline : iterator) {
        log.info("marking baseline for {} ", baseline);
        PageRequest<NewRelicMetricDataRecord> pageRequest =
            aPageRequest().addFilter("workflowExecutionId", Operator.EQ, baseline.getWorkflowExecutionId()).build();
        PageResponse<NewRelicMetricDataRecord> metricDataRecords =
            dataStoreService.list(NewRelicMetricDataRecord.class, pageRequest);

        log.info("num Of records: ", metricDataRecords.size());
        if (!metricDataRecords.isEmpty()) {
          metricDataRecords.forEach(
              dataRecord -> dataRecord.setValidUntil(WorkflowExecutionBaselineServiceImpl.BASELINE_TTL));

          dataStoreService.save(NewRelicMetricDataRecord.class, metricDataRecords, false);
        }
      }
    }

    log.info("migration done...");
  }
}
