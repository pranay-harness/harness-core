package migrations.all;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.DeploymentType;
import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowService;

import java.util.List;

public class SetDaemonSetInWorkflowPhase implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(SetDaemonSetInWorkflowPhase.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowService workflowService;
  @Inject private ServiceResourceService serviceResourceService;

  @Override
  public void migrate() {
    logger.info("Checking for DaemonSets in all CanaryOrchestrationWorkflows");
    PageRequest<Application> pageRequest = aPageRequest().withLimit(UNLIMITED).build();
    logger.info("Retrieving applications");
    PageResponse<Application> pageResponse = wingsPersistence.query(Application.class, pageRequest);

    List<Application> apps = pageResponse.getResponse();
    if (pageResponse.isEmpty() || isEmpty(apps)) {
      logger.info("No applications found");
      return;
    }
    logger.info("Checking {} applications", apps.size());
    for (Application app : apps) {
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
            if (!workflowPhase.isDaemonSet() && isDaemonSet(app.getUuid(), workflowPhase.getServiceId())) {
              workflowModified = true;
              workflowPhase.setDaemonSet(true);
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

  private boolean isDaemonSet(String appId, String serviceId) {
    KubernetesContainerTask containerTask =
        (KubernetesContainerTask) serviceResourceService.getContainerTaskByDeploymentType(
            appId, serviceId, DeploymentType.KUBERNETES.name());
    return containerTask != null && containerTask.checkDaemonSet();
  }
}
