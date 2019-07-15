package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import io.harness.exception.WingsException.ExecutionContext;
import io.harness.logging.ExceptionLogger;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.dl.WingsPersistence;

@Slf4j
public class UpdateWorkflowExecutionDuration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try (HIterator<WorkflowExecution> iterator =
             new HIterator<>(wingsPersistence.createQuery(WorkflowExecution.class, excludeAuthority)
                                 .project(WorkflowExecutionKeys.uuid, true)
                                 .project(WorkflowExecutionKeys.startTs, true)
                                 .project(WorkflowExecutionKeys.endTs, true)
                                 .fetch())) {
      for (WorkflowExecution workflowExecution : iterator) {
        migrate(workflowExecution);
      }
    }
  }

  public void migrate(WorkflowExecution workflowExecution) {
    if (workflowExecution.getStartTs() == null || workflowExecution.getEndTs() == null) {
      return;
    }
    try {
      UpdateOperations<WorkflowExecution> updateOps =
          wingsPersistence.createUpdateOperations(WorkflowExecution.class)
              .set(WorkflowExecutionKeys.duration, workflowExecution.getEndTs() - workflowExecution.getStartTs());

      wingsPersistence.update(workflowExecution, updateOps);
    } catch (WingsException exception) {
      exception.addContext(WorkflowExecution.class, workflowExecution.getUuid());
      ExceptionLogger.logProcessedMessages(exception, ExecutionContext.MANAGER, logger);
    } catch (RuntimeException exception) {
      logger.error("", exception);
    }
  }
}
