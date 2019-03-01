package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Workflow;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowService;

public class AddOrchestrationToWorkflows implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(AddOrchestrationToWorkflows.class);

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
      while (workflowIterator.hasNext()) {
        Workflow workflow = workflowIterator.next();
        try {
          Workflow readWorkflow = workflowService.readWorkflow(workflow.getAppId(), workflow.getUuid());
          workflowService.updateWorkflow(readWorkflow);
        } catch (Throwable exception) {
          logger.error(String.format("Exception while migrating orchestration for %s", workflow.getUuid()), exception);
        }
      }
    }
  }
}
