package software.wings.integration.yaml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.beans.Environment.GLOBAL_ENV_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.scm.ScmSecret;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.GitConfig;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.InfrastructureProvisionerType;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.beans.yaml.YamlConstants;
import software.wings.integration.BaseIntegrationTest;
import software.wings.service.impl.yaml.GitClientHelper;
import software.wings.service.impl.yaml.GitClientImpl;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.yaml.GitClient;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Ignore
@Slf4j
public class YamlIntegrationTest extends BaseIntegrationTest {
  private Application application;
  private Service service;
  private YamlGitConfig yamlGitConfig;
  private SettingAttribute gitConnector;
  @Inject private YamlIntegrationTestHelper yamlIntegrationTestHelper;
  @Inject private SettingsService settingsService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private YamlGitService yamlGitService;
  @Inject private EncryptionService encryptionService;
  @Inject private GitIntegrationTestUtil gitIntegrationTestUtil;
  @Inject private ScmSecret scmSecret;
  @Inject private InfrastructureProvisionerService infrastructureProvisionerService;
  @Inject private EnvironmentService environmentService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private ServiceTemplateService serviceTemplateService;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    GitClient gitClient = new GitClientImpl();
    setInternalState(gitClient, "gitClientHelper", new GitClientHelper());
    setInternalState(gitIntegrationTestUtil, "gitClient", gitClient);
    loginAdminUser();
    setYamlGitConfig();
    setGitConnector();
    decryptGitConfig();
  }

  private void decryptGitConfig() {
    yamlIntegrationTestHelper.decryptGitConfig(
        (GitConfig) gitConnector.getValue(), secretManager, logger, encryptionService);
  }

  private void setGitConnector() {
    gitConnector = yamlIntegrationTestHelper.createGitConnector(yamlGitConfig, wingsPersistence, scmSecret);
  }

  private void setYamlGitConfig() {
    yamlGitConfig =
        yamlIntegrationTestHelper.createYamlGitConfig(accountId, yamlGitService, wingsPersistence, scmSecret);
  }

  @Test
  @Category(UnitTests.class)
  public void testGitSyncAppCreatedOnHarness() throws Exception {
    String appName = "App" + System.currentTimeMillis();
    String yamlPath =
        new StringBuilder().append("Setup/Applications/").append(appName).append("/Index.yaml").toString();

    makeSureFileDoesntExistInRepo(yamlPath);

    Application application = yamlIntegrationTestHelper.createApplication(appName, accountId, appService);
    logger.info("Created Application : " + application.getName());

    GitFetchFilesResult gitFetchFilesResult = getGitFetchFilesResult(yamlPath);
    assertEquals(1, gitFetchFilesResult.getFiles().size());
    assertEquals("Index.yaml", gitFetchFilesResult.getFiles().get(0).getFilePath());
  }

  @Test
  @Category(UnitTests.class)
  public void testGitSyncServiceCreatedOnHarness() throws Exception {
    if (application == null) {
      application =
          yamlIntegrationTestHelper.createApplication("App" + System.currentTimeMillis(), accountId, appService);
      logger.info("Created Application : " + application.getName());
    }
    String serviceName = "Service" + System.currentTimeMillis();
    String yamlPath = new StringBuilder()
                          .append("Setup/Applications/")
                          .append(application.getName())
                          .append("/Services/")
                          .append(serviceName)
                          .toString();

    makeSureFileDoesntExistInRepo(yamlPath);
    Service service = yamlIntegrationTestHelper.createService(serviceName, application, serviceResourceService);
    logger.info("Created Service : " + service.getName());

    getGitFetchFilesResultByCount(yamlPath, 2);
  }

  private GitFetchFilesResult getGitFetchFilesResult(String yamlPath) throws Exception {
    return getGitFetchFilesResult(yamlPath, false, 1);
  }

  private GitFetchFilesResult getGitFetchFilesResultByCount(String yamlPath, int expectedCountOfFiles)
      throws Exception {
    return getGitFetchFilesResult(yamlPath, false, expectedCountOfFiles);
  }

  private GitFetchFilesResult getGitFetchFilesResult(
      String yamlPath, boolean isFailureExpected, int expectedCountOfFiles) throws Exception {
    logger.info("Executing  YamlIntegrationTest.getGitFetchFilesResult()");
    GitFetchFilesResult gitFetchFilesResult = null;
    for (int count = 0; count < 18; count++) {
      if (count > 0 && isFailureExpected) {
        throw new RuntimeException("Failed to fetch yaml files from git");
      }

      try {
        Thread.sleep(10000);
        gitFetchFilesResult =
            gitIntegrationTestUtil.fetchFromGitUsingUsingBranch(gitConnector, "test", logger, Arrays.asList(yamlPath));
        // No exception means we got files from git

        if (expectedCountOfFiles == gitFetchFilesResult.getFiles().size()) {
          logger.info("Retrieved files from git successfully");
          break;
        }
      } catch (Exception e) {
        if (!isFailureExpected) {
          logger.warn("Failed to retrieve files those were expected to be in Git repo");
        } else {
          logger.info("Not able to fetch file: " + e.getMessage());
        }
      }
    }

    assertNotNull(gitFetchFilesResult);
    assertNotNull(gitFetchFilesResult.getFiles());
    return gitFetchFilesResult;
  }

  private void makeSureFileDoesntExistInRepo(String yamlPath) {
    try {
      getGitFetchFilesResult(yamlPath, true, 0);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(true);
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testGitSyncCloudProviderCreatedOnHarness() throws Exception {
    if (application == null) {
      application = yamlIntegrationTestHelper.createApplication(
          YamlIntegrationTestConstants.APP + System.currentTimeMillis(), accountId, appService);
    }

    String cloudProviderName = YamlIntegrationTestConstants.PHYSICAL_CLOUD_PROVIDER + System.currentTimeMillis();
    SettingAttribute settingAttribute = yamlIntegrationTestHelper.createSettingAttribute(cloudProviderName, accountId,
        application.getAppId(), GLOBAL_ENV_ID, SettingVariableTypes.PHYSICAL_DATA_CENTER, settingsService);

    String yamlPath = yamlIntegrationTestHelper.getCloudProviderYamlPath(cloudProviderName);

    GitFetchFilesResult gitFetchFilesResult = getGitFetchFilesResult(yamlPath);
    assertEquals(1, gitFetchFilesResult.getFiles().size());
    assertEquals(cloudProviderName + YamlConstants.YAML_EXTENSION, gitFetchFilesResult.getFiles().get(0).getFilePath());
  }

  @Test
  @Category(UnitTests.class)
  public void testGitSyncInfraProvisionerCreatedOnHarness() throws Exception {
    if (application == null) {
      application = yamlIntegrationTestHelper.createApplication(
          YamlIntegrationTestConstants.APP + System.currentTimeMillis(), accountId, appService);
    }

    String infraProvisionerName = YamlIntegrationTestConstants.INFRA_PROVISIONER + System.currentTimeMillis();
    InfrastructureProvisioner infraProvisioner = yamlIntegrationTestHelper.createInfraProvisioner(infraProvisionerName,
        application.getAppId(), InfrastructureProvisionerType.CLOUD_FORMATION, infrastructureProvisionerService);

    String yamlPath = yamlIntegrationTestHelper.getInfraProvisionerYamlPath(application, infraProvisioner);

    GitFetchFilesResult gitFetchFilesResult = getGitFetchFilesResult(yamlPath);
    assertEquals(1, gitFetchFilesResult.getFiles().size());
    assertEquals(
        infraProvisionerName + YamlConstants.YAML_EXTENSION, gitFetchFilesResult.getFiles().get(0).getFilePath());
  }

  @Test
  @Category(UnitTests.class)
  public void testGitSyncEnvironmentCreatedOnHarness() throws Exception {
    if (application == null) {
      application = yamlIntegrationTestHelper.createApplication(
          YamlIntegrationTestConstants.APP + System.currentTimeMillis(), accountId, appService);
    }

    String envName = YamlIntegrationTestConstants.ENVIRONMENT + System.currentTimeMillis();
    Environment environment =
        yamlIntegrationTestHelper.createEnvironment(envName, application.getAppId(), environmentService);

    String yamlPath = yamlIntegrationTestHelper.getEnvironmentYamlPath(application, environment);
    GitFetchFilesResult gitFetchFilesResult = getGitFetchFilesResult(yamlPath);
    assertEquals(1, gitFetchFilesResult.getFiles().size());
    assertEquals(YamlConstants.INDEX_YAML, gitFetchFilesResult.getFiles().get(0).getFilePath());
  }

  // Environment with Physical Service Infra
  @Test
  @Category(UnitTests.class)
  public void testGitSyncEnvironment1CreatedOnHarness() throws Exception {
    if (application == null) {
      application = yamlIntegrationTestHelper.createApplication(
          YamlIntegrationTestConstants.APP + System.currentTimeMillis(), accountId, appService);
    }

    String serviceName = YamlIntegrationTestConstants.SERVICE;
    Service service = yamlIntegrationTestHelper.createService(serviceName, application, serviceResourceService);

    String envName = YamlIntegrationTestConstants.ENVIRONMENT;
    Environment environment =
        yamlIntegrationTestHelper.createEnvironment(envName, application.getAppId(), environmentService);

    String serviceTemplateName = YamlIntegrationTestConstants.SERVICE_TEMPLATE;
    ServiceTemplate serviceTemplate = yamlIntegrationTestHelper.createServiceTemplate(
        serviceTemplateName, application.getAppId(), service.getUuid(), environment.getUuid(), serviceTemplateService);

    String settingAttributeName = YamlIntegrationTestConstants.CONNECTION_NAME;
    SettingAttribute settingAttribute =
        yamlIntegrationTestHelper.createSettingAttribute(settingAttributeName, accountId, application.getAppId(),
            environment.getUuid(), SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES, settingsService);

    String computeProviderName = YamlIntegrationTestConstants.COMPUTE_PROVIDER_PHYSICAL;
    SettingAttribute computeProviderSetting =
        yamlIntegrationTestHelper.createSettingAttribute(computeProviderName, accountId, application.getAppId(),
            environment.getUuid(), SettingVariableTypes.PHYSICAL_DATA_CENTER, settingsService);

    InfrastructureMapping infrastructureMapping = yamlIntegrationTestHelper.getInfrastructureMapping(accountId,
        application.getAppId(), service.getUuid(), environment.getUuid(),
        InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH, computeProviderName, serviceTemplate.getUuid(),
        settingAttribute.getUuid(), computeProviderSetting.getUuid(), infrastructureMappingService);

    String yamlPath = yamlIntegrationTestHelper.getEnvironmentYamlPath(application, environment);
    GitFetchFilesResult gitFetchFilesResult = getGitFetchFilesResultByCount(yamlPath, 2);

    Map<String, String> expectedFiles = new HashMap<>();
    expectedFiles.put(YamlConstants.INDEX_YAML, YamlIntegrationTestEnvironmentConstants.ENVIRONMENT_1);
    expectedFiles.put(infrastructureMapping.getName() + YamlConstants.YAML_EXTENSION,
        YamlIntegrationTestEnvironmentConstants.ENVIRONMENT_1_SERVICE_INFRA);
    yamlIntegrationTestHelper.verifyResultFromGit(expectedFiles, gitFetchFilesResult);
  }
}
