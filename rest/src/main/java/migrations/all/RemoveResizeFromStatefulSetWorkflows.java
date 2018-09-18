package migrations.all;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.persistence.HQuery.excludeAuthority;
import static software.wings.api.DeploymentType.KUBERNETES;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.PhaseStepType.CONTAINER_DEPLOY;

import com.google.inject.Inject;

import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.DeploymentType;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;

import java.util.List;

public class RemoveResizeFromStatefulSetWorkflows implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(RemoveResizeFromStatefulSetWorkflows.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowService workflowService;
  @Inject private ServiceResourceService serviceResourceService;

  @Override
  public void migrate() {
    logger.info("Removing resize steps from stateful set workflows");
    List<Account> accounts = wingsPersistence.createQuery(Account.class, excludeAuthority).asList();
    logger.info("Checking {} accounts", accounts.size());
    for (Account account : accounts) {
      List<Application> apps =
          wingsPersistence.createQuery(Application.class).filter(ACCOUNT_ID_KEY, account.getUuid()).asList();
      logger.info("Checking {} applications in account {}", apps.size(), account.getAccountName());
      for (Application app : apps) {
        List<Workflow> workflows =
            workflowService
                .listWorkflows(aPageRequest().withLimit(UNLIMITED).addFilter("appId", EQ, app.getUuid()).build())
                .getResponse();
        for (Workflow workflow : workflows) {
          boolean workflowModified = false;
          if (workflow.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow) {
            CanaryOrchestrationWorkflow coWorkflow = (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
            for (WorkflowPhase workflowPhase : coWorkflow.getWorkflowPhases()) {
              if (DeploymentType.KUBERNETES == workflowPhase.getDeploymentType()) {
                boolean isStatefulSet = isStatefulSet(app.getUuid(), workflowPhase.getServiceId());
                if (isStatefulSet) {
                  workflowModified = true;
                  logger.info("Found stateful set");
                  workflowPhase.setStatefulSet(true);
                  for (PhaseStep phaseStep : workflowPhase.getPhaseSteps()) {
                    if (CONTAINER_DEPLOY == phaseStep.getPhaseStepType()) {
                      GraphNode removeNode = null;
                      for (GraphNode node : phaseStep.getSteps()) {
                        if (StateType.KUBERNETES_DEPLOY.name().equals(node.getType())) {
                          removeNode = node;
                          WorkflowPhase rollbackPhase =
                              coWorkflow.getRollbackWorkflowPhaseIdMap().get(workflowPhase.getUuid());

                          for (PhaseStep rollbackPhaseStep : rollbackPhase.getPhaseSteps()) {
                            GraphNode removeRollbackNode = null;
                            for (GraphNode rollbackNode : rollbackPhaseStep.getSteps()) {
                              if (StateType.KUBERNETES_DEPLOY.name().equals(rollbackNode.getType())) {
                                removeRollbackNode = rollbackNode;
                              }
                            }
                            if (removeRollbackNode != null) {
                              logger.info("Removing deploy rollback step");
                              rollbackPhaseStep.getSteps().remove(removeRollbackNode);
                            }
                          }
                        }
                      }
                      if (removeNode != null) {
                        logger.info("Removing deploy step");
                        phaseStep.getSteps().remove(removeNode);
                      }
                    }
                  }
                }
              }
            }
          }
          if (workflowModified) {
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
    }
  }

  private boolean isStatefulSet(String appId, String serviceId) {
    KubernetesContainerTask containerTask =
        (KubernetesContainerTask) serviceResourceService.getContainerTaskByDeploymentType(
            appId, serviceId, KUBERNETES.name());
    return containerTask != null && containerTask.checkStatefulSet();
  }
}
