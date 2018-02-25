package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.beans.PhaseStepType.ENABLE_SERVICE;
import static software.wings.beans.PhaseStepType.VERIFY_SERVICE;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;

import com.google.inject.Inject;

import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.PhaseStep;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowService;

import java.util.List;

public class VerifyStepWorkflowOrder implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(VerifyStepWorkflowOrder.class);

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
      migrate(workflow, application.getAccountId());
    }
  }

  public void migrate(Workflow workflow, String accountId) {
    if (!(workflow.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow)) {
      return;
    }

    boolean force = "JWNrP_OyRrSL6qe9pCSI0g".equals(accountId);

    boolean modified = false;

    CanaryOrchestrationWorkflow coWorkflow = (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    for (WorkflowPhase workflowPhase : coWorkflow.getWorkflowPhases()) {
      {
        final List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();
        if (swap(phaseSteps, force)) {
          modified = true;
          workflowPhase.setPhaseSteps(phaseSteps);
        }
      }
      WorkflowPhase rollbackPhase = coWorkflow.getRollbackWorkflowPhaseIdMap().get(workflowPhase.getUuid());
      if (rollbackPhase != null) {
        final List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();
        if (swap(phaseSteps, force)) {
          modified = true;
          workflowPhase.setPhaseSteps(phaseSteps);
        }
      }
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

  public boolean swap(List<PhaseStep> phaseSteps, boolean force) {
    boolean swapped = false;
    for (int i = 1; i < phaseSteps.size(); ++i) {
      final PhaseStep phaseStep1 = phaseSteps.get(i - 1);
      if (phaseStep1.getPhaseStepType() != VERIFY_SERVICE) {
        continue;
      }

      final PhaseStep phaseStep2 = phaseSteps.get(i);
      if (phaseStep2.getPhaseStepType() != ENABLE_SERVICE) {
        continue;
      }

      if (isNotEmpty(phaseStep1.getSteps()) && isNotEmpty(phaseStep2.getSteps())) {
        if (!force) {
          continue;
        }
        logger.info("Migration is forced");
      }

      swapped = true;
      phaseSteps.set(i - 1, phaseStep2);
      phaseSteps.set(i, phaseStep1);
    }
    return swapped;
  }
}
