package io.harness.e2e.dailysanity.cdworkflow.governance;

import static io.harness.rule.OwnerRule.SHUBHANSHU;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.GcpKubernetesInfrastructureMapping.Builder.aGcpKubernetesInfrastructureMapping;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;

import io.harness.beans.WorkflowType;
import io.harness.category.element.E2ETests;
import io.harness.e2e.AbstractE2ETest;
import io.harness.rule.Owner;
import io.harness.testframework.restutils.ApplicationRestUtils;
import io.harness.testframework.restutils.ArtifactStreamRestUtils;
import io.harness.testframework.restutils.CloudProviderUtils;
import io.harness.testframework.restutils.ConnectorUtils;
import io.harness.testframework.restutils.EnvironmentRestUtils;
import io.harness.testframework.restutils.ExecutionRestUtils;
import io.harness.testframework.restutils.GovernanceRestUtils;
import io.harness.testframework.restutils.ServiceRestUtils;
import io.harness.testframework.restutils.WorkflowRestUtils;
import io.restassured.path.json.JsonPath;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import software.wings.api.DeploymentType;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.beans.governance.GovernanceConfig;
import software.wings.utils.ArtifactType;

import java.util.ArrayList;
import java.util.List;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GovernanceTest extends AbstractE2ETest {
  private static long appendString = System.currentTimeMillis();
  private static String APPLICATION_NAME = "GovernenceTestApp" + appendString;
  private static String SERVICE_NAME = "K8sV1DockerServiceCanaryDeploy";
  private static String ENV_NAME = "QA";
  private static String CLOUD_PROVIDER_NAME = "harness-exploration-automation" + appendString;
  private static String DOCKER_REGISTRY_NAME = "HarnessRegistry-Automation-" + appendString;
  private static String ARTIFACT_PATTERN = "library/nginx";
  private static String CLUSTER_NAME = "us-central1-a/harness-test";
  private static String NAMESPACE_NAME = "default";
  private static String PROVIDER_TYPE = "GCP";
  private static String WORKFLOW_NAME = "CanaryDockerWorkflow";

  // Entities
  private static GovernanceConfig initialGovernanceStatus;
  private static GovernanceConfig finalGovernanceStatus;
  private static Boolean originalDeploymentFreeze;
  private static Boolean deploymentFreeze;
  private static Application application;
  private static String serviceId;
  private static Environment environment;
  private static Workflow workflow;
  private static String gcpInfraId;
  private static String dockerRegistryId;
  private static String cloudProviderId;
  private static ExecutionArgs executionArgs;

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(E2ETests.class)
  public void TC1_governanceStatusCheck() {
    // Checking initial Governance status
    initialGovernanceStatus = GovernanceRestUtils.checkGovernanceStatus(bearerToken, getAccount().getUuid());
    assertThat(initialGovernanceStatus).isNotNull();

    // Saving value of deploymentFreeze flag
    deploymentFreeze = initialGovernanceStatus.isDeploymentFreeze();
    originalDeploymentFreeze = initialGovernanceStatus.isDeploymentFreeze();
    if (deploymentFreeze) {
      GovernanceRestUtils.setDeploymentFreeze(bearerToken, getAccount().getUuid(), false);
      initialGovernanceStatus = GovernanceRestUtils.checkGovernanceStatus(bearerToken, getAccount().getUuid());
      assertThat(initialGovernanceStatus).isNotNull();
      deploymentFreeze = initialGovernanceStatus.isDeploymentFreeze();
    }
    // Ensuring that deployments freeze is false, i.e, deployments can be made
    assertThat(deploymentFreeze).isFalse();
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(E2ETests.class)
  public void TC2_createTestApplication() {
    // Test data setup
    cloudProviderId =
        CloudProviderUtils.createGCPCloudProvider(bearerToken, CLOUD_PROVIDER_NAME, getAccount().getUuid());
    dockerRegistryId =
        ConnectorUtils.createDockerRegistryConnector(bearerToken, DOCKER_REGISTRY_NAME, getAccount().getUuid());

    Application k8sV1App = anApplication().name(APPLICATION_NAME).build();
    application = ApplicationRestUtils.createApplication(bearerToken, getAccount(), k8sV1App);
    assertThat(application).isNotNull();
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(E2ETests.class)
  public void TC3_createTestServiceMapArtifactStream() {
    Service testService = Service.builder().name(SERVICE_NAME).artifactType(ArtifactType.DOCKER).build();
    serviceId =
        ServiceRestUtils.createService(bearerToken, getAccount().getUuid(), application.getAppId(), testService);

    DockerArtifactStream dockerArtifactStream = DockerArtifactStream.builder()
                                                    .appId(application.getUuid())
                                                    .serviceId(serviceId)
                                                    .settingId(dockerRegistryId)
                                                    .imageName(ARTIFACT_PATTERN)
                                                    .autoPopulate(true)
                                                    .build();
    JsonPath response = ArtifactStreamRestUtils.configureDockerArtifactStream(
        bearerToken, getAccount().getUuid(), application.getAppId(), dockerArtifactStream);
    assertThat(response).isNotNull();
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(E2ETests.class)
  public void TC4_createTestEnvironmentAndInfraMapping() {
    Environment testEnv = anEnvironment().name(ENV_NAME).environmentType(EnvironmentType.PROD).build();
    environment = EnvironmentRestUtils.createEnvironment(bearerToken, getAccount(), application.getAppId(), testEnv);
    assertThat(environment).isNotNull();

    String testServiceTemplateId = EnvironmentRestUtils.getServiceTemplateId(
        bearerToken, getAccount(), application.getUuid(), environment.getUuid());

    GcpKubernetesInfrastructureMapping gcpInfraMapping =
        aGcpKubernetesInfrastructureMapping()
            .withNamespace(NAMESPACE_NAME)
            .withClusterName(CLUSTER_NAME)
            .withServiceId(serviceId)
            .withServiceTemplateId(testServiceTemplateId)
            .withDeploymentType(DeploymentType.KUBERNETES.name())
            .withComputeProviderSettingId(cloudProviderId)
            .withComputeProviderType(PROVIDER_TYPE)
            .withInfraMappingType(InfrastructureMappingType.GCP_KUBERNETES.getName())
            .withComputeProviderName("Google Cloud Platform: " + CLOUD_PROVIDER_NAME)
            .withAutoPopulate(true)
            .build();

    JsonPath infraMappingResponse = EnvironmentRestUtils.configureInfraMapping(
        bearerToken, getAccount().getUuid(), application.getUuid(), environment.getUuid(), gcpInfraMapping);
    assertThat(infraMappingResponse).isNotNull();
    gcpInfraId = infraMappingResponse.getString("resource.uuid");
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(E2ETests.class)
  public void TC5_createTestCanaryWorkflow() {
    workflow = aWorkflow()
                   .name(WORKFLOW_NAME)
                   .serviceId(serviceId)
                   .envId(environment.getUuid())
                   .workflowType(WorkflowType.ORCHESTRATION)
                   .infraMappingId(gcpInfraId)
                   .orchestrationWorkflow(aBasicOrchestrationWorkflow()
                                              .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
                                              .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
                                              .build())
                   .build();

    // Checking workflow is created successfully
    workflow = WorkflowRestUtils.createWorkflow(bearerToken, getAccount().getUuid(), application.getUuid(), workflow);
    assertThat(workflow).isNotNull();
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(E2ETests.class)
  public void TC6_workflowDeployment() {
    String artifactId = ArtifactStreamRestUtils.getArtifactStreamId(
        bearerToken, application.getAppId(), environment.getUuid(), serviceId);
    executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(workflow.getWorkflowType());
    List<Artifact> artifacts = new ArrayList<>();
    Artifact artifact = new Artifact();
    artifact.setUuid(artifactId);
    artifacts.add(artifact);
    executionArgs.setArtifacts(artifacts);
    executionArgs.setOrchestrationId(workflow.getUuid());
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(E2ETests.class)
  public void TC7_checkWorkflowDeployed() {
    WorkflowExecution deploymentsAllowed =
        ExecutionRestUtils.runWorkflow(bearerToken, application.getAppId(), environment.getUuid(), executionArgs);
    assertThat(deploymentsAllowed).isNotNull();
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(E2ETests.class)
  public void TC8_shouldBlockDeploymentsWhenFreezeIsEnabled() {
    // Setting deployment freeze : true
    GovernanceRestUtils.setDeploymentFreeze(bearerToken, getAccount().getUuid(), true);
    // Checking that deployments are blocked now
    WorkflowExecution deploymentsNotAllowed =
        ExecutionRestUtils.runWorkflow(bearerToken, application.getAppId(), environment.getUuid(), executionArgs);
    assertThat(deploymentsNotAllowed).isNull();

    // Setting deployment freeze value to the original value
    GovernanceRestUtils.setDeploymentFreeze(bearerToken, getAccount().getUuid(), originalDeploymentFreeze);
    finalGovernanceStatus = GovernanceRestUtils.checkGovernanceStatus(bearerToken, getAccount().getUuid());
    assertThat(finalGovernanceStatus).isNotNull();
    // checking that finally original settings are restored
    deploymentFreeze = finalGovernanceStatus.isDeploymentFreeze();
    assertThat(deploymentFreeze == originalDeploymentFreeze).isTrue();
  }
}