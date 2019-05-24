package io.harness.functional.yaml;

import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.generator.AccountGenerator.ACCOUNT_ID;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.filesystem.FileIo;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.resource.Project;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.harness.testframework.framework.Setup;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ResourceUtils;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.GcpConfig;
import software.wings.beans.GitConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Builder;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.yaml.GitFileChange;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.yaml.gitSync.YamlGitConfig;
import software.wings.yaml.gitSync.YamlGitConfig.SyncMode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

@Singleton
@Slf4j
public class YamlFunctionalTestHelper {
  @Inject private ScmSecret scmSecret;
  @Inject private SettingsService settingsService;
  @Inject private AppService appService;
  @Inject private YamlGitService yamlGitService;
  @Inject WingsPersistence wingsPersistence;

  public SettingAttribute createSettingAttribute(
      String settingAttributeName, SettingValue settingValue, String bearerToken) {
    SettingAttribute settingAttribute =
        Builder.aSettingAttribute()
            .withName(settingAttributeName)
            .withAccountId(ACCOUNT_ID)
            .withAppId(GLOBAL_APP_ID)
            .withValue(settingValue)
            .withCategory(SettingCategory.getCategory(SettingVariableTypes.valueOf(settingValue.getType())))
            .build();

    Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("accountId", ACCOUNT_ID)
        .queryParam("appId", GLOBAL_APP_ID)
        .body(settingAttribute, ObjectMapperType.GSON)
        .contentType(ContentType.JSON)
        .post("/settings");

    settingAttribute = settingsService.getByName(ACCOUNT_ID, GLOBAL_APP_ID, settingAttributeName);
    assertThat(settingAttribute).isNotNull();

    return settingAttribute;
  }

  public SettingAttribute createGCPCloudProvider(String cloudProviderName) {
    GcpConfig gcpConfig = GcpConfig.builder()
                              .serviceAccountKeyFileContent(
                                  scmSecret.decryptToString(new SecretName("harness_gcp_exploration")).toCharArray())
                              .build();
    gcpConfig.setDecrypted(true);
    gcpConfig.setAccountId(ACCOUNT_ID);

    SettingAttribute settingAttribute =
        Builder.aSettingAttribute()
            .withName(cloudProviderName)
            .withAccountId(ACCOUNT_ID)
            .withAppId(GLOBAL_APP_ID)
            .withValue(gcpConfig)
            .withCategory(SettingCategory.getCategory(SettingVariableTypes.valueOf(gcpConfig.getType())))
            .build();

    settingAttribute = settingsService.save(settingAttribute);
    assertThat(settingAttribute).isNotNull();

    return settingAttribute;
  }

  public SettingAttribute createGitConnector(String gitConnectorName, String gitRepoPath, String bearerToken) {
    GitConfig gitConfig = GitConfig.builder()
                              .accountId(ACCOUNT_ID)
                              .repoUrl(gitRepoPath)
                              .username("abc")
                              .password("xyz".toCharArray())
                              .generateWebhookUrl(true)
                              .build();
    return createSettingAttribute(gitConnectorName, gitConfig, bearerToken);
  }

  private String getFilePath(Path path, String repoPath) {
    Path fileAbsolutePath = path.toAbsolutePath();
    Path repoAbsolutePath = Paths.get(repoPath).toAbsolutePath();
    return repoAbsolutePath.relativize(fileAbsolutePath).toString();
  }

  private void addFilesToMap(Path path, String repoPath, Map<String, String> map) {
    StringBuilder contentBuilder = new StringBuilder();
    try (Stream<String> stream = Files.lines(path, StandardCharsets.UTF_8)) {
      stream.forEach(s -> contentBuilder.append(s).append("\n"));
    } catch (IOException e) {
      logger.error("Failed to read file Content {}", path.toString());
      throw new WingsException(GENERAL_ERROR, "Failed to read file Content {}", e);
    }

    String content = contentBuilder.toString();
    String filePath = getFilePath(path, repoPath);
    map.put(filePath, content);
  }

