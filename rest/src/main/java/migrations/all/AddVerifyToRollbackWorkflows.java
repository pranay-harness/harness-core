package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.VERIFY_SERVICE;
import static software.wings.beans.PhaseStepType.WRAP_UP;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;

import com.google.inject.Inject;

import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.common.Constants;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ExecutionStatus;

import java.util.List;

public class AddVerifyToRollbackWorkflows implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(AddVerifyToRollbackWorkflows.class);

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
    for (WorkflowPhase workflowPhase : coWorkflow.getWorkflowPhases()) {
      if (workflowPhase.getPhaseSteps().stream().noneMatch(step -> step.getPhaseStepType() == VERIFY_SERVICE)) {
        continue;
      }

      WorkflowPhase rollbackPhase = coWorkflow.getRollbackWorkflowPhaseIdMap().get(workflowPhase.getUuid());
      if (rollbackPhase == null) {
        continue;
      }
      if (isEmpty(rollbackPhase.getPhaseSteps())) {
        continue;
      }
      if (rollbackPhase.getPhaseSteps().stream().anyMatch(step -> step.getPhaseStepType() == VERIFY_SERVICE)) {
        continue;
      }

      int index = rollbackPhase.getPhaseSteps().size();
      if (index <= rollbackPhase.getPhaseSteps().size()
          && rollbackPhase.getPhaseSteps().get(index - 1).getPhaseStepType() == WRAP_UP) {
        --index;
      }

      rollbackPhase.getPhaseSteps().add(index,
          aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
              .withRollback(true)
              .withPhaseStepNameForRollback(Constants.VERIFY_SERVICE)
              .withStatusForRollback(ExecutionStatus.SUCCESS)
              .build());

      modified = true;
    }

    if (modified) {
      try {
        logger.info("--- Workflow updated: {}", workflow.getName());
        workflowService.updateWorkflow(workflow);
        Thread.sleep(100);
      } catch (Exception e) {
        logger.error("Error updating workflow", e);
      }
    }
  }
}
