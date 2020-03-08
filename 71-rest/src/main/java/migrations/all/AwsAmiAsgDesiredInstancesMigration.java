package migrations.all;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.service.impl.aws.model.AwsConstants.DEFAULT_AMI_ASG_DESIRED_INSTANCES;

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
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class AwsAmiAsgDesiredInstancesMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowService workflowService;

  private static final String MAX_INTANCES_KEY = "maxInstances";
  private static final String DESIRED_INTANCES_KEY = "desiredInstances";

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

        if (isNotEmpty(workflows)) {
          for (Workflow workflow : workflows) {
            try {
              updateWorkflow(workflow);
            } catch (Exception ex) {
              logger.error("Error updating workflow: [{}]", workflow.getUuid(), ex);
            }
          }
        }
        logger.info("Completed updating app {}", application.getUuid());
      }
    }

    logger.info("Updated all apps");
    logger.info("Finished running AwsAmiAsgDesiredInstancesMigration");
  }

  private void updateWorkflow(Workflow workflow) throws Exception {
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
          List<PhaseStep> phaseSteps = phase.getPhaseSteps();
          if (isNotEmpty(phaseSteps)) {
            for (PhaseStep phaseStep : phaseSteps) {
              List<GraphNode> steps = phaseStep.getSteps();
              if (isNotEmpty(steps)) {
                for (GraphNode node : steps) {
                  if (StateType.AWS_AMI_SERVICE_SETUP.name().equals(node.getType())) {
                    workflowModified = true;
                    Map<String, Object> properties = node.getProperties();
                    if (properties == null) {
                      properties = new HashMap<>();
                    }
                    int desiredInstances = DEFAULT_AMI_ASG_DESIRED_INSTANCES;
                    if (isNotEmpty(properties)) {
                      Object o = properties.get(MAX_INTANCES_KEY);
                      if (o instanceof Integer) {
                        desiredInstances = (Integer) o;
                      }
                    }
                    properties.put(DESIRED_INTANCES_KEY, desiredInstances);
                  }
                }
              }
            }
          }
        }
      }
    }

    if (workflowModified) {
      logger.info("Updating workflow: {} - {}", workflow.getUuid(), workflow.getName());
      workflowService.updateWorkflow(workflow, false);
      Thread.sleep(100);
    }
  }
}