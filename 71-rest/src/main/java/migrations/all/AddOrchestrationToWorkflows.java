package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Workflow;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowService;

@Slf4j
public class AddOrchestrationToWorkflows implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowService workflowService;

  @Override
  @SuppressWarnings("PMD")
  public void migrate() {
    try (HIterator<Workflow> workflowIterator =
             new HIterator<>(wingsPersistence.createQuery(Workflow.class, excludeAuthority)
                                 .field(Workflow.ORCHESTRATION_KEY)
                                 .doesNotExist()
                                 .project(Workflow.ID_KEY, true)
                                 .project(Workflow.APP_ID_KEY, true)
                                 .fetch())) {
      for (Workflow workflow : workflowIterator) {
        try {
          Workflow readWorkflow = workflowService.readWorkflow(workflow.getAppId(), workflow.getUuid());
          workflowService.updateWorkflow(readWorkflow, false);
        } catch (Throwable exception) {
          logger.error("Exception while migrating orchestration for {}", workflow.getUuid(), exception);
        }
      }
    }
  }
}
