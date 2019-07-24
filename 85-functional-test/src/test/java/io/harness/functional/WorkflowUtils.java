package io.harness.functional;

import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.exception.WingsException;
import org.awaitility.Awaitility;
import software.wings.beans.GraphNode;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.service.intfc.WorkflowExecutionService;

import java.util.concurrent.TimeUnit;

@Singleton
public class WorkflowUtils {
  private static final long TEST_TIMEOUT_IN_MINUTES = 3;

  @Inject private WorkflowExecutionService workflowExecutionService;

  public void checkForWorkflowSuccess(WorkflowExecution workflowExecution) {
    Awaitility.await().atMost(TEST_TIMEOUT_IN_MINUTES, TimeUnit.MINUTES).pollInterval(5, TimeUnit.SECONDS).until(() -> {
      ExecutionStatus status =
          workflowExecutionService.getWorkflowExecution(workflowExecution.getAppId(), workflowExecution.getUuid())
              .getStatus();
      return status == ExecutionStatus.SUCCESS || status == ExecutionStatus.FAILED;
    });
    WorkflowExecution finalWorkflowExecution =
        workflowExecutionService.getWorkflowExecution(workflowExecution.getAppId(), workflowExecution.getUuid());
    if (finalWorkflowExecution.getStatus() != ExecutionStatus.SUCCESS) {
      throw new WingsException(
          "workflow execution did not succeed. Final status: " + finalWorkflowExecution.getStatus());
    }
  }
  public Workflow buildCanaryWorkflowPostDeploymentStep(String name, String envId, GraphNode graphNode) {
    return aWorkflow()
        .name(name)
        .envId(envId)
        .workflowType(WorkflowType.ORCHESTRATION)
        .orchestrationWorkflow(aCanaryOrchestrationWorkflow()
                                   .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
                                   .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).addStep(graphNode).build())
                                   .build())
        .build();
  }
  public Workflow buildCanaryWorkflowPostDeploymentStep(String name, String envId) {
    return aWorkflow()
        .name(name)
        .envId(envId)
        .workflowType(WorkflowType.ORCHESTRATION)
        .orchestrationWorkflow(aCanaryOrchestrationWorkflow()
                                   .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
                                   .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
                                   .build())
        .build();
  }
}
