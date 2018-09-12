package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;

import com.google.inject.Inject;

import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.GraphNode;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowService;

import java.util.List;

public class SetRollbackFlagToWorkflows implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(SetRollbackFlagToWorkflows.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowService workflowService;

  @Override
  public void migrate() {
    PageRequest<Application> pageRequest = aPageRequest().withLimit(UNLIMITED).build();
    logger.info("Retrieving applications");
    PageResponse<Application> pageResponse = wingsPersistence.query(Application.class, pageRequest, excludeAuthority);

    List<Application> apps = pageResponse.getResponse();
    if (pageResponse.isEmpty() || isEmpty(apps)) {
      logger.info("No applications found");
      return;
    }
    logger.info("Updating {} applications.", apps.size());
    for (Application app : apps) {
      migrate(app);
    }
  }

  public void migrate(Application application) {
    List<Workflow> workflows =
        workflowService
            .listWorkflows(aPageRequest().withLimit(UNLIMITED).addFilter("appId", EQ, application.getUuid()).build())
            .getResponse();

    logger.info("Updating {} workflows.", workflows.size());
    for (Workflow workflow : workflows) {
      migrate(workflow);
    }
  }

  public void migrate(Workflow workflow) {
    if (!(workflow.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow)) {
      return;
    }

    boolean modified = false;

    CanaryOrchestrationWorkflow coWorkflow = (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    for (WorkflowPhase workflowPhase : coWorkflow.getWorkflowPhaseIdMap().values()) {
      modified = modified || workflowPhase.isRollback()
          || workflowPhase.getPhaseSteps().stream().anyMatch(
                 phaseStep -> phaseStep.isRollback() || phaseStep.getSteps().stream().anyMatch(GraphNode::isRollback));

      workflowPhase.setRollback(false);
      workflowPhase.getPhaseSteps().forEach(phaseStep -> {
        phaseStep.setRollback(false);
        phaseStep.getSteps().forEach(step -> step.setRollback(false));
      });
    }

    for (WorkflowPhase workflowPhase : coWorkflow.getRollbackWorkflowPhaseIdMap().values()) {
      modified = modified || !workflowPhase.isRollback()
          || workflowPhase.getPhaseSteps().stream().anyMatch(phaseStep
                 -> !phaseStep.isRollback() || phaseStep.getSteps().stream().anyMatch(step -> !step.isRollback()));
      workflowPhase.setRollback(true);
      workflowPhase.getPhaseSteps().forEach(phaseStep -> {
        phaseStep.setRollback(true);
        phaseStep.getSteps().forEach(step -> step.setRollback(true));
      });
    }

    if (modified) {
      try {
        logger.info("--- Workflow updated: {}, {}", workflow.getUuid(), workflow.getName());
        workflowService.updateWorkflow(workflow);
        Thread.sleep(100);
      } catch (Exception e) {
        logger.error("Error updating workflow", e);
      }
    }
  }
}
