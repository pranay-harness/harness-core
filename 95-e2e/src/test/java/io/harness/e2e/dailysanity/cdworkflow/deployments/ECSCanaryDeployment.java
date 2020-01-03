package io.harness.e2e.dailysanity.cdworkflow.deployments;

import static io.harness.rule.OwnerRule.VENKATESH;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.WRAP_UP;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;

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
import lombok.extern.slf4j.Slf4j;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import software.wings.api.DeploymentType;
import software.wings.beans.Application;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.ExecutionCredential.ExecutionType;
import software.wings.beans.PhaseStep;
import software.wings.beans.SSHExecutionCredential;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.EcrArtifactStream;
import software.wings.utils.ArtifactType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ECSCanaryDeployment extends AbstractE2ETest {
  // TEST constants
  private static SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH-mm-ss");
  private static Date date = new Date();
  private static String appendString = formatter.format(date);
  private static String APP_NAME = "ECSServiceCanaryDeploy-" + appendString;
  private static String CLOUD_PROVIDER_NAME = "AWS-Automation-CloudProvider-" + appendString;
  private static String ARTIFACTORY_NAME = "Artifactory-Automation-" + appendString;
  private static String SSHKEY_NAME = "Aws_playground_ec2_user_key_Automation-" + appendString;
  private String SERVICE_NAME = "ECSServiceCanaryDeploy";
  private String IMAGE_NAME = "hello-world";
  private String ARTIFACT_TYPE = "ECR";
  private String ENV_MAME = "QA";
  private String DEPLOYMENT_TYPE = "ECS";
  private String PROVIDER_TYPE = "AWS";
  private String INFRA_TYPE = "AWS_ECS";
  private String INFRA_REGION = "us-east-1";
  private String WF_NAME = "BasicWarWorkflow";
  private String CLUSTER_NAME = "Q-FUNCTIONAL-TESTS-DO-NOT-DELETE";

  // Entities
  private static Application application;
  private static String serviceId;
  private static Environment environment;
  private static Workflow workflow;
  private static String awsInfraId;
  private static WorkflowExecution workflowExecution;
  private static String artifactoryId;
  private static String cloudProviderId;
  private static String sshKeyId;
  private static ExecutionArgs executionArgs;

  final String SETUP_CONTAINER_CONSTANT = "Setup Container";
  final String PRE_DEPLOYMENT_CONSTANT = "Pre-Deployment";
  final String ECS_DAEMON_SERVICE_SETUP_NAME = "ECS Daemon Service Setup";
  final String POST_DEPLOYMENT_CONSTANT = "Post-Deployment";
  final String WRAP_UP_CONSTANT = "Wrap Up";
  final String ECS_SERVICE_SETUP_CONSTANT = "ECS Service Setup";
  final String UPGRADE_CONTAINERS_CONSTANT = "Upgrade Containers";
  final String DEPLOY_CONTAINERS_CONSTANT = "Deploy Containers";

  @Test
  @Owner(developers = VENKATESH)
  @Category(E2ETests.class)
  public void TC1_createApplication() {
    // Test data setup
    cloudProviderId =
        CloudProviderUtils.createAWSCloudProvider(bearerToken, CLOUD_PROVIDER_NAME, getAccount().getUuid());
    artifactoryId = ConnectorUtils.createArtifactoryConnector(bearerToken, ARTIFACTORY_NAME, getAccount().getUuid());

    Application warApp = anApplication().name(APP_NAME).build();
    application = ApplicationRestUtils.createApplication(bearerToken, getAccount(), warApp);
    assertThat(application).isNotNull();
  }

  @Test
  @Owner(developers = VENKATESH)
  @Category(E2ETests.class)
  public void TC2_createECSServiceAndCollectArtifact() {
    Service ecsService = Service.builder()
                             .name(SERVICE_NAME)
                             .deploymentType(DeploymentType.ECS)
                             .artifactType(ArtifactType.DOCKER)
                             .build();
    serviceId = ServiceRestUtils.createService(bearerToken, getAccount().getUuid(), application.getAppId(), ecsService);
    assertThat(serviceId).isNotNull();
    Set<String> stringSet = new HashSet<>();
    stringSet.add("ecr");
    stringSet.add("hello-world");
    EcrArtifactStream ecrArtifactStream = EcrArtifactStream.builder()
                                              .appId(application.getUuid())
                                              .accountId(getAccount().getUuid())
                                              .serviceId(serviceId)
                                              .imageName(IMAGE_NAME)
                                              .region(INFRA_REGION)
                                              .sourceName(IMAGE_NAME)
                                              .keywords(stringSet)
                                              .autoPopulate(true)
                                              .settingId(cloudProviderId)
                                              .build();

    JsonPath response =
        ArtifactStreamRestUtils.configureArtifactory(bearerToken, application.getAppId(), ecrArtifactStream);
    assertThat(response).isNotNull();
  }

  @Test
  @Owner(developers = VENKATESH)
  @Category(E2ETests.class)
  public void TC3_createEnvironment() {
    Environment myEnv = anEnvironment().name(ENV_MAME).environmentType(EnvironmentType.NON_PROD).build();
    environment = EnvironmentRestUtils.createEnvironment(bearerToken, getAccount(), application.getAppId(), myEnv);
    assertThat(environment).isNotNull();

    String serviceTemplateId = EnvironmentRestUtils.getServiceTemplateId(
        bearerToken, getAccount(), application.getUuid(), environment.getUuid());

    EcsInfrastructureMapping ecsInfrastructureMapping = new EcsInfrastructureMapping();
    ecsInfrastructureMapping.setComputeProviderName(CLOUD_PROVIDER_NAME);
    ecsInfrastructureMapping.setComputeProviderSettingId(cloudProviderId);
    ecsInfrastructureMapping.setComputeProviderType(PROVIDER_TYPE);
    ecsInfrastructureMapping.setDeploymentType(DEPLOYMENT_TYPE);
    ecsInfrastructureMapping.setInfraMappingType(INFRA_TYPE);
    ecsInfrastructureMapping.setRegion(INFRA_REGION);
    ecsInfrastructureMapping.setServiceId(serviceId);
    ecsInfrastructureMapping.setServiceTemplateId(serviceTemplateId);
    ecsInfrastructureMapping.setClusterName(CLUSTER_NAME);
    ecsInfrastructureMapping.setAutoPopulate(true);

    JsonPath response = EnvironmentRestUtils.configureInfraMapping(
        bearerToken, getAccount().getUuid(), application.getUuid(), environment.getUuid(), ecsInfrastructureMapping);
    // assertThat(response).isNotNull();
    awsInfraId = response.getString("resource.uuid").trim();
  }

  @Test
  @Owner(developers = VENKATESH)
  @Category(E2ETests.class)
  public void TC4_createCanaryWorkflow() {
    List<PhaseStep> phaseSteps = new ArrayList<>();

    phaseSteps.add(WorkflowRestUtils.ecsContainerSetupPhaseStep());
    phaseSteps.add(WorkflowRestUtils.ecsContainerDeployPhaseStep());
    phaseSteps.add(aPhaseStep(WRAP_UP, WRAP_UP_CONSTANT).build());

    Workflow workflowBuilder =
        aWorkflow()
            .name("Canary ECS" + System.currentTimeMillis())
            .workflowType(WorkflowType.ORCHESTRATION)
            .appId(application.getUuid())
            .envId(environment.getUuid())
            .infraMappingId(awsInfraId)
            .serviceId(serviceId)
            .orchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, PRE_DEPLOYMENT_CONSTANT).build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, POST_DEPLOYMENT_CONSTANT).build())
                    .addWorkflowPhase(aWorkflowPhase()
                                          .serviceId(serviceId)
                                          .deploymentType(DeploymentType.ECS)
                                          .daemonSet(true)
                                          .infraMappingId(awsInfraId)
                                          .computeProviderId(cloudProviderId)
                                          .phaseSteps(phaseSteps)
                                          .build())
                    .build())
            .build();
    workflow =
        WorkflowRestUtils.createWorkflow(bearerToken, getAccount().getUuid(), application.getUuid(), workflowBuilder);
    assertThat(workflow).isNotNull();
  }

  @Test
  @Owner(developers = VENKATESH, intermittent = false)
  @Category(E2ETests.class)
  public void TC5_deployWorkflow() {
    String artifactStreamId = ArtifactStreamRestUtils.getArtifactStreamId(
        bearerToken, application.getAppId(), environment.getUuid(), serviceId);
    executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(workflow.getWorkflowType());
    List<Artifact> artifacts = new ArrayList<>();
    Artifact artifact = new Artifact();
    artifact.setUuid(artifactStreamId);
    artifacts.add(artifact);
    executionArgs.setArtifacts(artifacts);
    executionArgs.setExecutionCredential(
        SSHExecutionCredential.Builder.aSSHExecutionCredential().withExecutionType(ExecutionType.SSH).build());
    executionArgs.setOrchestrationId(workflow.getUuid());
  }

  @Test
  @Owner(developers = VENKATESH, intermittent = false)
  @Category(E2ETests.class)
  public void TC6_startAndCheck() {
    ExecutionRestUtils.executeAndCheck(
        bearerToken, getAccount(), application.getAppId(), environment.getUuid(), executionArgs);
  }
}