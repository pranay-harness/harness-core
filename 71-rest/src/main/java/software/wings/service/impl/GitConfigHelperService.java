package software.wings.service.impl;

import static io.harness.exception.WingsException.USER;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.yaml.YamlConstants.GIT_YAML_LOG_PREFIX;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.GitConfig;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.yaml.GitCommand.GitCommandType;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitCommandExecutionResponse.GitCommandStatus;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class GitConfigHelperService {
  private static final Logger logger = LoggerFactory.getLogger(GitConfigHelperService.class);

  @Inject private DelegateService delegateService;
  @Inject private SettingsService settingsService;
  @Inject private ManagerDecryptionService managerDecryptionService;
  @Inject private SecretManager secretManager;

  public void validateGitConfig(GitConfig gitConfig, List<EncryptedDataDetail> encryptionDetails) {
    if (gitConfig.isKeyAuth()) {
      if (gitConfig.getSshSettingAttribute() == null) {
        throw new InvalidRequestException("SSH key can not be empty");
      }
    } else {
      if (gitConfig.getUsername() == null || gitConfig.getPassword() == null) {
        throw new InvalidRequestException("Username and password can not be empty", USER);
      }
    }

    try {
      GitCommandExecutionResponse gitCommandExecutionResponse = delegateService.executeTask(
          aDelegateTask()
              .withTaskType(TaskType.GIT_COMMAND)
              .withAccountId(gitConfig.getAccountId())
              .withAppId(GLOBAL_APP_ID)
              .withAsync(false)
              .withTimeout(TimeUnit.SECONDS.toMillis(60))
              .withParameters(new Object[] {GitCommandType.VALIDATE, gitConfig, encryptionDetails})
              .build());
      logger.info(GIT_YAML_LOG_PREFIX + "GitConfigValidation [{}]", gitCommandExecutionResponse);

      if (gitCommandExecutionResponse.getGitCommandStatus().equals(GitCommandStatus.FAILURE)) {
        throw new WingsException(ErrorCode.INVALID_CREDENTIAL).addParam("message", "Invalid git credentials.");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new InvalidRequestException(
          "Thread was interrupted. Please try again. " + e.getMessage(), WingsException.USER);
    }
  }

  /**
   * If GitConfig has keyAuth enabled, and fetch SshKeySettingAttribute using sshSettingId
   * and set it into gitConfig.
   * @param gitConfig
   */
  public void setSshKeySettingAttributeIfNeeded(GitConfig gitConfig) {
    if (gitConfig.isKeyAuth() && StringUtils.isNotBlank(gitConfig.getSshSettingId())) {
      SettingAttribute settingAttribute = settingsService.get(gitConfig.getSshSettingId());
      if (settingAttribute != null && settingAttribute.getValue() != null) {
        HostConnectionAttributes attributeValue = (HostConnectionAttributes) settingAttribute.getValue();
        List<EncryptedDataDetail> encryptionDetails =
            secretManager.getEncryptionDetails(attributeValue, GLOBAL_APP_ID, null);
        managerDecryptionService.decrypt(attributeValue, encryptionDetails);
        gitConfig.setSshSettingAttribute(settingAttribute);
      }
    }
  }
}
