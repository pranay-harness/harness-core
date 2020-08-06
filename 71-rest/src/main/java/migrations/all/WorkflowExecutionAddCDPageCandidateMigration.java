package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;
import static software.wings.service.impl.WorkflowExecutionServiceHelper.calculateCdPageCandidate;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.dl.WingsPersistence;

@Slf4j
public class WorkflowExecutionAddCDPageCandidateMigration implements Migration {
  private final String DEBUG_LINE = "WORKFLOW_EXECUTION_MIGRATION: ";

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    int count = 0;
    logger.info(DEBUG_LINE + "Starting migration");
    try (HIterator<WorkflowExecution> iterator =
             new HIterator<>(wingsPersistence.createQuery(WorkflowExecution.class, excludeAuthority)
                                 .field(WorkflowExecutionKeys.cdPageCandidate)
                                 .doesNotExist()
                                 .order(Sort.descending(WorkflowExecutionKeys.createdAt))
                                 .fetch())) {
      for (WorkflowExecution workflowExecution : iterator) {
        migrate(workflowExecution);
        count++;
      }
      logger.info(DEBUG_LINE + "Completed migrating {} records", count);
    } catch (Exception e) {
      logger.error(DEBUG_LINE + "Failed to complete migration", e);
    }
  }

  private void migrate(WorkflowExecution workflowExecution) {
    try {
      String pipelineExecutionId = workflowExecution.getPipelineExecutionId(); // isEmpty --> Candidate else not
      String pipelineResumeId =
          workflowExecution.getPipelineResumeId(); // if pipelineResumeId is empty--> then pipeline was never resumed.
                                                   // Latest pipeline resume is still false in this case
      boolean latestPipelineResume =
          workflowExecution
              .isLatestPipelineResume(); // if pipelineResumeId is NotEmpty--> then latestPipelineResume must be true

      boolean cdPageCandidate = calculateCdPageCandidate(pipelineExecutionId, pipelineResumeId, latestPipelineResume);
      UpdateOperations<WorkflowExecution> updateOps = wingsPersistence.createUpdateOperations(WorkflowExecution.class)
                                                          .set(WorkflowExecutionKeys.cdPageCandidate, cdPageCandidate);
      wingsPersistence.update(workflowExecution, updateOps);

    } catch (Exception e) {
      logger.error(
          DEBUG_LINE + "Exception occurred while processing workflowExecution:[{}]", workflowExecution.getUuid(), e);
    }
  }
}
