package io.harness.e2e.dailysanity.cdworkflow;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;

import io.harness.e2e.AbstractE2ETest;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.framework.utils.TestUtils;
import io.harness.testframework.restutils.AccountRestUtils;
import io.harness.testframework.restutils.ApplicationRestUtils;
import io.harness.testframework.restutils.ArtifactStreamRestUtils;
import io.harness.testframework.restutils.CloudProviderUtils;
import io.harness.testframework.restutils.ConnectorUtils;
import io.harness.testframework.restutils.SSHKeysUtils;
import io.harness.testframework.restutils.ServiceRestUtils;
import io.restassured.path.json.JsonPath;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.DeploymentType;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactoryArtifactStream;
import software.wings.utils.ArtifactType;

@Slf4j
public class LoadTest extends AbstractE2ETest {
  // TEST constants
  private static long appendString = System.currentTimeMillis();
  private static String APP_NAME = "WarServiceBasicDeploy-" + appendString;
  private static String CLOUD_PROVIDER_NAME = "AWS-Automation-CloudProvider-" + appendString;
  private static String ARTIFACTORY_NAME = "Artifactory-Automation-" + appendString;
  private static String SSHKEY_NAME = "Aws_playground_ec2_user_key_Automation-" + appendString;
  private static String SERVICE_NAME = "WarService";
  private static String ARTIFACT_PATTERN = "war-automation/todolist-1.war";
  private static String ARTIFACT_TYPE = "ARTIFACTORY";
  private static String REPOSITORY_TYPE = "Any";
  private static String JOB_NAME = "qa-test";
  static Account account;

  private String ENV_MAME = "QA";
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
  private static ExecutionArgs executionArgs;

  public static void main(String[] arg) {
    // Test data setup
    qaAccount1 = TestUtils.getDecryptedValue("e2etest_qa_account1");
    bearerToken = Setup.getAuthToken("autouser1@harness.io", TestUtils.getDecryptedValue("e2etest_autouser_password"));
    account = AccountRestUtils.getAccount(qaAccount1, bearerToken);

    cloudProviderId = CloudProviderUtils.createAWSCloudProvider(bearerToken, CLOUD_PROVIDER_NAME, account.getUuid());
    artifactoryId = ConnectorUtils.createArtifactoryConnector(bearerToken, ARTIFACTORY_NAME, account.getUuid());
    sshKeyId = SSHKeysUtils.createSSHKey(bearerToken, SSHKEY_NAME, account.getUuid());

    Application warApp = anApplication().name(APP_NAME).build();
    application = ApplicationRestUtils.createApplication(bearerToken, account, warApp);
    assertThat(application).isNotNull();

    for (int i = 1; i < 2000; i++) {
      Service warService = Service.builder()
                               .name(SERVICE_NAME + i)
                               .deploymentType(DeploymentType.SSH)
                               .artifactType(ArtifactType.WAR)
                               .build();
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
  }
}
