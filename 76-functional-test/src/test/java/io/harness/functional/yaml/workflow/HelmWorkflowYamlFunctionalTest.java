package io.harness.functional.yaml.workflow;

import static io.harness.functional.yaml.YamlFunctionalTestConstants.BASE_CLONE_PATH;
import static io.harness.functional.yaml.YamlFunctionalTestConstants.BASE_GIT_REPO_PATH;
import static io.harness.functional.yaml.YamlFunctionalTestConstants.BASE_VERIFY_CLONE_PATH;
import static io.harness.functional.yaml.YamlFunctionalTestConstants.YAML_WEBHOOK_PAYLOAD_GITHUB;
import static io.harness.generator.AccountGenerator.ACCOUNT_ID;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofSeconds;
import static software.wings.beans.yaml.YamlConstants.APPLICATIONS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.SETUP_FOLDER_PATH;

import com.google.inject.Inject;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.yaml.YamlFunctionalTestHelper;
import io.harness.rule.OwnerRule.Owner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.GitConfig;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.AppService;

import java.nio.file.Paths;

public class HelmWorkflowYamlFunctionalTest extends AbstractFunctionalTest {
  private static final Logger logger = LoggerFactory.getLogger(HelmWorkflowYamlFunctionalTest.class);

  @Inject private AppService appService;
  @Inject YamlFunctionalTestHelper yamlFunctionalTestHelper;

  private SettingAttribute gitConnector;

  // oldAppName is the name of zip file at location io.harness/yaml/ in the resources.
  // We will use classpath:io.harness/yaml/<OLD_APP_NAME>.zip to create application
  private String OLD_APP_NAME = "Helm-App";
  private String NEW_APP_NAME = OLD_APP_NAME + "-1";

  private String CLOUD_PROVIDER_NAME;
  private String GIT_CONNECTOR_NAME;

  private String REPO_NAME;
  private String GIT_REPO_PATH;
  private String CLONE_PATH;
  private String VERIFY_CLONE_PATH;

  @Before
  public void setUpConfig() {
    REPO_NAME = this.getClass().getSimpleName();
    GIT_CONNECTOR_NAME = "GitConnector-" + REPO_NAME;
    CLOUD_PROVIDER_NAME = "CloudProvider-" + REPO_NAME;

    GIT_REPO_PATH = Paths.get(BASE_GIT_REPO_PATH, REPO_NAME).toString();
    VERIFY_CLONE_PATH = Paths.get(BASE_VERIFY_CLONE_PATH, REPO_NAME).toString();
    CLONE_PATH = Paths.get(BASE_CLONE_PATH, REPO_NAME).toString();

    cleanUpConfig();
    createLocalGitRepo();
    createGCPCloudProvider();
    createGitConnector();
  }

  @Test
  @Owner(emails = "anshul@harness.io", intermittent = true)
  @Category(FunctionalTests.class)
  public void testHelmWorkflow() {
    uploadYamlZipFile(ACCOUNT_ID, OLD_APP_NAME, SETUP_FOLDER_PATH + APPLICATIONS_FOLDER);
    // TODO: we cannot have such sleeps in the test code
    sleep(ofSeconds(20));

    Application app = appService.getAppByName(ACCOUNT_ID, OLD_APP_NAME);
    createYamlGitConfig(ACCOUNT_ID, app.getUuid(), gitConnector.getUuid());
    // TODO: ditto
    sleep(ofSeconds(20));

    yamlFunctionalTestHelper.duplicateApplication(OLD_APP_NAME, NEW_APP_NAME, REPO_NAME, GIT_REPO_PATH, CLONE_PATH);
    // TODO: ditto
    sleep(ofSeconds(2));

    String webhookToken = ((GitConfig) gitConnector.getValue()).getWebhookToken();
    triggerWebhookPost(ACCOUNT_ID, webhookToken, YAML_WEBHOOK_PAYLOAD_GITHUB);
    // TODO: ditto
    sleep(ofSeconds(20));

    verify(OLD_APP_NAME, NEW_APP_NAME, VERIFY_CLONE_PATH);
  }

  @After
  public void cleanUpConfig() {
    yamlFunctionalTestHelper.cleanUpApplication(OLD_APP_NAME, NEW_APP_NAME);
    yamlFunctionalTestHelper.deleteSettingAttributeByName(CLOUD_PROVIDER_NAME);
    yamlFunctionalTestHelper.deleteSettingAttributeByName(GIT_CONNECTOR_NAME);

    yamlFunctionalTestHelper.cleanUpRepoFolders(REPO_NAME, GIT_REPO_PATH, CLONE_PATH, VERIFY_CLONE_PATH);
  }

  private void createGCPCloudProvider() {
    yamlFunctionalTestHelper.createGCPCloudProvider(CLOUD_PROVIDER_NAME);
  }

  private void createGitConnector() {
    gitConnector = yamlFunctionalTestHelper.createGitConnector(GIT_CONNECTOR_NAME, GIT_REPO_PATH, bearerToken);
  }

  private void createLocalGitRepo() {
    yamlFunctionalTestHelper.createLocalGitRepo(GIT_REPO_PATH);
  }

  private void createYamlGitConfig(String accountId, String appId, String connectorId) {
    yamlFunctionalTestHelper.createYamlGitConfig(accountId, appId, connectorId, bearerToken);
  }

  private void triggerWebhookPost(String accountId, String webhookToken, String yamlWebhookPayload) {
    yamlFunctionalTestHelper.triggerWebhookPost(accountId, webhookToken, yamlWebhookPayload, bearerToken);
  }

  private void uploadYamlZipFile(String accountId, String oldAppName, String yamlPath) {
    yamlFunctionalTestHelper.uploadYamlZipFile(accountId, oldAppName, yamlPath, bearerToken);
  }

  private void verify(String oldAppName, String newAppName, String clonedRepoPath) {
    yamlFunctionalTestHelper.verify(oldAppName, newAppName, REPO_NAME, GIT_REPO_PATH, clonedRepoPath);
  }
}