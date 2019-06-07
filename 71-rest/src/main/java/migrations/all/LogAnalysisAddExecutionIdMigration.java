package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogMLAnalysisRecord.LogMLAnalysisRecordKeys;
import software.wings.sm.StateExecutionInstance;

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
          logger.info("For analysis {} wqrkflow execution id is already set to {}", logMLAnalysisRecord.getUuid(),
              logMLAnalysisRecord.getWorkflowExecutionId());
          continue;
        }

        final StateExecutionInstance stateExecutionInstance =
            wingsPersistence.get(StateExecutionInstance.class, logMLAnalysisRecord.getStateExecutionId());
        if (stateExecutionInstance == null) {
          logger.info("For analysis {} no state execution found for id {}", logMLAnalysisRecord.getUuid(),
              logMLAnalysisRecord.getStateExecutionId());
          continue;
        }

        final String workflowExecutionId = stateExecutionInstance.getExecutionUuid();
        if (isEmpty(workflowExecutionId)) {
          logger.info("For analysis {} and state {} no workflow execution was set", logMLAnalysisRecord.getUuid(),
              logMLAnalysisRecord.getStateExecutionId());
          continue;
        }

        wingsPersistence.updateField(LogMLAnalysisRecord.class, logMLAnalysisRecord.getUuid(),
            LogMLAnalysisRecordKeys.workflowExecutionId, workflowExecutionId);
        sleep(ofMillis(100));
        if (++updatedCount % 1000 == 0) {
          logger.info("processed {} records", updatedCount);
        }
      }
    }
    logger.info("Updated {} logMLAnalysisRecords with workflow execution id", updatedCount);
  }
}
