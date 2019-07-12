package migrations.all;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.PhaseStepType.CONTAINER_SETUP;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.api.DeploymentType;
import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Map;

@Slf4j
public class FixMaxInstancesFieldInContainerSetup implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowService workflowService;

  @Override
  public void migrate() {
    logger.info("Retrieving applications");
    try (HIterator<Application> iterator = new HIterator<>(wingsPersistence.createQuery(Application.class).fetch())) {
      for (Application app : iterator) {
        List<Workflow> workflows =
            workflowService
                .listWorkflows(aPageRequest().withLimit(UNLIMITED).addFilter("appId", EQ, app.getUuid()).build())
                .getResponse();
        int updateCount = 0;
        for (Workflow workflow : workflows) {
          boolean workflowModified = false;
          if (workflow.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow) {
            CanaryOrchestrationWorkflow coWorkflow = (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
            for (WorkflowPhase workflowPhase : coWorkflow.getWorkflowPhases()) {
              if (workflowPhase.getDeploymentType() == DeploymentType.KUBERNETES) {
                for (PhaseStep phaseStep : workflowPhase.getPhaseSteps()) {
                  if (CONTAINER_SETUP == phaseStep.getPhaseStepType()) {
                    for (GraphNode node : phaseStep.getSteps()) {
                      if (StateType.KUBERNETES_SETUP.name().equals(node.getType())
                          || StateType.ECS_SERVICE_SETUP.name().equals(node.getType())) {
                        Map<String, Object> properties = node.getProperties();
                        Object value = properties.get("maxInstances");
                        if (value != null) {
                          if (value instanceof String) {
                            if (((String) value).contains("randomKey")) {
                              logger.info("Resetting [{}] to 2", value);
                              workflowModified = true;
                              properties.put("maxInstances", "2");
                            }
                          } else {
                            logger.info("Setting [{}] to string value", value.toString());
                            workflowModified = true;
                            properties.put("maxInstances", value.toString());
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
          if (workflowModified) {
            try {
              logger.info("... Updating workflow: {} - {}", workflow.getUuid(), workflow.getName());
              workflowService.updateWorkflow(workflow);
              Thread.sleep(100);
            } catch (Exception e) {
              logger.error("Error updating workflow", e);
            }
            updateCount++;
          }
        }
        logger.info("Application migrated: {} - {}. Updated {} out of {} workflows", app.getUuid(), app.getName(),
            updateCount, workflows.size());
      }
    }
  }
}
