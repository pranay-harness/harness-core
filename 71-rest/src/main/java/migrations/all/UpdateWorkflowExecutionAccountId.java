package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import io.harness.exception.WingsException.ExecutionContext;
import io.harness.logging.ExceptionLogger;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Application;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.dl.WingsPersistence;

@Slf4j
public class UpdateWorkflowExecutionAccountId implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    int count = 0;
    try (HIterator<WorkflowExecution> iterator =
             new HIterator<>(wingsPersistence.createQuery(WorkflowExecution.class, excludeAuthority)
                                 .order(Sort.descending(WorkflowExecutionKeys.createdAt))
                                 .field(WorkflowExecutionKeys.accountId)
                                 .doesNotExist()
                                 .project(WorkflowExecutionKeys.appId, true)
                                 .fetch())) {
      for (WorkflowExecution workflowExecution : iterator) {
        migrate(workflowExecution);
        count++;
      }
      if (count % 1000 == 0) {
        logger.info("Completed migrating {} records", count);
      }
    } catch (Exception e) {
      logger.error("Failed to complete migration", e);
    }
  }

  public void migrate(WorkflowExecution workflowExecution) {
    try {
      Application application = wingsPersistence.get(Application.class, workflowExecution.getAppId());
      if (application != null) {
        UpdateOperations<WorkflowExecution> updateOps =
            wingsPersistence.createUpdateOperations(WorkflowExecution.class)
                .set(WorkflowExecutionKeys.accountId, application.getAccountId());
        wingsPersistence.update(workflowExecution, updateOps);
      } else {
        logger.info("No app found with appID:[{}]", workflowExecution.getAppId());
      }

    } catch (WingsException exception) {
      exception.addContext(WorkflowExecution.class, workflowExecution.getUuid());
      ExceptionLogger.logProcessedMessages(exception, ExecutionContext.MANAGER, logger);
    } catch (Exception e) {
      logger.error("Exception occurred while processing workflowExecution:[{}]", workflowExecution.getUuid(), e);
    }
  }
}
