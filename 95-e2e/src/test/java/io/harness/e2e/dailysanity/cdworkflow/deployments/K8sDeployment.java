package io.harness.e2e.dailysanity.cdworkflow.deployments;

import static io.harness.rule.OwnerRule.UJJAWAL;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
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
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.utils.ArtifactType;

import java.util.ArrayList;
import java.util.List;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class K8sDeployment extends AbstractE2ETest {
  private static long appendString = System.currentTimeMillis();
  private static String APP_NAME = "K8s_Automation-" + appendString;
  private static String CLOUD_PROVIDER_NAME = "harness-exploration-automation" + appendString;
  private static String DOCKERREGISTRY_NAME = "HarnessRegistry-Automation-" + appendString;
  private static boolean IS_K8s_V2_BOOLEAN = true;
  private static String SERVICE_NAME_V1 = "K8sV1DockerServiceCanaryDeploy";
  private static String SERVICE_NAME_V2 = "K8sV2DockerServiceCanaryDeploy";
  private static String ARTIFACT_PATTERN = "library/nginx";
  private static String CLUSTER_NAME = "us-central1-a/harness-test";
  private static String NAMESPACE_NAME = "default";
  private static String ENV_MAME_V1 = "QA_V1";
  private static String ENV_MAME_V2 = "QA_V2";

  private static String PROVIDER_TYPE = "GCP";
  private static String WF_NAME_V1 = "CanaryDockerWorkflow_V1";
  private static String WF_NAME_V2 = "CanaryDockerWorkflow_V2";

  // Entities
  private static Application application;
  private static Environment environment_V1;
  private static Environment environment_V2;

  private static Workflow workflow_V1;
  private static Workflow workflow_V2;

  private static String gcpInfraId_V1;
  private static String gcpInfraId_V2;
  private static String dockerRegistryId;
  private static String cloudProviderId;
  private static String serviceId_V1;
  private static String serviceId_V2;
  private static ExecutionArgs executionArgs_V1;
  private static ExecutionArgs executionArgs_V2;

  @Test
  @Owner(developers = UJJAWAL)
  @Category(E2ETests.class)
  public void TC0_createApplication() {
    // Test data setup
    cloudProviderId =
        CloudProviderUtils.createGCPCloudProvider(bearerToken, CLOUD_PROVIDER_NAME, getAccount().getUuid());
    dockerRegistryId =
        ConnectorUtils.createDockerRegistryConnector(bearerToken, DOCKERREGISTRY_NAME, getAccount().getUuid());

    Application k8sV1App = anApplication().name(APP_NAME).build();
    application = ApplicationRestUtils.createApplication(bearerToken, getAccount(), k8sV1App);
    assertThat(application).isNotNull();

    System.out.println("Application created '" + APP_NAME + "'");
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(E2ETests.class)
  public void TC1_createK8sV1ServiceMapArtifactStream() {
    Service service = Service.builder().name(SERVICE_NAME_V1).artifactType(ArtifactType.DOCKER).build();
    serviceId_V1 = ServiceRestUtils.createService(bearerToken, getAccount().getUuid(), application.getAppId(), service);

    DockerArtifactStream dockerArtifactStream = DockerArtifactStream.builder()
                                                    .appId(application.getUuid())
                                                    .serviceId(serviceId_V1)
                                                    .settingId(dockerRegistryId)
                                                    .imageName(ARTIFACT_PATTERN)
                                                    .autoPopulate(true)
                                                    .build();
    JsonPath response = ArtifactStreamRestUtils.configureDockerArtifactStream(
        bearerToken, getAccount().getUuid(), application.getAppId(), dockerArtifactStream);
    assertThat(response).isNotNull();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(E2ETests.class)
  public void TC2_createK8sV1EnvironmentAndInfraMapping() {
    Environment myEnv = anEnvironment().name(ENV_MAME_V1).environmentType(EnvironmentType.PROD).build();
    environment_V1 = EnvironmentRestUtils.createEnvironment(bearerToken, getAccount(), application.getAppId(), myEnv);
    assertThat(environment_V1).isNotNull();

    String serviceTemplateId1 = EnvironmentRestUtils.getServiceTemplateId(
        bearerToken, getAccount(), application.getUuid(), environment_V1.getUuid());

    GcpKubernetesInfrastructureMapping gcpInfraMapping1 =
        aGcpKubernetesInfrastructureMapping()
            .withClusterName(CLUSTER_NAME)
            .withNamespace(NAMESPACE_NAME)
            .withServiceId(serviceId_V1)
            .withDeploymentType(DeploymentType.KUBERNETES.name())
            .withComputeProviderSettingId(cloudProviderId)
            .withServiceTemplateId(serviceTemplateId1)
            .withComputeProviderType(PROVIDER_TYPE)
            .withComputeProviderName("Google Cloud Platform: " + CLOUD_PROVIDER_NAME)
            .withInfraMappingType(InfrastructureMappingType.GCP_KUBERNETES.getName())
            .withAutoPopulate(true)
            .build();

    JsonPath response = EnvironmentRestUtils.configureInfraMapping(
        bearerToken, getAccount().getUuid(), application.getUuid(), environment_V1.getUuid(), gcpInfraMapping1);
    assertThat(response).isNotNull();
    gcpInfraId_V1 = response.getString("resource.uuid");
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(E2ETests.class)
  public void TC3_CreateK8sV1CanaryWorkflow() throws Exception {
    workflow_V1 = aWorkflow()
                      .name(WF_NAME_V1)
                      .envId(environment_V1.getUuid())
                      .serviceId(serviceId_V1)
                      .infraMappingId(gcpInfraId_V1)
                      .workflowType(WorkflowType.ORCHESTRATION)
                      .orchestrationWorkflow(aCanaryOrchestrationWorkflow()
                                                 .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
                                                 .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
                                                 .build())
                      .build();

    workflow_V1 =
        WorkflowRestUtils.createWorkflow(bearerToken, getAccount().getUuid(), application.getUuid(), workflow_V1);
    assertThat(workflow_V1).isNotNull();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(E2ETests.class)
  public void TC4_DeployK8sV1Workflow() {
    String artifactId = ArtifactStreamRestUtils.getArtifactStreamId(
        bearerToken, application.getAppId(), environment_V1.getUuid(), serviceId_V1);
    executionArgs_V1 = new ExecutionArgs();
    executionArgs_V1.setWorkflowType(workflow_V1.getWorkflowType());
    List<Artifact> artifacts = new ArrayList<>();
    Artifact artifact = new Artifact();
    artifact.setUuid(artifactId);
    artifacts.add(artifact);
    executionArgs_V1.setArtifacts(artifacts);
    executionArgs_V1.setOrchestrationId(workflow_V1.getUuid());
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(E2ETests.class)
  public void TC5_checkV1ExecutionStarted() {
    ExecutionRestUtils.executeAndCheck(
        bearerToken, getAccount(), application.getAppId(), environment_V1.getUuid(), executionArgs_V1);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(E2ETests.class)
  public void TC6_createK8sV2ServiceMapArtifactStream() {
    Service service = Service.builder()
                          .name(SERVICE_NAME_V2)
                          .artifactType(ArtifactType.DOCKER)
                          .deploymentType(DeploymentType.KUBERNETES)
                          .isK8sV2(IS_K8s_V2_BOOLEAN)
                          .build();
    serviceId_V2 = ServiceRestUtils.createService(bearerToken, getAccount().getUuid(), application.getAppId(), service);

    DockerArtifactStream dockerArtifactStream = DockerArtifactStream.builder()
                                                    .appId(application.getUuid())
                                                    .serviceId(serviceId_V2)
                                                    .settingId(dockerRegistryId)
                                                    .imageName(ARTIFACT_PATTERN)
                                                    .autoPopulate(true)
                                                    .build();
    JsonPath response = ArtifactStreamRestUtils.configureDockerArtifactStream(
        bearerToken, getAccount().getUuid(), application.getAppId(), dockerArtifactStream);
    assertThat(response).isNotNull();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(E2ETests.class)
  public void TC7_createK8sV2EnvironmentAndInfraMapping() {
    Environment myEnv = anEnvironment().name(ENV_MAME_V2).environmentType(EnvironmentType.PROD).build();
    environment_V2 = EnvironmentRestUtils.createEnvironment(bearerToken, getAccount(), application.getAppId(), myEnv);
    assertThat(environment_V2).isNotNull();

    String serviceTemplateId = EnvironmentRestUtils.getServiceTemplateId(
        bearerToken, getAccount(), application.getUuid(), environment_V2.getUuid());

    GcpKubernetesInfrastructureMapping gcpInfraMapping =
        aGcpKubernetesInfrastructureMapping()
            .withClusterName(CLUSTER_NAME)
            .withNamespace(NAMESPACE_NAME)
            .withServiceId(serviceId_V2)
            .withDeploymentType(DeploymentType.KUBERNETES.name())
            .withComputeProviderSettingId(cloudProviderId)
            .withServiceTemplateId(serviceTemplateId)
            .withComputeProviderType(PROVIDER_TYPE)
            .withComputeProviderName("Google Cloud Platform: " + CLOUD_PROVIDER_NAME)
            .withInfraMappingType(InfrastructureMappingType.GCP_KUBERNETES.getName())
            .withAutoPopulate(true)
            .build();

    JsonPath response = EnvironmentRestUtils.configureInfraMapping(
        bearerToken, getAccount().getUuid(), application.getUuid(), environment_V2.getUuid(), gcpInfraMapping);
    assertThat(response).isNotNull();
    gcpInfraId_V2 = response.getString("resource.uuid");
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(E2ETests.class)
  public void TC8_CreateK8sV2CanaryWorkflow() throws Exception {
    workflow_V2 = aWorkflow()
                      .name(WF_NAME_V2)
                      .envId(environment_V2.getUuid())
                      .serviceId(serviceId_V2)
                      .infraMappingId(gcpInfraId_V2)
                      .workflowType(WorkflowType.ORCHESTRATION)
                      .orchestrationWorkflow(aCanaryOrchestrationWorkflow()
                                                 .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
                                                 .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
                                                 .build())
                      .build();

    workflow_V2 =
        WorkflowRestUtils.createWorkflow(bearerToken, getAccount().getUuid(), application.getUuid(), workflow_V2);
    assertThat(workflow_V2).isNotNull();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(E2ETests.class)
  public void TC91_DeployK8sV2Workflow() {
    String artifactId = ArtifactStreamRestUtils.getArtifactStreamId(
        bearerToken, application.getAppId(), environment_V2.getUuid(), serviceId_V2);
    executionArgs_V2 = new ExecutionArgs();
    executionArgs_V2.setWorkflowType(workflow_V2.getWorkflowType());
    List<Artifact> artifacts = new ArrayList<>();
    Artifact artifact = new Artifact();
    artifact.setUuid(artifactId);
    artifacts.add(artifact);
    executionArgs_V2.setArtifacts(artifacts);
    executionArgs_V2.setOrchestrationId(workflow_V2.getUuid());
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(E2ETests.class)
  public void TC92_checkV2ExecutionStarted() {
    ExecutionRestUtils.executeAndCheck(
        bearerToken, getAccount(), application.getAppId(), environment_V2.getUuid(), executionArgs_V2);
  }
}