package io.harness.functional;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;

import com.google.inject.Inject;

import io.harness.beans.OrchestrationWorkflowType;
import io.harness.beans.WorkflowType;
import io.harness.category.element.FunctionalTests;
import io.harness.generator.InfrastructureDefinitionGenerator;
import io.harness.generator.InfrastructureDefinitionGenerator.InfrastructureDefinitions;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.ServiceGenerator;
import io.harness.generator.ServiceGenerator.Services;
import io.harness.rule.OwnerRule.Owner;
import io.harness.testframework.restutils.WorkflowRestUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.BasicOrchestrationWorkflow;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.GraphNode;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowPhase;
import software.wings.infra.InfrastructureDefinition;

import java.util.Map;

public class HelmFunctionalTest extends AbstractFunctionalTest {
  private static final String WORKFLOW_NAME = "Helm S3 Functional Test";
  private static final String RELEASE_NAME = "functional-test";
  private static final String HELM_RELEASE_NAME_PREFIX = "helmReleaseNamePrefix";

  @Inject private OwnerManager ownerManager;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private InfrastructureDefinitionGenerator infrastructureDefinitionGenerator;
  @Inject private WorkflowUtils workflowUtils;
  private Owners owners;
  private InfrastructureDefinition infrastructureDefinition;
  private Workflow workflow;

  private final Seed seed = new Seed(0);

  @Before
  public void setUp() throws Exception {
    owners = ownerManager.create();
    infrastructureDefinition =
        infrastructureDefinitionGenerator.ensurePredefined(seed, owners, InfrastructureDefinitions.AZURE_HELM);
    resetCache(owners.obtainAccount().getUuid());
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(FunctionalTests.class)
  @Ignore("Working locally, need to install helm on Jenkins box")
  public void testHelmS3WorkflowExecution() {
    Service helmS3Service = serviceGenerator.ensurePredefined(seed, owners, Services.HELM_S3);
    workflow = createWorkflow(helmS3Service, infrastructureDefinition);
    updateReleaseNameInWorkflow(workflow);

    resetCache(owners.obtainAccount().getUuid());
    ExecutionArgs executionArgs = getExecutionArgs();

    WorkflowExecution workflowExecution = WorkflowRestUtils.startWorkflow(
        bearerToken, helmS3Service.getAppId(), infrastructureDefinition.getEnvId(), executionArgs);
    resetCache(owners.obtainAccount().getUuid());
    workflowUtils.checkForWorkflowSuccess(workflowExecution);
  }

  @NotNull
  private ExecutionArgs getExecutionArgs() {
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setOrchestrationId(workflow.getUuid());
    executionArgs.setWorkflowType(workflow.getWorkflowType());
    return executionArgs;
  }

  private void updateReleaseNameInWorkflow(Workflow workflow) {
    BasicOrchestrationWorkflow orchestrationWorkflow = (BasicOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    WorkflowPhase workflowPhase = orchestrationWorkflow.getWorkflowPhases().get(0);
    GraphNode helmDeployStep = workflowPhase.getPhaseSteps().get(0).getSteps().get(0);
    Map<String, Object> properties = helmDeployStep.getProperties();
    properties.put(HELM_RELEASE_NAME_PREFIX, RELEASE_NAME);
    helmDeployStep.setProperties(properties);

    WorkflowRestUtils.updateWorkflowPhase(bearerToken, owners.obtainAccount().getUuid(), workflow.getAppId(),
        workflow.getUuid(), workflowPhase.getUuid(), workflowPhase);
  }

  private Workflow createWorkflow(Service helmS3Service, InfrastructureDefinition infrastructureDefinition) {
    Workflow workflow = aWorkflow()
                            .name(WORKFLOW_NAME)
                            .appId(helmS3Service.getAppId())
                            .envId(infrastructureDefinition.getEnvId())
                            .serviceId(helmS3Service.getUuid())
                            .infraDefinitionId(infrastructureDefinition.getUuid())
                            .workflowType(WorkflowType.ORCHESTRATION)
                            .orchestrationWorkflow(aCanaryOrchestrationWorkflow().build())
                            .build();
    workflow.getOrchestrationWorkflow().setOrchestrationWorkflowType(OrchestrationWorkflowType.BASIC);
    return WorkflowRestUtils.createWorkflow(
        bearerToken, owners.obtainAccount().getUuid(), helmS3Service.getAppId(), workflow);
  }

  @After
  public void tearDown() throws Exception {
    if (workflow != null) {
      WorkflowRestUtils.deleteWorkflow(bearerToken, workflow.getUuid(), workflow.getAppId());
    }
  }
}
