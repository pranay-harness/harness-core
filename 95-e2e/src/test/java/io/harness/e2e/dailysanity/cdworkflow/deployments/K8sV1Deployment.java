package io.harness.e2e.dailysanity.cdworkflow.deployments;

import static io.harness.rule.OwnerRule.SUNIL;
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
  private static String SERVICE_NAME = "K8sV1DockerServiceCanaryDeploy";
  private static String ARTIFACT_PATTERN = "library/nginx";
  private static String CLUSTER_NAME = "us-west1-a/harness-test";
  private static String NAMESPACE_NAME = "default";
  private static String ENV_MAME = "QA";
  private static String PROVIDER_TYPE = "GCP";
  private static String WF_NAME = "CanaryDockerWorkflow";

  // Entities
  private static Application application;
  private static Environment environment;
  private static Workflow workflow;
  private static WorkflowExecution workflowExecution;
  private static String gcpInfraId;
  private static String dockerRegistryId;
  private static String cloudProviderId;
  private static String serviceId;
  private static ExecutionArgs executionArgs;

  @Test
  @Owner(emails = SUNIL)
  @Category(E2ETests.class)
  public void TC1_createApplication() {
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
  @Owner(emails = SUNIL)
  @Category(E2ETests.class)
  public void TC2_createServiceMapArtifactStream() {
    Service service = Service.builder().name(SERVICE_NAME).artifactType(ArtifactType.DOCKER).build();
    serviceId = ServiceRestUtils.createService(bearerToken, getAccount().getUuid(), application.getAppId(), service);

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
  @Owner(emails = SUNIL)
  @Category(E2ETests.class)
  public void TC3_createEnvironmentAndInfraMapping() {
    Environment myEnv = anEnvironment().name(ENV_MAME).environmentType(EnvironmentType.PROD).build();
    environment = EnvironmentRestUtils.createEnvironment(bearerToken, getAccount(), application.getAppId(), myEnv);
    assertThat(environment).isNotNull();

    String serviceTemplateId = EnvironmentRestUtils.getServiceTemplateId(
        bearerToken, getAccount(), application.getUuid(), environment.getUuid());

    GcpKubernetesInfrastructureMapping gcpInfraMapping =
        aGcpKubernetesInfrastructureMapping()
            .withClusterName(CLUSTER_NAME)
            .withNamespace(NAMESPACE_NAME)
            .withServiceId(serviceId)
            .withDeploymentType(DeploymentType.KUBERNETES.name())
            .withComputeProviderSettingId(cloudProviderId)
            .withServiceTemplateId(serviceTemplateId)
            .withComputeProviderType(PROVIDER_TYPE)
            .withComputeProviderName("Google Cloud Platform: " + CLOUD_PROVIDER_NAME)
            .withInfraMappingType(InfrastructureMappingType.GCP_KUBERNETES.getName())
            .withAutoPopulate(true)
            .build();

    JsonPath response = EnvironmentRestUtils.configureInfraMapping(
        bearerToken, getAccount().getUuid(), application.getUuid(), environment.getUuid(), gcpInfraMapping);
    assertThat(response).isNotNull();
    gcpInfraId = response.getString("resource.uuid");
  }

  @Test
  @Owner(emails = SUNIL)
  @Category(E2ETests.class)
  public void TC4_CreateCanaryWorkflow() throws Exception {
    workflow = aWorkflow()
                   .name(WF_NAME)
                   .envId(environment.getUuid())
                   .serviceId(serviceId)
                   .infraMappingId(gcpInfraId)
                   .workflowType(WorkflowType.ORCHESTRATION)
                   .orchestrationWorkflow(aBasicOrchestrationWorkflow()
                                              .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
                                              .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
                                              .build())
                   .build();

    workflow = WorkflowRestUtils.createWorkflow(bearerToken, getAccount().getUuid(), application.getUuid(), workflow);
    assertThat(workflow).isNotNull();
  }

  @Test
  @Owner(emails = SUNIL)
  @Category(E2ETests.class)
  public void TC5_DeployWorkflow() {
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
  @Owner(emails = SUNIL)
  @Category(E2ETests.class)
  public void TC6_checkExecutionStarted() {
    ExecutionRestUtils.executeAndCheck(
        bearerToken, getAccount(), application.getAppId(), environment.getUuid(), executionArgs);
  }
}