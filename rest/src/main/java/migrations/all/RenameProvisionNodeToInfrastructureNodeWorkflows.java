package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;
import static software.wings.beans.PhaseStepType.INFRASTRUCTURE_NODE;
import static software.wings.beans.PhaseStepType.PROVISION_NODE;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.common.Constants.INFRASTRUCTURE_NODE_NAME;
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
import java.util.concurrent.atomic.AtomicInteger;

public class RenameProvisionNodeToInfrastructureNodeWorkflows implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(RenameProvisionNodeToInfrastructureNodeWorkflows.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowService workflowService;

  AtomicInteger count = new AtomicInteger(0);

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

    boolean updated = false;

    for (WorkflowPhase workflowPhase :
        ((CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow()).getWorkflowPhases()) {
      PhaseStep provisionPhaseStep = workflowPhase.getPhaseSteps()
                                         .stream()
                                         .filter(ps -> ps.getPhaseStepType() == PROVISION_NODE)
                                         .findFirst()
                                         .orElse(null);
      if (provisionPhaseStep != null) {
        updated = true;
        provisionPhaseStep.setPhaseStepType(INFRASTRUCTURE_NODE);
        provisionPhaseStep.setName(INFRASTRUCTURE_NODE_NAME);
      }
    }

    if (updated) {
      try {
        logger.info("--- {} Workflow updated: {}", count.incrementAndGet(), workflow.getName());
        workflowService.updateWorkflow(workflow);
        Thread.sleep(100);
      } catch (Exception e) {
        logger.error("Error updating workflow", e);
      }
    }
  }
}
