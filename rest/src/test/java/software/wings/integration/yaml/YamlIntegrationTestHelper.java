package software.wings.integration.yaml;

import static io.harness.exception.WingsException.USER_ADMIN;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.yaml.YamlConstants.GIT_YAML_LOG_PREFIX;

import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import software.wings.beans.Application;
import software.wings.beans.Base;
import software.wings.beans.GitConfig;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.dl.WingsPersistence;
import software.wings.generator.ScmSecret;
import software.wings.generator.SecretName;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.utils.ArtifactType;
import software.wings.yaml.gitSync.YamlGitConfig;
import software.wings.yaml.gitSync.YamlGitConfig.SyncMode;

import java.util.List;

@Singleton
public class YamlIntegrationTestHelper {
  public static final String URL_FOR_GIT_SYNC = "https://github.com/wings-software/yamlIntegrationTest.git";

  public Application createApplication(String appName, String accountId, AppService appService) {
    return appService.save(anApplication()
                               .withAccountId(accountId)
                               .withName(appName)
                               .withDescription("Application for Integration Test")
                               .build());
  }

  public Service createService(
      String serviceName, Application application, ServiceResourceService serviceResourceService) {
    return serviceResourceService.save(Service.builder()
                                           .appId(application.getUuid())
                                           .name(serviceName)
                                           .artifactType(ArtifactType.WAR)
                                           .description("Service For Integration Test")
                                           .build());
  }

  public YamlGitConfig createYamlGitConfig(
      String accountId, YamlGitService yamlGitService, WingsPersistence wingsPersistence, ScmSecret scmSecret) {
    YamlGitConfig yamlGitConfig = yamlGitService.get(accountId, accountId);

    if (yamlGitConfig == null) {
      yamlGitConfig = YamlGitConfig.builder()
                          .syncMode(SyncMode.BOTH)
                          .accountId(accountId)
                          .url(URL_FOR_GIT_SYNC)
                          .username("yaml-test-harness")
                          .password(scmSecret.decryptToCharArray(new SecretName("yaml_integration_git_connector_pwd")))
                          .branchName("test")
                          .build();
      yamlGitConfig.setAppId(Base.GLOBAL_APP_ID);
    } else {
      yamlGitConfig.setSyncMode(SyncMode.BOTH);
      yamlGitConfig.setUrl(URL_FOR_GIT_SYNC);
      yamlGitConfig.setUsername("yaml-test-harness");
      yamlGitConfig.setBranchName("test");
      yamlGitConfig.setPassword(scmSecret.decryptToCharArray(new SecretName("yaml_integration_git_connector_pwd")));
    }

    return wingsPersistence.saveAndGet(YamlGitConfig.class, yamlGitConfig);
  }

  public SettingAttribute createGitConnector(
      YamlGitConfig yamlGitConfig, WingsPersistence wingsPersistence, ScmSecret scmSecret) {
    SettingAttribute gitConnector =
        aSettingAttribute()
            .withName("GitConnector" + System.currentTimeMillis())
            .withCategory(Category.CONNECTOR)
            .withAccountId(yamlGitConfig.getAccountId())
            .withValue(GitConfig.builder()
                           .accountId(yamlGitConfig.getAccountId())
                           .repoUrl(yamlGitConfig.getUrl())
                           .username(yamlGitConfig.getUsername())
                           .password(scmSecret.decryptToCharArray(new SecretName("yaml_integration_git_connector_pwd")))
                           .branch(yamlGitConfig.getBranchName())
                           .build())
            .build();

    return wingsPersistence.saveAndGet(SettingAttribute.class, gitConnector);
  }

  public void decryptGitConfig(
      GitConfig gitConfig, SecretManager secretManager, Logger logger, EncryptionService encryptionService) {
    try {
      List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(gitConfig, GLOBAL_APP_ID, null);

      encryptionService.decrypt(gitConfig, encryptionDetails);
    } catch (Exception ex) {
      logger.error(GIT_YAML_LOG_PREFIX + "Exception in processing GitTask, decryption of git config failed: ", ex);
      throw new WingsException("Decryption of git config failed: " + ex.getMessage(), USER_ADMIN)
          .addParam(ErrorCode.GIT_CONNECTION_ERROR.name(), ErrorCode.GIT_CONNECTION_ERROR);
    }
  }
}