  public void executeCommand(String command) {
    try {
      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(30, TimeUnit.SECONDS)
                                            .command("/bin/sh", "-c", command)
                                            .readOutput(true)
                                            .redirectOutput(new LogOutputStream() {
                                              @Override
                                              protected void processLine(String line) {
                                                logger.info(line);
                                              }
                                            });

      ProcessResult processResult = processExecutor.execute();
      assertThat(processResult.getExitValue()).isEqualTo(0);
    } catch (InterruptedException | TimeoutException | IOException ex) {
      logger.error(ExceptionUtils.getMessage(ex));
      assertTrue(false);
    }
  }

  private Map<String, String> getFilesMapFromGit(String repoName, String gitRepoPath, String clonedRepoPath) {
    String command = new StringBuilder(128)
                         .append("mkdir -p " + clonedRepoPath + ";")
                         .append("cd " + clonedRepoPath + ";")
                         .append("git clone " + gitRepoPath + ";")
                         .toString();
    executeCommand(command);

    Map<String, String> map = new HashMap<>();

    try {
      Stream<Path> paths = Files.walk(Paths.get(clonedRepoPath + "/" + repoName));
      paths.filter(Files::isRegularFile)
          .filter(path -> !path.toString().contains(".git"))
          .forEach(path -> addFilesToMap(path, clonedRepoPath + "/" + repoName, map));
    } catch (IOException ex) {
      logger.error(ExceptionUtils.getMessage(ex));
      assertTrue(false);
    } catch (Exception e) {
      logger.error(ExceptionUtils.getMessage(e));
    }

    return map;
  }

  private Map<String, String> getFilesMapFromHarness(String oldAppName, String newAppName) {
    Application oldApp = appService.getAppByName(ACCOUNT_ID, oldAppName);
    Application newApp = appService.getAppByName(ACCOUNT_ID, newAppName);

    List<GitFileChange> oldAppChanges = yamlGitService.obtainApplicationYamlGitFileChanges(ACCOUNT_ID, oldApp);
    List<GitFileChange> newAppChanges = yamlGitService.obtainApplicationYamlGitFileChanges(ACCOUNT_ID, newApp);

    Map<String, String> map = new HashMap<>();

    for (GitFileChange gitFileChange : oldAppChanges) {
      map.put(gitFileChange.getFilePath(), gitFileChange.getFileContent());
    }
    for (GitFileChange gitFileChange : newAppChanges) {
      map.put(gitFileChange.getFilePath(), gitFileChange.getFileContent());
    }

    return map;
  }

  public void verify(String oldAppName, String newAppName, String repoName, String repoPath, String clonedRepoPath) {
    Map<String, String> filesFromHarness = getFilesMapFromHarness(oldAppName, newAppName);
    Map<String, String> filesFromGit = getFilesMapFromGit(repoName, repoPath, clonedRepoPath);

    assertThat(filesFromHarness.size()).isEqualTo(filesFromGit.size());

    int count = 0;
    for (Entry<String, String> entry : filesFromHarness.entrySet()) {
      String filePath = entry.getKey();

      if (!entry.getValue().equals(filesFromGit.get(filePath))) {
        logger.info("File path: " + filePath);
        logger.info("File Content in harness: " + entry.getValue());
        logger.info("File Content in git: " + filesFromGit.get(filePath));
        count++;
      }
    }

    assertThat(count).isEqualTo(0);
  }

  public void triggerWebhookPost(String accountId, String webhookToken, String yamlWebhookPayload, String bearerToken) {
    HashMap<String, String> objectObjectHashMap = new HashMap<>();
    objectObjectHashMap.put("X-GitHub-Event", "abc");

    Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("accountId", accountId)
        .pathParam("entityToken", webhookToken)
        .body(yamlWebhookPayload)
        .contentType(ContentType.JSON)
        .headers(objectObjectHashMap)
        //.header(new Header("X-GitHub-Event", "abc"))
        .post("/setup-as-code/yaml/webhook/{entityToken}");
  }

