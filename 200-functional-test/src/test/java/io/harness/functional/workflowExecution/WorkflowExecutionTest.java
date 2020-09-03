package io.harness.functional.workflowExecution;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.GEORGE;
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
import io.harness.rule.Owner;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.service.intfc.WorkflowExecutionService;

import java.util.Collections;

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

    return workflow;
  }

  @NotNull
  public WorkflowExecution executeWorkflow(Workflow workflow, Application application, Environment environment) {
    WorkflowExecution workflowExecution = runWorkflow(bearerToken, application.getUuid(), environment.getUuid(),
        workflow.getUuid(), Collections.<Artifact>emptyList());
    assertThat(workflowExecution).isNotNull();
    return workflowExecution;
  }

  @Test
  @Owner(developers = GEORGE)
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
