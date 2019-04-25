package io.harness.functional.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;
import static software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;

import io.harness.beans.WorkflowType;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.OwnerRule.Owner;
import io.harness.testframework.restutils.ApplicationRestUtils;
import io.harness.testframework.restutils.ArtifactStreamRestUtils;
import io.harness.testframework.restutils.CloudProviderUtils;
import io.harness.testframework.restutils.ConnectorUtils;
import io.harness.testframework.restutils.EnvironmentRestUtils;
import io.harness.testframework.restutils.ExecutionRestUtils;
import io.harness.testframework.restutils.SSHKeysUtils;
import io.harness.testframework.restutils.ServiceRestUtils;
import io.harness.testframework.restutils.WorkflowRestUtils;
import io.restassured.path.json.JsonPath;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import software.wings.api.DeploymentType;
import software.wings.beans.Application;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.AwsInstanceFilter;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.ExecutionCredential.ExecutionType;
import software.wings.beans.SSHExecutionCredential;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactoryArtifactStream;
import software.wings.utils.ArtifactType;

import java.util.ArrayList;
import java.util.List;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BasicSSHDeploymentWarTest extends AbstractFunctionalTest {
  // TEST constants
  private static long appendString = System.currentTimeMillis();
  private static String APP_NAME = "WarServiceBasicDeploy-" + appendString;
  private static String CLOUD_PROVIDER_NAME = "AWS-Automation-CloudProvider-" + appendString;
  private static String ARTIFACTORY_NAME = "Artifactory-Automation-" + appendString;
  private static String Automation_SSH_KEY = "Automation_SSH_KEY-" + appendString;
  private String SERVICE_NAME = "WarServiceBasicDeploy";
  private String ARTIFACT_PATTERN = "war-automation/todolist-1.war";
  private String ARTIFACT_TYPE = "ARTIFACTORY";
  private String REPOSITORY_TYPE = "Any";
  private String JOB_NAME = "qa-test";
  private static String ACCNT_ID = "kmpySmUISimoRrJL6NL73w";
  private String ENV_MAME = "QA";
  private String PROVIDER_NAME = "Amazon Web Services: " + CLOUD_PROVIDER_NAME;
  private String DEPLOYMENT_TYPE = "SSH";
  private String PROVIDER_TYPE = "AWS";
  private String INFRA_TYPE = "AWS_SSH";
  private String INFRA_REGION = "us-east-1";
  private String HOST_NAME_CONV = "${host.ec2Instance.privateDnsName.split('\\.')[0]}";
  private String WF_NAME = "BasicWarWorkflow";

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

  @Test
  @Owner(emails = "sunil@harness.io", resent = false)
  @Category(FunctionalTests.class)
  public void TC1_createApplication() {
    // Test data setup
    cloudProviderId = CloudProviderUtils.createAWSCloudProvider(bearerToken, CLOUD_PROVIDER_NAME, ACCNT_ID);
    artifactoryId = ConnectorUtils.createArtifactoryConnector(bearerToken, CLOUD_PROVIDER_NAME, ACCNT_ID);
    sshKeyId = SSHKeysUtils.createSSHKey(bearerToken, CLOUD_PROVIDER_NAME, ACCNT_ID);

    Application warApp = anApplication().name(APP_NAME).build();
    application = ApplicationRestUtils.createApplication(bearerToken, getAccount(), warApp);
    assertThat(application).isNotNull();
  }

  @Test
  @Owner(emails = "sunil@harness.io", resent = false)
  @Category(FunctionalTests.class)
  public void TC2_createServiceAndCollectArtifact() {
    Service warService =
        Service.builder().name(SERVICE_NAME).deploymentType(DeploymentType.SSH).artifactType(ArtifactType.WAR).build();
    serviceId = ServiceRestUtils.createSSHService(bearerToken, application.getAppId(), warService);
    assertThat(serviceId).isNotNull();

    ArtifactStream artifactStreamReg = ArtifactoryArtifactStream.builder()
                                           .artifactPattern(ARTIFACT_PATTERN)
                                           .repositoryType(REPOSITORY_TYPE)
                                           .jobname(JOB_NAME)
                                           .autoPopulate(true)
                                           .serviceId(serviceId)
                                           .settingId(artifactoryId)
                                           .build();
    artifactStreamReg.setArtifactStreamType(ARTIFACT_TYPE);
    JsonPath response =
        ArtifactStreamRestUtils.configureArtifactory(bearerToken, application.getAppId(), artifactStreamReg);
    assertThat(response).isNotNull();
  }

  @Test
  @Owner(emails = "sunil@harness.io", resent = false)
  @Category(FunctionalTests.class)
  public void TC3_createEnvironment() {
    Environment myEnv = anEnvironment().withName(ENV_MAME).withEnvironmentType(EnvironmentType.NON_PROD).build();
    environment = EnvironmentRestUtils.createEnvironment(bearerToken, getAccount(), application.getAppId(), myEnv);
    assertThat(environment).isNotNull();

    String serviceTemplateId = EnvironmentRestUtils.getServiceTemplateId(
        bearerToken, getAccount(), application.getUuid(), environment.getUuid());

    AwsInfrastructureMapping awsInfraMap = anAwsInfrastructureMapping()
                                               .withComputeProviderName(CLOUD_PROVIDER_NAME)
                                               .withComputeProviderSettingId(cloudProviderId)
                                               .withComputeProviderType(PROVIDER_TYPE)
                                               .withDeploymentType(DEPLOYMENT_TYPE)
                                               .withHostConnectionAttrs(sshKeyId)
                                               .withHostNameConvention(HOST_NAME_CONV)
                                               .withInfraMappingType(INFRA_TYPE)
                                               .withProvisionInstances(false)
                                               .withRegion(INFRA_REGION)
                                               .withServiceId(serviceId)
                                               .withServiceTemplateId(serviceTemplateId)
                                               .withAwsInstanceFilter(AwsInstanceFilter.builder().build())
                                               .withUsePublicDns(true)
                                               .withAutoPopulate(true)
                                               .build();

    JsonPath response = EnvironmentRestUtils.configureInfraMapping(
        bearerToken, getAccount(), application.getUuid(), environment.getUuid(), awsInfraMap);
    assertThat(response).isNotNull();
    awsInfraId = response.getString("resource.uuid").trim();
  }

  @Test
  @Owner(emails = "sunil@harness.io", resent = false)
  @Category(FunctionalTests.class)
  public void TC4_createWorkflow() throws Exception {
    Workflow workflowBuilder =
        aWorkflow()
            .name(WF_NAME)
            .envId(environment.getUuid())
            .serviceId(serviceId)
            .infraMappingId(awsInfraId)
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(aBasicOrchestrationWorkflow()
                                       .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, "PRE DEPLOYMENT").build())
                                       .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, "POST DEPLOYMENT").build())
                                       .build())
            .build();

    workflow =
        WorkflowRestUtils.createWorkflow(bearerToken, getAccount().getUuid(), application.getUuid(), workflowBuilder);
    assertThat(workflow).isNotNull();
  }

  @Test
  @Owner(emails = "sunil@harness.io", intermittent = false)
  @Category(FunctionalTests.class)
  public void TC5_deployWorkflow() {
    String artifactStreamId = ArtifactStreamRestUtils.getArtifactStreamId(
        bearerToken, application.getAppId(), environment.getUuid(), serviceId);
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(workflow.getWorkflowType());
    List<Artifact> artifacts = new ArrayList<>();
    Artifact artifact = new Artifact();
    artifact.setUuid(artifactStreamId);
    artifacts.add(artifact);
    executionArgs.setArtifacts(artifacts);
    executionArgs.setExecutionCredential(
        SSHExecutionCredential.Builder.aSSHExecutionCredential().withExecutionType(ExecutionType.SSH).build());
    executionArgs.setOrchestrationId(workflow.getUuid());

    workflowExecution =
        ExecutionRestUtils.runWorkflow(bearerToken, application.getAppId(), environment.getUuid(), executionArgs);
    assertThat(workflowExecution).isNotNull();
  }

  @Test
  @Owner(emails = "sunil@harness.io")
  @Category(FunctionalTests.class)
  public void TC6_checkExecutionStarted() {
    String status = ExecutionRestUtils.getWorkflowExecutionStatus(
        bearerToken, getAccount(), application.getAppId(), workflowExecution.getUuid());
    if (!(status.equals("RUNNING") || status.equals("QUEUED"))) {
      Assert.fail("ERROR: Execution did not START");
    }
  }
}