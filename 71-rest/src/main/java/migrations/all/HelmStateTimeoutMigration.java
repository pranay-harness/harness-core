package migrations.all;

import static software.wings.beans.Base.APP_ID_KEY;
import static software.wings.sm.StateType.HELM_DEPLOY;
import static software.wings.sm.StateType.HELM_ROLLBACK;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class HelmStateTimeoutMigration implements Migration {
  private static final String steadyStateTimeout = "steadyStateTimeout";
  private static final int minTimeoutInMs = 60000;

  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowService workflowService;

  @Override
  public void migrate() {
    logger.info("Running HelmStateTimeoutMigration");
    logger.info("Retrieving applications");

    try (HIterator<Application> apps = new HIterator<>(wingsPersistence.createQuery(Application.class).fetch())) {
      for (Application application : apps) {
        try (HIterator<Workflow> workflowHIterator = new HIterator<>(
                 wingsPersistence.createQuery(Workflow.class).filter(APP_ID_KEY, application.getUuid()).fetch())) {
          for (Workflow workflow : workflowHIterator) {
            try {
              workflowService.loadOrchestrationWorkflow(workflow, workflow.getDefaultVersion());
              updateTimeoutInWorkflow(workflow);
            } catch (Exception e) {
              logger.error("Failed to load Orchestration workflow {}", workflow.getUuid(), e);
            }
          }
        }
      }
    }

    logger.info("Completed HelmStateTimeoutMigration");
  }

  private void updateTimeoutInWorkflow(Workflow workflow) {
    boolean workflowModified = false;

    if (!(workflow.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow)) {
      return;
    }

    CanaryOrchestrationWorkflow coWorkflow = (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    if (coWorkflow.getWorkflowPhases() == null) {
      return;
    }

    Map<String, WorkflowPhase> rollbackWorkflowPhaseIdMap = coWorkflow.getRollbackWorkflowPhaseIdMap();

    List<WorkflowPhase> workflowPhaseList = new ArrayList<>();
    for (WorkflowPhase workflowPhase : coWorkflow.getWorkflowPhases()) {
      workflowPhaseList.add(workflowPhase);
      WorkflowPhase rollbackPhase = rollbackWorkflowPhaseIdMap.get(workflowPhase.getUuid());
      if (rollbackPhase != null) {
        workflowPhaseList.add(rollbackPhase);
      }

      for (WorkflowPhase phase : workflowPhaseList) {
        for (PhaseStep phaseStep : phase.getPhaseSteps()) {
          for (GraphNode node : phaseStep.getSteps()) {
            workflowModified = updateGraphNode(node, workflow) || workflowModified;
          }
        }
      }

      workflowPhaseList.clear();
    }

    if (workflowModified) {
      try {
        logger.info("Updating workflow: {} - {}", workflow.getUuid(), workflow.getName());
        workflowService.updateWorkflow(workflow, false);
      } catch (Exception e) {
        logger.error("Error updating workflow", e);
      }
    }
  }

  private boolean updateGraphNode(GraphNode node, Workflow workflow) {
    boolean workflowModified = false;

    if (HELM_DEPLOY.name().equals(node.getType()) || HELM_ROLLBACK.name().equals(node.getType())) {
      Map<String, Object> properties = node.getProperties();
      if (properties != null && properties.containsKey(steadyStateTimeout)) {
        Object timeOutObject = properties.get(steadyStateTimeout);
        if (timeOutObject != null) {
          try {
            int timeout = (int) timeOutObject;
            if (timeout > minTimeoutInMs) {
              int updatedTimeout = timeout / minTimeoutInMs;
              workflowModified = true;
              properties.put(steadyStateTimeout, updatedTimeout);
              logger.info("Updating the timeout from {} to {} for state {} in workflowId {}", timeout, updatedTimeout,
                  node.getType(), workflow.getUuid());
            }
          } catch (ClassCastException ex) {
            logger.info("Failed to convert timeout to integer for workflowId {}", workflow.getUuid());
          }
        }
      }
    }

    return workflowModified;
  }
}
