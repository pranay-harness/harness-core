package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import io.harness.threading.Morpheus;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.StateMachine;

import java.time.Duration;

public class AddStateMachineToWorkflowExecutions implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(AddStateMachineToWorkflowExecutions.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowExecutionService workflowExecutionService;

  @Override
  @SuppressWarnings("PMD")
  public void migrate() {
    try (HIterator<WorkflowExecution> workflowExecutionIterator =
             new HIterator<>(wingsPersistence.createQuery(WorkflowExecution.class, excludeAuthority)
                                 .field(WorkflowExecution.STATE_MACHINE_KEY)
                                 .doesNotExist()
                                 .field(WorkflowExecution.STATE_MACHINE_ID_KEY)
                                 .exists()
                                 .project(WorkflowExecution.ID_KEY, true)
                                 .project(WorkflowExecution.APP_ID_KEY, true)
                                 .project(WorkflowExecution.STATE_MACHINE_ID_KEY, true)
                                 .fetch())) {
      while (workflowExecutionIterator.hasNext()) {
        WorkflowExecution workflowExecution = workflowExecutionIterator.next();
        try {
          StateMachine stateMachine = workflowExecutionService.obtainStateMachine(workflowExecution);

          final Query<WorkflowExecution> query = wingsPersistence.createQuery(WorkflowExecution.class)
                                                     .filter(WorkflowExecution.ID_KEY, workflowExecution.getUuid());

          final UpdateOperations<WorkflowExecution> updateOperations =
              wingsPersistence.createUpdateOperations(WorkflowExecution.class)
                  .set(WorkflowExecution.STATE_MACHINE_KEY, stateMachine)
                  .unset(WorkflowExecution.STATE_MACHINE_ID_KEY);

          wingsPersistence.update(query, updateOperations);

        } catch (Throwable exception) {
          logger.error(String.format("Exception while migrating workflowExecution for %s", workflowExecution.getUuid()),
              exception);
        }

        Morpheus.sleep(Duration.ofMillis(10));
      }
    }
  }
}
