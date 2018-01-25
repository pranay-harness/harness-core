package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.PhaseStepType.CONTAINER_SETUP;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;
import static software.wings.sm.StateType.KUBERNETES_DAEMON_SET_ROLLBACK;
import static software.wings.sm.StateType.KUBERNETES_SETUP_ROLLBACK;

import com.google.inject.Inject;

import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.Graph;
import software.wings.beans.PhaseStep;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.states.KubernetesSetupRollback;

import java.util.List;
import java.util.Map;

public class RenameK8sDaemonSetRollback implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(RenameK8sDaemonSetRollback.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowService workflowService;

  @Override
  public void migrate() {
    PageRequest<Application> pageRequest = aPageRequest().withLimit(UNLIMITED).build();
    logger.info("Retrieving applications");
    PageResponse<Application> pageResponse = wingsPersistence.query(Application.class, pageRequest);

    List<Application> apps = pageResponse.getResponse();
    if (pageResponse.isEmpty() || isEmpty(apps)) {
      logger.info("No applications found");
      return;
    }
    logger.info("Updating {} applications.", apps.size());
    for (Application app : apps) {
      List<Workflow> workflows =
          workflowService
              .listWorkflows(aPageRequest().withLimit(UNLIMITED).addFilter("appId", EQ, app.getUuid()).build())
              .getResponse();
      int updateCount = 0;
      int candidateCount = 0;
      for (Workflow workflow : workflows) {
        boolean workflowModified = false;
        boolean candidateFound = false;
        if (workflow.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow) {
          CanaryOrchestrationWorkflow coWorkflow = (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
          for (WorkflowPhase workflowPhase : coWorkflow.getRollbackWorkflowPhaseIdMap().values()) {
            for (PhaseStep phaseStep : workflowPhase.getPhaseSteps()) {
              if (CONTAINER_SETUP == phaseStep.getPhaseStepType()) {
                candidateFound = true;
                for (Graph.Node node : phaseStep.getSteps()) {
                  if (KUBERNETES_DAEMON_SET_ROLLBACK.name().equals(node.getType())) {
                    workflowModified = true;
                    node.setType(KUBERNETES_SETUP_ROLLBACK.name());
                    Map<String, Object> properties = node.getProperties();
                    properties.put("className", KubernetesSetupRollback.class.getCanonicalName());
                  }
                }
              }
            }
          }
        }
        if (candidateFound) {
          candidateCount++;
        }
        if (workflowModified) {
          try {
            logger.info("--- Workflow updated:{}", workflow.getName());
            workflowService.updateWorkflow(workflow);
            Thread.sleep(100);
          } catch (Exception e) {
            logger.error("Error updating workflow", e);
          }

          updateCount++;
        }
      }
      if (candidateCount > 0) {
        logger.info("Application migrated: {} - {}. Updated {} workflows out of {} candidates.", app.getUuid(),
            app.getName(), updateCount, candidateCount);
      }
    }
  }
}
