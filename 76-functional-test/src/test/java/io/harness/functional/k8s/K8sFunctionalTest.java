package io.harness.functional.k8s;

import static io.harness.beans.WorkflowType.ORCHESTRATION;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;

import com.google.inject.Inject;

import io.harness.RestUtils.ArtifactStreamRestUtil;
import io.harness.RestUtils.WorkflowRestUtil;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.beans.WorkflowType;
import io.harness.category.element.FunctionalTests;
import io.harness.framework.Setup;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.windows.TestConstants;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.EnvironmentGenerator.Environments;
import io.harness.generator.InfrastructureMappingGenerator;
import io.harness.generator.InfrastructureMappingGenerator.InfrastructureMappings;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.generator.ServiceGenerator;
import io.harness.generator.ServiceGenerator.Services;
import io.harness.generator.SettingGenerator;
import io.harness.rule.OwnerRule.Owner;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.service.impl.WorkflowExecutionServiceImpl;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class K8sFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private WorkflowExecutionServiceImpl workflowExecutionService;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private InfrastructureMappingGenerator infrastructureMappingGenerator;
  @Inject private SettingGenerator settingGenerator;
  @Inject private WorkflowRestUtil workflowRestUtil;
  @Inject private TestConstants testConstants;
  @Inject private ArtifactStreamRestUtil artifactStreamRestUtil;

  final Randomizer.Seed seed = new Randomizer.Seed(0);
  OwnerManager.Owners owners;
  Application application;

  @Before
  public void setUp() {
    owners = ownerManager.create();
    application = applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST);
    assertThat(application).isNotNull();
  }

  @Test
  @Owner(emails = "puneet.saraswat@harness.io", intermittent = true)
  @Category(FunctionalTests.class)
  public void testK8sRollingWorkflow() {
    testK8sWorkflow(OrchestrationWorkflowType.ROLLING);
  }

  @Test
  @Owner(emails = "puneet.saraswat@harness.io", intermittent = true)
  @Category(FunctionalTests.class)
  public void testK8sCanaryWorkflow() {
    testK8sWorkflow(OrchestrationWorkflowType.CANARY);
  }

  @Test
  @Owner(emails = "puneet.saraswat@harness.io", intermittent = true)
  @Category(FunctionalTests.class)
  public void testK8sBlueGreenWorkflow() {
    testK8sWorkflow(OrchestrationWorkflowType.BLUE_GREEN);
  }

  private void testK8sWorkflow(OrchestrationWorkflowType workflowType) {
    Service savedService = serviceGenerator.ensurePredefined(seed, owners, Services.K8S_V2_TEST);
    Environment savedEnvironment = environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST);

    InfrastructureMappings infrastructureMappingTestType = null;
    String workflowName = "k8s";
    switch (workflowType) {
      case ROLLING:
        infrastructureMappingTestType = InfrastructureMappings.K8S_ROLLING_TEST;
        workflowName = "k8s-rolling";
        break;

      case CANARY:
        infrastructureMappingTestType = InfrastructureMappings.K8S_CANARY_TEST;
        workflowName = "k8s-canary";
        break;

      case BLUE_GREEN:
        infrastructureMappingTestType = InfrastructureMappings.K8S_BLUE_GREEN_TEST;
        workflowName = "k8s-bg";
        break;

      default:
        assert false;
    }

    InfrastructureMapping infrastructureMapping =
        infrastructureMappingGenerator.ensurePredefined(seed, owners, infrastructureMappingTestType);

    resetCache();

    Workflow savedWorkflow = createK8sV2Workflow(application.getUuid(), savedEnvironment.getUuid(),
        savedService.getUuid(), infrastructureMapping.getUuid(), workflowName, workflowType);

    assertThat(savedWorkflow).isNotNull();
    assertThat(savedWorkflow.getUuid()).isNotEmpty();
    assertThat(savedWorkflow.getWorkflowType()).isEqualTo(ORCHESTRATION);

    // Deploy the workflow
    WorkflowExecution workflowExecution =
        workflowRestUtil.runWorkflow(application.getUuid(), savedEnvironment.getUuid(),
            getExecutionArgs(savedWorkflow, savedEnvironment.getUuid(), savedService.getUuid()));
    assertThat(workflowExecution).isNotNull();

    Awaitility.await()
        .atMost(120, TimeUnit.SECONDS)
        .pollInterval(5, TimeUnit.SECONDS)
        .until(()
                   -> workflowExecutionService.getWorkflowExecution(application.getUuid(), workflowExecution.getUuid())
                          .getStatus()
                          .equals(ExecutionStatus.SUCCESS));

    // Clean up workflow
    cleanUpWorkflow(application.getUuid(), savedWorkflow.getUuid());
  }

  private ExecutionArgs getExecutionArgs(Workflow workflow, String envId, String serviceId) {
    String artifactId = artifactStreamRestUtil.getArtifactStreamId(application.getUuid(), envId, serviceId);
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

  private void cleanUpWorkflow(String appId, String workflowId) {
    assertThat(appId).isNotNull();
    assertThat(workflowId).isNotNull();
    // Clean up resources
    Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("appId", appId)
        .pathParam("workflowId", workflowId)
        .delete("/workflows/{workflowId}")
        .then()
        .statusCode(200);
  }

  private Workflow createK8sV2Workflow(String appId, String envId, String serviceId, String infraMappingId, String name,
      OrchestrationWorkflowType orchestrationWorkflowType) {
    Workflow workflow = aWorkflow()
                            .withName(name)
                            .withAppId(appId)
                            .withEnvId(envId)
                            .withServiceId(serviceId)
                            .withInfraMappingId(infraMappingId)
                            .withWorkflowType(WorkflowType.ORCHESTRATION)
                            .withOrchestrationWorkflow(aCanaryOrchestrationWorkflow().build())
                            .build();
    workflow.getOrchestrationWorkflow().setOrchestrationWorkflowType(orchestrationWorkflowType);
    return workflowRestUtil.createWorkflow(application.getAccountId(), appId, workflow);
  }
}
