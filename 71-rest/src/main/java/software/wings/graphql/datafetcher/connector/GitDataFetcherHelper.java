package software.wings.graphql.datafetcher.connector;

import com.google.inject.Singleton;

import io.harness.utils.RequestField;
import software.wings.beans.GitConfig;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.schema.mutation.connector.input.QLCustomCommitDetailsInput;
import software.wings.graphql.schema.mutation.connector.input.QLGitConnectorInput;
import software.wings.graphql.schema.mutation.connector.input.QLUpdateGitConnectorInput;

@Singleton
public class GitDataFetcherHelper {
  public SettingAttribute toSettingAttribute(QLGitConnectorInput input, String accountId) {
    GitConfig gitConfig = new GitConfig();
    gitConfig.setAccountId(accountId);
    handleSecrets(input.getPasswordSecretId(), input.getSshSettingId(), gitConfig);

    if (input.getUserName().isPresent()) {
      input.getUserName().getValue().ifPresent(gitConfig::setUsername);
    }
    if (input.getURL().isPresent()) {
      input.getURL().getValue().ifPresent(gitConfig::setRepoUrl);
    }
    if (input.getUrlType().isPresent()) {
      input.getUrlType().getValue().ifPresent(gitConfig::setUrlType);
    }
    if (input.getBranch().isPresent()) {
      input.getBranch().getValue().ifPresent(gitConfig::setBranch);
    }
    if (input.getGenerateWebhookUrl().isPresent()) {
      input.getGenerateWebhookUrl().getValue().ifPresent(gitConfig::setGenerateWebhookUrl);
    }
    if (input.getCustomCommitDetails().isPresent()) {
      input.getCustomCommitDetails().getValue().ifPresent(
          customCommitDetailsInput -> setCustomCommitDetails(gitConfig, customCommitDetailsInput));
    }

    SettingAttribute.Builder settingAttributeBuilder =
        SettingAttribute.Builder.aSettingAttribute().withValue(gitConfig).withAccountId(accountId).withCategory(
            SettingAttribute.SettingCategory.SETTING);

    if (input.getName().isPresent()) {
      input.getName().getValue().ifPresent(settingAttributeBuilder::withName);
    }

    return settingAttributeBuilder.build();
  }

  public void updateSettingAttribute(SettingAttribute settingAttribute, QLUpdateGitConnectorInput input) {
    GitConfig gitConfig = (GitConfig) settingAttribute.getValue();

    handleSecrets(input.getPasswordSecretId(), input.getSshSettingId(), gitConfig);

    if (input.getUserName().isPresent()) {
      input.getUserName().getValue().ifPresent(gitConfig::setUsername);
    }
    if (input.getURL().isPresent()) {
      input.getURL().getValue().ifPresent(gitConfig::setRepoUrl);
    }
    if (input.getBranch().isPresent()) {
      input.getBranch().getValue().ifPresent(gitConfig::setBranch);
    }
    if (input.getGenerateWebhookUrl().isPresent()) {
      input.getGenerateWebhookUrl().getValue().ifPresent(gitConfig::setGenerateWebhookUrl);
    }
    if (input.getCustomCommitDetails().isPresent()) {
      input.getCustomCommitDetails().getValue().ifPresent(
          customCommitDetailsInput -> setCustomCommitDetails(gitConfig, customCommitDetailsInput));
    }

    settingAttribute.setValue(gitConfig);

    if (input.getName().isPresent()) {
      input.getName().getValue().ifPresent(settingAttribute::setName);
    }
  }

  private void setCustomCommitDetails(GitConfig gitConfig, QLCustomCommitDetailsInput customCommitDetailsInput) {
    if (customCommitDetailsInput.getAuthorName().isPresent()) {
      customCommitDetailsInput.getAuthorName().getValue().ifPresent(gitConfig::setAuthorName);
    }
    if (customCommitDetailsInput.getAuthorEmailId().isPresent()) {
      customCommitDetailsInput.getAuthorEmailId().getValue().ifPresent(gitConfig::setAuthorEmailId);
    }
    if (customCommitDetailsInput.getCommitMessage().isPresent()) {
      customCommitDetailsInput.getCommitMessage().getValue().ifPresent(gitConfig::setCommitMessage);
    }
  }

  private void handleSecrets(
      RequestField<String> passwordSecretId, RequestField<String> sshSettingId, GitConfig gitConfig) {
    if (passwordSecretId.isPresent()) {
      passwordSecretId.getValue().ifPresent(s -> {
        gitConfig.setEncryptedPassword(s);
        gitConfig.setKeyAuth(false);
        gitConfig.setSshSettingId(null);
      });
    } else if (sshSettingId.isPresent()) {
      sshSettingId.getValue().ifPresent(s -> {
        gitConfig.setSshSettingId(s);
        gitConfig.setKeyAuth(true);
        gitConfig.setEncryptedPassword(null);
      });
    }
  }
}
