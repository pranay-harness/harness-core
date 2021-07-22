package software.wings.service.impl;

import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ExecutionContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

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
        .useInlineServiceDefinition(gitFileConfig.isUseInlineServiceDefinition())
        .serviceSpecFilePath(gitFileConfig.getServiceSpecFilePath())
        .taskSpecFilePath(gitFileConfig.getTaskSpecFilePath())
        .connectorName(gitFileConfig.getConnectorName())
        .repoName(gitFileConfig.getRepoName())
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

    if (gitFileConfig.getRepoName() != null) {
      gitFileConfig.setRepoName(context.renderExpression(gitFileConfig.getRepoName()).trim());
    }

    if (gitFileConfig.getServiceSpecFilePath() != null) {
      gitFileConfig.setServiceSpecFilePath(context.renderExpression(gitFileConfig.getServiceSpecFilePath().trim()));
    }

    if (gitFileConfig.getTaskSpecFilePath() != null) {
      gitFileConfig.setTaskSpecFilePath(context.renderExpression(gitFileConfig.getTaskSpecFilePath().trim()));
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

  public void validate(GitFileConfig gitFileConfig) {
    notNullCheck("gitFileConfig has to be specified", gitFileConfig, USER);
    if (isBlank(gitFileConfig.getConnectorId())) {
      throw new InvalidRequestException("Connector id cannot be empty.", USER);
    }

    if (gitFileConfig.isUseBranch() && isBlank(gitFileConfig.getBranch())) {
      throw new InvalidRequestException("Branch cannot be empty if useBranch is selected.", USER);
    }

    if (!gitFileConfig.isUseBranch() && isBlank(gitFileConfig.getCommitId())) {
      throw new InvalidRequestException("CommitId cannot be empty if useBranch is not selected.", USER);
    }

    SettingAttribute settingAttribute = settingsService.get(gitFileConfig.getConnectorId());
    if (null == settingAttribute) {
      throw new InvalidRequestException("Invalid git connector provided.", USER);
    }

    GitConfig gitConfig = (GitConfig) settingAttribute.getValue();
    if (GitConfig.UrlType.ACCOUNT == gitConfig.getUrlType() && isBlank(gitFileConfig.getRepoName())) {
      throw new InvalidRequestException("Repository name not provided for Account level git connector.", USER);
    }
  }
}
