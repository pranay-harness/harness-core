package software.wings.integration.yaml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import software.wings.beans.Application;
import software.wings.beans.GitConfig;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.generator.ScmSecret;
import software.wings.integration.BaseIntegrationTest;
import software.wings.service.impl.yaml.GitClientHelper;
import software.wings.service.impl.yaml.GitClientImpl;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.yaml.GitClient;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.Arrays;

// This is to be removed no later than Friday, 9/21/2018
@Ignore
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
  @Inject private YamlHandlerFactory yamlHandlerFactory;
  @Inject private GitIntegrationTestUtil gitIntegrationTestUtil;
  @Inject private ScmSecret scmSecret;

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
  public void testGitSyncAppCreatedOnHarness() throws Exception {
    String appName = "App" + System.currentTimeMillis();
    String yamlPath =
        new StringBuilder().append("Setup/Applications/").append(appName).append("/Index.yaml").toString();

    makeSureFileDoesntExistInRepo(yamlPath);

    yamlIntegrationTestHelper.createApplication(appName, accountId, appService);

    GitFetchFilesResult gitFetchFilesResult = getGitFetchFilesResult(yamlPath, false);
    assertEquals(1, gitFetchFilesResult.getFiles().size());
    assertEquals("Index.yaml", gitFetchFilesResult.getFiles().get(0).getFilePath());
  }

  @Test
  public void testGitSyncServiceCreatedOnHarness() throws Exception {
    if (application == null) {
      application =
          yamlIntegrationTestHelper.createApplication("App" + System.currentTimeMillis(), accountId, appService);
    }
    String serviceName = "Service" + System.currentTimeMillis();
    String yamlPath = new StringBuilder()
                          .append("Setup/Applications/")
                          .append(application.getName())
                          .append("/Services/")
                          .append(serviceName)
                          .toString();

    makeSureFileDoesntExistInRepo(yamlPath);
    yamlIntegrationTestHelper.createService(serviceName, application, serviceResourceService);

    getGitFetchFilesResult(yamlPath, false);
  }

  private GitFetchFilesResult getGitFetchFilesResult(String yamlPath, boolean isFailureExpected) throws Exception {
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
        break;
      } catch (Exception e) {
        logger.info("Not able to fetch file: " + e.getMessage());
      }
    }

    assertNotNull(gitFetchFilesResult);
    assertNotNull(gitFetchFilesResult.getFiles());
    return gitFetchFilesResult;
  }

  private void makeSureFileDoesntExistInRepo(String yamlPath) {
    try {
      getGitFetchFilesResult(yamlPath, true);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(true);
    }
  }
}
