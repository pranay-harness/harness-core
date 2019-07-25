package io.harness.e2e.dailysanity.cdworkflow.deployments;

import static io.harness.rule.OwnerRule.UJJAWAL;
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
import io.harness.rule.OwnerRule.Owner;
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
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.utils.ArtifactType;

import java.util.ArrayList;
import java.util.List;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class K8sV1Deployment extends AbstractE2ETest {
  private static long appendString = System.currentTimeMillis();
  private static String APP_NAME = "K8sV1_Automation-" + appendString;
  private static String CLOUD_PROVIDER_NAME = "harness-exploration-automation" + appendString;
  private static String DOCKERREGISTRY_NAME = "HarnessRegistry-Automation-" + appendString;
  private static boolean IS_K8s_V2_BOOLEAN = true;
  private static String SERVICE_NAME = "K8sV1DockerServiceCanaryDeploy";
  private static String SERVICE_NAME_V2 = "K8sV2DockerServiceCanaryDeploy";
  private static String ARTIFACT_PATTERN = "library/nginx";
  private static String CLUSTER_NAME = "us-central1-a/harness-test";
  private static String NAMESPACE_NAME = "default";
  private static String ENV_MAME = "QA";
  private static String ENV_MAME_V2 = "QA_V2";

  private static String PROVIDER_TYPE = "GCP";
  private static String WF_NAME = "CanaryDockerWorkflow";
  private static String WF_NAME_V2 = "CanaryDockerWorkflow_V2";

  // Entities
  private static Application application;
  private static Environment environment_v1;
  private static Environment environment_v2;

  private static Workflow workflow_v1;
  private static Workflow workflow_v2;

  private static WorkflowExecution workflowExecution;
  private static String gcpInfraId;
  private static String dockerRegistryId;
  private static String cloudProviderId;
  private static String serviceId_V1;
  private static String serviceId_V2;
  private static ExecutionArgs executionArgs;

  @Test
  @Owner(emails = UJJAWAL)
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
  }

  @Test
  @Owner(emails = UJJAWAL)
  @Category(E2ETests.class)
  public void TC1_createK8sV1ServiceMapArtifactStream() {
    Service service = Service.builder().name(SERVICE_NAME).artifactType(ArtifactType.DOCKER).build();
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
  @Owner(emails = UJJAWAL)
  @Category(E2ETests.class)
  public void TC2_createK8sV2ServiceMapArtifactStream() {
    Service service =
        Service.builder().name(SERVICE_NAME_V2).artifactType(ArtifactType.DOCKER).isK8sV2(IS_K8s_V2_BOOLEAN).build();
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
  @Owner(emails = UJJAWAL)
  @Category(E2ETests.class)
  public void TC3_createK8sV1EnvironmentAndInfraMapping() {
    Environment myEnv = anEnvironment().name(ENV_MAME).environmentType(EnvironmentType.PROD).build();
    environment_v1 = EnvironmentRestUtils.createEnvironment(bearerToken, getAccount(), application.getAppId(), myEnv);
    assertThat(environment_v1).isNotNull();

    String serviceTemplateId = EnvironmentRestUtils.getServiceTemplateId(
        bearerToken, getAccount(), application.getUuid(), environment_v1.getUuid());

    GcpKubernetesInfrastructureMapping gcpInfraMapping =
        aGcpKubernetesInfrastructureMapping()
            .withClusterName(CLUSTER_NAME)
            .withNamespace(NAMESPACE_NAME)
            .withServiceId(serviceId_V1)
            .withDeploymentType(DeploymentType.KUBERNETES.name())
            .withComputeProviderSettingId(cloudProviderId)
            .withServiceTemplateId(serviceTemplateId)
            .withComputeProviderType(PROVIDER_TYPE)
            .withComputeProviderName("Google Cloud Platform: " + CLOUD_PROVIDER_NAME)
            .withInfraMappingType(InfrastructureMappingType.GCP_KUBERNETES.getName())
            .withAutoPopulate(true)
            .build();

    JsonPath response = EnvironmentRestUtils.configureInfraMapping(
        bearerToken, getAccount().getUuid(), application.getUuid(), environment_v1.getUuid(), gcpInfraMapping);
    assertThat(response).isNotNull();
    gcpInfraId = response.getString("resource.uuid");
  }

  @Test
  @Owner(emails = UJJAWAL)
  @Category(E2ETests.class)
  public void TC4_createK8sV2EnvironmentAndInfraMapping() {
    Environment myEnv = anEnvironment().name(ENV_MAME_V2).environmentType(EnvironmentType.PROD).build();
    environment_v2 = EnvironmentRestUtils.createEnvironment(bearerToken, getAccount(), application.getAppId(), myEnv);
    assertThat(environment_v2).isNotNull();

    String serviceTemplateId = EnvironmentRestUtils.getServiceTemplateId(
        bearerToken, getAccount(), application.getUuid(), environment_v2.getUuid());

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
        bearerToken, getAccount().getUuid(), application.getUuid(), environment_v2.getUuid(), gcpInfraMapping);
    assertThat(response).isNotNull();
    gcpInfraId = response.getString("resource.uuid");
  }

  @Test
  @Owner(emails = UJJAWAL)
  @Category(E2ETests.class)
  public void TC5_CreateK8sV1CanaryWorkflow() throws Exception {
    workflow_v1 = aWorkflow()
                      .name(WF_NAME)
                      .envId(environment_v1.getUuid())
                      .serviceId(serviceId_V1)
                      .infraMappingId(gcpInfraId)
                      .workflowType(WorkflowType.ORCHESTRATION)
                      .orchestrationWorkflow(aBasicOrchestrationWorkflow()
                                                 .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
                                                 .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
                                                 .build())
                      .build();

    workflow_v1 =
        WorkflowRestUtils.createWorkflow(bearerToken, getAccount().getUuid(), application.getUuid(), workflow_v1);
    assertThat(workflow_v1).isNotNull();
  }

  @Test
  @Owner(emails = UJJAWAL)
  @Category(E2ETests.class)
  public void TC6_CreateK8sV2CanaryWorkflow() throws Exception {
    workflow_v2 = aWorkflow()
                      .name(WF_NAME_V2)
                      .envId(environment_v2.getUuid())
                      .serviceId(serviceId_V2)
                      .infraMappingId(gcpInfraId)
                      .workflowType(WorkflowType.ORCHESTRATION)
                      .orchestrationWorkflow(aBasicOrchestrationWorkflow()
                                                 .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
                                                 .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
                                                 .build())
                      .build();

    workflow_v2 =
        WorkflowRestUtils.createWorkflow(bearerToken, getAccount().getUuid(), application.getUuid(), workflow_v2);
    assertThat(workflow_v2).isNotNull();
  }

  @Test
  @Owner(emails = UJJAWAL)
  @Category(E2ETests.class)
  public void TC7_DeployK8sV1Workflow() {
    String artifactId = ArtifactStreamRestUtils.getArtifactStreamId(
        bearerToken, application.getAppId(), environment_v1.getUuid(), serviceId_V1);
    executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(workflow_v1.getWorkflowType());
    List<Artifact> artifacts = new ArrayList<>();
    Artifact artifact = new Artifact();
    artifact.setUuid(artifactId);
    artifacts.add(artifact);
    executionArgs.setArtifacts(artifacts);
    executionArgs.setOrchestrationId(workflow_v1.getUuid());
  }

  @Test
  @Owner(emails = UJJAWAL)
  @Category(E2ETests.class)
  public void TC8_DeployK8sV2Workflow() {
    String artifactId = ArtifactStreamRestUtils.getArtifactStreamId(
        bearerToken, application.getAppId(), environment_v2.getUuid(), serviceId_V2);
    executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(workflow_v2.getWorkflowType());
    List<Artifact> artifacts = new ArrayList<>();
    Artifact artifact = new Artifact();
    artifact.setUuid(artifactId);
    artifacts.add(artifact);
    executionArgs.setArtifacts(artifacts);
    executionArgs.setOrchestrationId(workflow_v2.getUuid());
  }

  @Test
  @Owner(emails = UJJAWAL)
  @Category(E2ETests.class)
  public void TC9_checkExecutionStarted() {
    ExecutionRestUtils.executeAndCheck(
        bearerToken, getAccount(), application.getAppId(), environment_v2.getUuid(), executionArgs);

    ExecutionRestUtils.executeAndCheck(
        bearerToken, getAccount(), application.getAppId(), environment_v2.getUuid(), executionArgs);
  }
}