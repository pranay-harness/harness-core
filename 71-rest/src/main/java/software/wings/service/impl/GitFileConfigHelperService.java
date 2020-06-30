package software.wings.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.GitFileConfig;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ExecutionContext;

import java.util.stream.Collectors;

@Singleton
@Slf4j
public class GitFileConfigHelperService {
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

  public GitFileConfig renderGitFileConfig(ExecutionContext context, GitFileConfig gitFileConfig) {
    if (context == null) {
      return gitFileConfig;
    }

    if (gitFileConfig.getCommitId() != null) {
      gitFileConfig.setCommitId(context.renderExpression(gitFileConfig.getCommitId()).trim());
    }

    if (gitFileConfig.getBranch() != null) {
      gitFileConfig.setBranch(context.renderExpression(gitFileConfig.getBranch()).trim());
    }

    if (gitFileConfig.getFilePath() != null) {
      gitFileConfig.setFilePath(context.renderExpression(gitFileConfig.getFilePath()).trim());
    }

    if (gitFileConfig.getFilePathList() != null) {
      gitFileConfig.setFilePathList(gitFileConfig.getFilePathList()
                                        .stream()
                                        .map(context::renderExpression)
                                        .map(String::trim)
                                        .collect(Collectors.toList()));
    }

    return gitFileConfig;
  }
}