  public void uploadYamlZipFile(String accountId, String oldAppName, String yamlPath, String bearerToken) {
    File file = null;
    try {
      file = ResourceUtils.getFile("classpath:io/harness/yaml/" + oldAppName + ".zip");
    } catch (FileNotFoundException ex) {
      logger.error(ExceptionUtils.getMessage(ex));
      assertTrue(false);
    }

    Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("accountId", accountId)
        .queryParam("yamlPath", yamlPath)
        .contentType(MULTIPART_FORM_DATA)
        .multiPart(file)
        .post("/setup-as-code/yaml/yaml-as-zip");
  }

  public void createYamlGitConfig(String accountId, String appId, String connectorId, String bearerToken) {
    YamlGitConfig yamlGitConfig = YamlGitConfig.builder()
                                      .accountId(accountId)
                                      .entityId(appId)
                                      .syncMode(SyncMode.BOTH)
                                      .branchName("master")
                                      .enabled(true)
                                      .gitConnectorId(connectorId)
                                      .entityType(EntityType.APPLICATION)
                                      .build();
    yamlGitConfig.setAppId(appId);

    Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("accountId", accountId)
        .body(yamlGitConfig)
        .contentType(ContentType.JSON)
        .post("/setup-as-code/yaml/git-config");
  }

  public void duplicateApplication(
      String oldAppName, String newAppName, String repoName, String gitRepoPath, String clonePath) {
    String command = new StringBuilder(128)
                         .append("mkdir -p " + clonePath + ";")
                         .append("cd " + clonePath + ";")
                         .append("git clone " + gitRepoPath + ";")
                         .append("cd " + repoName + ";")
                         .append("cp -r Setup/Applications/" + oldAppName + " Setup/Applications/" + newAppName + ";")
                         .append("git add Setup;")
                         .append("git commit -m " + newAppName + ";")
                         .append("git push;")
                         .toString();

    executeCommand(command);
  }

  public void cleanHarnessRepo(String repoName) {
    try {
      String directoryPath = Project.rootDirectory(AbstractFunctionalTest.class);
      String harnessYamPath =
          Paths.get(directoryPath, "81-delegate", "repository", "yaml", ACCOUNT_ID, repoName).toString();

      FileIo.deleteDirectoryAndItsContentIfExists(harnessYamPath);
    } catch (IOException ex) {
      logger.error("Exception during cleaning up harness repo " + ExceptionUtils.getMessage(ex));
    }
  }

  public void createLocalGitRepo(String gitRepoPath) {
    String command = new StringBuilder(128)
                         .append("mkdir -p " + gitRepoPath + ";")
                         .append("cd " + gitRepoPath + ";")
                         .append("git init;")
                         .append("git checkout -b master;")
                         .append("git config receive.denyCurrentBranch ignore;")
                         .toString();

    executeCommand(command);
  }

  public void cleanUpRepoFolders(String repoName, String gitRepoPath, String clonePath, String verifyClonePath) {
    String command = new StringBuilder(128)
                         .append("rm -rfv " + gitRepoPath + ";")
                         .append("rm -rfv " + clonePath + ";")
                         .append("rm -rfv " + verifyClonePath + ";")
                         .toString();

    executeCommand(command);
    cleanHarnessRepo(repoName);
  }

  public void cleanUpApplication(String oldAppName, String newAppName) {
    Application app = appService.getAppByName(ACCOUNT_ID, oldAppName);
    if (app != null) {
      appService.delete(app.getUuid());
    }

    app = appService.getAppByName(ACCOUNT_ID, newAppName);
    if (app != null) {
      appService.delete(app.getUuid());
    }
  }

  public void deleteSettingAttributeByName(String settingName) {
    SettingAttribute settingAttribute = settingsService.getByName(ACCOUNT_ID, GLOBAL_APP_ID, settingName);
    if (settingAttribute != null) {
      wingsPersistence.delete(settingAttribute);
    }
  }
}
