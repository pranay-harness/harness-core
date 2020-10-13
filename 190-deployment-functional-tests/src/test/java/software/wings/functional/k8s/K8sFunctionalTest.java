package software.wings.functional.k8s;

import static io.harness.rule.OwnerRule.YOGESH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.category.element.CDFunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.utils.K8SUtils;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.EnvironmentGenerator.Environments;
import io.harness.generator.InfrastructureDefinitionGenerator;
import io.harness.generator.InfrastructureDefinitionGenerator.InfrastructureDefinitions;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.generator.ServiceGenerator;
import io.harness.generator.ServiceGenerator.Services;
import io.harness.rule.Owner;
import io.harness.testframework.restutils.ArtifactStreamRestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.infra.InfrastructureDefinition;

import java.util.Collections;

public class K8sFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private InfrastructureDefinitionGenerator infrastructureDefinitionGenerator;

  private static final long TIMEOUT = 1200000; // 20 minutes

  final Randomizer.Seed seed = new Randomizer.Seed(0);
  OwnerManager.Owners owners;
  Application application;

  @Before
  public void setUp() {
    owners = ownerManager.create();
    application = applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST);
    assertThat(application).isNotNull();
  }

  @Test(timeout = TIMEOUT)
  @Owner(developers = YOGESH)
  @Category(CDFunctionalTests.class)
  public void testK8sRollingWorkflow() {
    testK8sWorkflow(OrchestrationWorkflowType.ROLLING);
  }

  @Test(timeout = TIMEOUT)
  @Owner(developers = YOGESH)
  @Category({CDFunctionalTests.class})
  public void testK8sCanaryWorkflow() {
    testK8sWorkflow(OrchestrationWorkflowType.CANARY);
  }

  @Test(timeout = TIMEOUT)
  @Owner(developers = YOGESH)
  @Category(CDFunctionalTests.class)
  public void testK8sBlueGreenWorkflow() {
    testK8sWorkflow(OrchestrationWorkflowType.BLUE_GREEN);
  }

  private void testK8sWorkflow(OrchestrationWorkflowType workflowType) {
    Service savedService = serviceGenerator.ensurePredefined(seed, owners, Services.K8S_V2_TEST);
    Environment savedEnvironment = environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST);

    InfrastructureDefinitions infrastructureDefinitionTestType = null;
    String workflowName = "k8s";
    switch (workflowType) {
      case ROLLING:
        infrastructureDefinitionTestType = InfrastructureDefinitions.K8S_ROLLING_TEST;
        workflowName = "k8s-rolling";
        break;

      case CANARY:
        infrastructureDefinitionTestType = InfrastructureDefinitions.K8S_CANARY_TEST;
        workflowName = "k8s-canary";
        break;

      case BLUE_GREEN:
        infrastructureDefinitionTestType = InfrastructureDefinitions.K8S_BLUE_GREEN_TEST;
        workflowName = "k8s-bg";
        break;

      default:
        assert false;
    }

    workflowName = workflowName + '-' + System.currentTimeMillis();

    InfrastructureDefinition infrastructureDefinition =
        infrastructureDefinitionGenerator.ensurePredefined(seed, owners, infrastructureDefinitionTestType);

    // create workflow
    Workflow savedWorkflow =
        K8SUtils.createWorkflow(application.getUuid(), savedEnvironment.getUuid(), savedService.getUuid(),
            infrastructureDefinition.getUuid(), workflowName, workflowType, bearerToken, application.getAccountId());

    // Deploy the workflow
    WorkflowExecution workflowExecution = runWorkflow(bearerToken, application.getUuid(), savedEnvironment.getUuid(),
        getExecutionArgs(savedWorkflow, savedEnvironment.getUuid(), savedService.getUuid()));

    assertInstanceCount(workflowExecution.getStatus(), application.getUuid(), savedService.getUuid(),
        workflowExecution.getInfraMappingIds().get(0));

    // create clean up workflow
    Workflow cleanupWorkflow =
        K8SUtils.createK8sCleanupWorkflow(application.getUuid(), savedEnvironment.getUuid(), savedService.getUuid(),
            infrastructureDefinition.getUuid(), workflowName, bearerToken, application.getAccountId());

    runWorkflow(bearerToken, application.getUuid(), savedEnvironment.getUuid(),
        getExecutionArgs(cleanupWorkflow, savedEnvironment.getUuid(), savedService.getUuid()));

    logStateExecutionInstanceErrors(workflowExecution);
    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  private ExecutionArgs getExecutionArgs(Workflow workflow, String envId, String serviceId) {
    String artifactId =
        ArtifactStreamRestUtils.getArtifactStreamId(bearerToken, application.getUuid(), envId, serviceId);
    Artifact artifact = new Artifact();
    artifact.setUuid(artifactId);

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(workflow.getWorkflowType());
    executionArgs.setOrchestrationId(workflow.getUuid());
    executionArgs.setServiceId(serviceId);
    executionArgs.setCommandName("START");
    executionArgs.setArtifacts(Collections.singletonList(artifact));

    return executionArgs;
  }
}
