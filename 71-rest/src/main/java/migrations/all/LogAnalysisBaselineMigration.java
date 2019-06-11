package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;
import static software.wings.service.impl.WorkflowExecutionBaselineServiceImpl.BASELINE_TTL;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.UpdateResults;
import software.wings.beans.baseline.WorkflowExecutionBaseline;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogMLAnalysisRecord.LogMLAnalysisRecordKeys;

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
          logger.info(
              "for pinned baseline {} there was no analysis remaining in the db", workflowExecutionBaseline.getUuid());
        } else {
          logger.info("updated {} records for baseline {}", updateResults.getUpdatedCount(),
              workflowExecutionBaseline.getUuid());
        }
        sleep(ofMillis(1000));
      }
    }
  }
}
