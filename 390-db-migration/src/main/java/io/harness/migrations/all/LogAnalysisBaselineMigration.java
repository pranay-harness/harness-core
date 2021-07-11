package io.harness.migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.threading.Morpheus.sleep;

import static software.wings.service.impl.WorkflowExecutionBaselineServiceImpl.BASELINE_TTL;

import static java.time.Duration.ofMillis;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.baseline.WorkflowExecutionBaseline;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogMLAnalysisRecord.LogMLAnalysisRecordKeys;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.UpdateResults;

@Slf4j
public class LogAnalysisBaselineMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Override
  public void migrate() {
    try (HIterator<WorkflowExecutionBaseline> iterator =
             new HIterator<>(wingsPersistence.createQuery(WorkflowExecutionBaseline.class, excludeAuthority).fetch())) {
      while (iterator.hasNext()) {
        final WorkflowExecutionBaseline workflowExecutionBaseline = iterator.next();

        final UpdateResults updateResults =
            wingsPersistence.update(wingsPersistence.createQuery(LogMLAnalysisRecord.class, excludeAuthority)
                                        .filter(LogMLAnalysisRecordKeys.workflowExecutionId,
                                            workflowExecutionBaseline.getWorkflowExecutionId()),
                wingsPersistence.createUpdateOperations(LogMLAnalysisRecord.class)
                    .set(LogMLAnalysisRecordKeys.validUntil, BASELINE_TTL));

        if (updateResults.getUpdatedCount() == 0) {
          log.info(
              "for pinned baseline {} there was no analysis remaining in the db", workflowExecutionBaseline.getUuid());
        } else {
          log.info("updated {} records for baseline {}", updateResults.getUpdatedCount(),
              workflowExecutionBaseline.getUuid());
        }
        sleep(ofMillis(1000));
      }
    }
  }
}
