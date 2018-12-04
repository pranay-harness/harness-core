package software.wings.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.GitFileConfig;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.SettingsService;

@Singleton
public class GitFileConfigHelperService {
  private static final Logger logger = LoggerFactory.getLogger(GitFileConfigHelperService.class);

  @Inject private SettingsService settingsService;

  public GitFileConfig getGitFileConfigFromYaml(String accountId, String appId, GitFileConfig gitFileConfig) {
    if (gitFileConfig == null) {
      return null;
    }

    GitFileConfig newGitFileConfig = createNewGitFileConfig(gitFileConfig);

    SettingAttribute settingAttribute =
        settingsService.getByName(accountId, appId, newGitFileConfig.getConnectorName());
    if (settingAttribute == null) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam("args", "No git connector exists with name " + newGitFileConfig.getConnectorName());
    }

    newGitFileConfig.setConnectorId(settingAttribute.getUuid());
    newGitFileConfig.setConnectorName(null);

    return newGitFileConfig;
  }

  public GitFileConfig getGitFileConfigForToYaml(GitFileConfig gitFileConfig) {
    if (gitFileConfig == null) {
      return null;
    }

    GitFileConfig newGitFileConfig = createNewGitFileConfig(gitFileConfig);

    SettingAttribute settingAttribute = settingsService.get(newGitFileConfig.getConnectorId());
    if (settingAttribute == null) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam("args", "No git connector exists with id " + newGitFileConfig.getConnectorId());
    }

    newGitFileConfig.setConnectorId(null);
    newGitFileConfig.setConnectorName(settingAttribute.getName());

    return newGitFileConfig;
  }

  private GitFileConfig createNewGitFileConfig(GitFileConfig gitFileConfig) {
    return GitFileConfig.builder()
        .connectorId(gitFileConfig.getConnectorId())
        .branch(gitFileConfig.getBranch())
        .filePath(gitFileConfig.getFilePath())
        .commitId(gitFileConfig.getCommitId())
        .useBranch(gitFileConfig.isUseBranch())
        .connectorName(gitFileConfig.getConnectorName())
        .build();
  }
}
