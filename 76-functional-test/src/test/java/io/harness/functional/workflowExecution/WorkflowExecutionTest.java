package io.harness.functional.workflowExecution;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;

import com.google.inject.Inject;

import io.harness.beans.WorkflowType;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.WorkflowGenerator;
import io.harness.persistence.HIterator;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.ExecutionCredential.ExecutionType;
import software.wings.beans.SSHExecutionCredential;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.service.intfc.WorkflowExecutionService;

public class WorkflowExecutionTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private WorkflowGenerator workflowGenerator;
  @Inject private WorkflowExecutionService workflowExecutionService;

  public Workflow buildWorkflow(Seed seed, Owners owners) {
    // Test  creating a workflow
    Workflow workflow = workflowGenerator.ensureWorkflow(seed, owners,
        aWorkflow()
            .name("Workflow - " + generateUuid())
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(aCanaryOrchestrationWorkflow()
                                       .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
                                       .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
                                       .build())
            .build());

    resetCache();
    return workflow;
  }

  @NotNull
  public WorkflowExecution executeWorkflow(Workflow workflow, Application application, Environment environment) {
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(workflow.getWorkflowType());
    executionArgs.setExecutionCredential(
        SSHExecutionCredential.Builder.aSSHExecutionCredential().withExecutionType(ExecutionType.SSH).build());
    executionArgs.setOrchestrationId(workflow.getUuid());

    WorkflowExecution workflowExecution =
        runWorkflow(bearerToken, application.getUuid(), environment.getUuid(), executionArgs);
    assertThat(workflowExecution).isNotNull();
    return workflowExecution;
  }

  @Test
  @Category({FunctionalTests.class})
  public void fetchExecutionsInRange() throws Exception {
    final Seed seed = new Seed(0);
    Owners owners = ownerManager.create();
    Workflow workflow = buildWorkflow(seed, owners);

    final Application application = owners.obtainApplication();
    final Environment environment = owners.obtainEnvironment();

    // Test running the workflow

    WorkflowExecution workflowExecution1 = executeWorkflow(workflow, application, environment);
    WorkflowExecution workflowExecution2 = executeWorkflow(workflow, application, environment);

    {
      try (HIterator<WorkflowExecution> executions = workflowExecutionService.executions(
               application.getUuid(), workflowExecution1.getStartTs(), workflowExecution2.getStartTs(), null)) {
        assertThat(executions.hasNext()).isTrue();
        assertThat(executions.next().getUuid()).isEqualTo(workflowExecution1.getUuid());
      }
    }
  }
}
