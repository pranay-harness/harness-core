/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.threading.Morpheus.sleep;

import static java.time.Duration.ofMillis;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogMLAnalysisRecord.LogMLAnalysisRecordKeys;
import software.wings.sm.StateExecutionInstance;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogAnalysisAddExecutionIdMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Override
  public void migrate() {
    int updatedCount = 0;
    try (HIterator<LogMLAnalysisRecord> iterator =
             new HIterator<>(wingsPersistence.createQuery(LogMLAnalysisRecord.class, excludeAuthority)
                                 .field(LogMLAnalysisRecordKeys.stateExecutionId)
                                 .exists()
                                 .project(LogMLAnalysisRecordKeys.analysisDetailsCompressedJson, false)
                                 .project(LogMLAnalysisRecordKeys.protoSerializedAnalyisDetails, false)
                                 .fetch())) {
      while (iterator.hasNext()) {
        final LogMLAnalysisRecord logMLAnalysisRecord = iterator.next();
        if (isNotEmpty(logMLAnalysisRecord.getWorkflowExecutionId())) {
          log.info("For analysis {} wqrkflow execution id is already set to {}", logMLAnalysisRecord.getUuid(),
              logMLAnalysisRecord.getWorkflowExecutionId());
          continue;
        }

        final StateExecutionInstance stateExecutionInstance =
            wingsPersistence.get(StateExecutionInstance.class, logMLAnalysisRecord.getStateExecutionId());
        if (stateExecutionInstance == null) {
          log.info("For analysis {} no state execution found for id {}", logMLAnalysisRecord.getUuid(),
              logMLAnalysisRecord.getStateExecutionId());
          continue;
        }

        final String workflowExecutionId = stateExecutionInstance.getExecutionUuid();
        if (isEmpty(workflowExecutionId)) {
          log.info("For analysis {} and state {} no workflow execution was set", logMLAnalysisRecord.getUuid(),
              logMLAnalysisRecord.getStateExecutionId());
          continue;
        }

        wingsPersistence.updateField(LogMLAnalysisRecord.class, logMLAnalysisRecord.getUuid(),
            LogMLAnalysisRecordKeys.workflowExecutionId, workflowExecutionId);
        sleep(ofMillis(100));
        if (++updatedCount % 1000 == 0) {
          log.info("processed {} records", updatedCount);
        }
      }
    }
    log.info("Updated {} logMLAnalysisRecords with workflow execution id", updatedCount);
  }
}
