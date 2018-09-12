package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;
import static software.wings.sm.StateType.KUBERNETES_SETUP;

import com.google.inject.Inject;

import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AddReplCtrlNameToKubeSetupProperties implements Migration {
  private static Logger logger = LoggerFactory.getLogger(AddReplCtrlNameToKubeSetupProperties.class);

  private static final String DEFAULT_REPLICATION_CONTROLLER_NAME = "${app.name}.${service.name}.${env.name}";
  private static final String REPLICATION_CONTROLLER_NAME_KEY = "replicationControllerName";

  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowService workflowService;

  @Override
  public void migrate() {
    logger.info("Retrieving applications");
    PageRequest<Application> pageRequest = aPageRequest().withLimit(UNLIMITED).build();
    PageResponse<Application> pageResponse = wingsPersistence.query(Application.class, pageRequest, excludeAuthority);

    List<Application> apps = pageResponse.getResponse();
    if (pageResponse.isEmpty() || isEmpty(apps)) {
      logger.info("No applications found");
      return;
    }

    logger.info("Updating {} applications", apps.size());
    for (Application app : apps) {
      logger.info("Updating app {}", app.getUuid());
      List<Workflow> workflows =
          workflowService
              .listWorkflows(aPageRequest().withLimit(UNLIMITED).addFilter("appId", EQ, app.getUuid()).build())
              .getResponse();

      for (Workflow workflow : workflows) {
        updateWorkflowsWithReplCtrlName(workflow, KUBERNETES_SETUP);
      }
      logger.info("Completed updating app {}", app.getUuid());
    }

    logger.info("Updated all apps");
  }

  private void updateWorkflowsWithReplCtrlName(Workflow workflow, StateType stateType) {
    boolean workflowModified = false;
    if (workflow.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow) {
      CanaryOrchestrationWorkflow coWorkflow = (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
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
                if (!properties.containsKey(REPLICATION_CONTROLLER_NAME_KEY)) {
                  workflowModified = true;
                  properties.put(REPLICATION_CONTROLLER_NAME_KEY, DEFAULT_REPLICATION_CONTROLLER_NAME);
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
