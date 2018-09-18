package migrations.all;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static software.wings.sm.StateType.HELM_DEPLOY;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HelmReleaseNamePrefixMigration implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(HelmReleaseNamePrefixMigration.class);

  private static final String HELM_RELEASE_NAME_PREFIX_KEY = "helmReleaseNamePrefix";
  private static final String HELM_RELEASE_NAME_PREFIX_DEFAULT_VALUE = "${app.name}-${service.name}-${env.name}";

  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowService workflowService;

  @Override
  public void migrate() {
    logger.info("Retrieving applications");

    try (HIterator<Application> apps = new HIterator<>(wingsPersistence.createQuery(Application.class).fetch())) {
      while (apps.hasNext()) {
        Application application = apps.next();
        logger.info("Updating app {}", application.getUuid());
        List<Workflow> workflows =
            workflowService
                .listWorkflows(
                    aPageRequest().withLimit(UNLIMITED).addFilter("appId", EQ, application.getUuid()).build())
                .getResponse();

        for (Workflow workflow : workflows) {
          updateWorkflowsWithHelmReleaseNamePrefix(workflow, HELM_DEPLOY);
        }
        logger.info("Completed updating app {}", application.getUuid());
      }
    }

    logger.info("Updated all apps");
  }

  private void updateWorkflowsWithHelmReleaseNamePrefix(Workflow workflow, StateType stateType) {
    boolean workflowModified = false;

    if (workflow.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow) {
      CanaryOrchestrationWorkflow coWorkflow = (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();

      if (coWorkflow.getWorkflowPhases() == null) {
        return;
      }

      for (WorkflowPhase workflowPhase : coWorkflow.getWorkflowPhases()) {
        List<WorkflowPhase> workflowPhases = new ArrayList<>();
        workflowPhases.add(workflowPhase);
        WorkflowPhase rollbackPhase = coWorkflow.getRollbackWorkflowPhaseIdMap().get(workflowPhase.getUuid());
        if (rollbackPhase != null) {
          workflowPhases.add(rollbackPhase);
        }

        for (WorkflowPhase phase : workflowPhases) {
          for (PhaseStep phaseStep : phase.getPhaseSteps()) {
            for (GraphNode node : phaseStep.getSteps()) {
              if (stateType.name().equals(node.getType())) {
                Map<String, Object> properties = node.getProperties();
                if (properties != null && !properties.containsKey(HELM_RELEASE_NAME_PREFIX_KEY)) {
                  workflowModified = true;
                  properties.put(HELM_RELEASE_NAME_PREFIX_KEY, HELM_RELEASE_NAME_PREFIX_DEFAULT_VALUE);
                }
              }
            }
          }
        }
      }
    }

    if (workflowModified) {
      try {
        logger.info("Updating workflow: {} - {}", workflow.getUuid(), workflow.getName());
        workflowService.updateWorkflow(workflow);
        Thread.sleep(100);
      } catch (Exception e) {
        logger.error("Error updating workflow", e);
      }
    }
  }
}
