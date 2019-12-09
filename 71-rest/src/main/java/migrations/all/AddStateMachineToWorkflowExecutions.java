package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import io.harness.threading.Morpheus;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.StateMachine;

import java.time.Duration;

@Slf4j
public class AddStateMachineToWorkflowExecutions implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowExecutionService workflowExecutionService;

  @Override
  @SuppressWarnings("PMD")
  public void migrate() {
    try (HIterator<WorkflowExecution> workflowExecutionIterator =
             new HIterator<>(wingsPersistence.createQuery(WorkflowExecution.class, excludeAuthority)
                                 .field(WorkflowExecutionKeys.stateMachine)
                                 .doesNotExist()
                                 .field(WorkflowExecutionKeys.stateMachineId)
                                 .exists()
                                 .project(WorkflowExecutionKeys.uuid, true)
                                 .project(WorkflowExecutionKeys.appId, true)
                                 .project(WorkflowExecutionKeys.stateMachineId, true)
                                 .fetch())) {
      while (workflowExecutionIterator.hasNext()) {
        WorkflowExecution workflowExecution = workflowExecutionIterator.next();
        try {
          StateMachine stateMachine = workflowExecutionService.obtainStateMachine(workflowExecution);
          if (stateMachine != null) {
            final Query<WorkflowExecution> query = wingsPersistence.createQuery(WorkflowExecution.class)
                                                       .filter(WorkflowExecutionKeys.uuid, workflowExecution.getUuid());

            final UpdateOperations<WorkflowExecution> updateOperations =
                wingsPersistence.createUpdateOperations(WorkflowExecution.class)
                    .set(WorkflowExecutionKeys.stateMachine, stateMachine)
                    .unset(WorkflowExecutionKeys.stateMachineId);

            wingsPersistence.update(query, updateOperations);
          }

        } catch (Throwable exception) {
          logger.error("Exception while migrating workflowExecution for {}", workflowExecution.getUuid(), exception);
        }

        Morpheus.sleep(Duration.ofMillis(10));
      }
    }
  }
}
