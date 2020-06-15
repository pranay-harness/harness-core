package software.wings.service.impl.yaml.handler.setting.sourcerepoprovider;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.GitConfig;
import software.wings.beans.GitConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

@OwnedBy(CDC)
@Singleton
public class GitConfigYamlHandler extends SourceRepoProviderYamlHandler<Yaml, GitConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    GitConfig gitConfig = (GitConfig) settingAttribute.getValue();

    Yaml yaml =
        Yaml.builder()
            .harnessApiVersion(getHarnessApiVersion())
            .type(gitConfig.getType())
            .url(gitConfig.getRepoUrl())
            .username(gitConfig.getUsername())
            .password(gitConfig.getEncryptedPassword() != null ? getEncryptedValue(gitConfig, "password", false) : null)
            .branch(gitConfig.getBranch())
            .keyAuth(gitConfig.isKeyAuth())
            .sshKeyName(
                gitConfig.getSshSettingId() != null ? settingsService.getSSHKeyName(gitConfig.getSshSettingId()) : null)
            .description(gitConfig.getDescription())
            .authorName(gitConfig.getAuthorName())
            .authorEmailId(gitConfig.getAuthorEmailId())
            .commitMessage(gitConfig.getCommitMessage())
            .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  public SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    GitConfig config =
        GitConfig.builder()
            .accountId(accountId)
            .repoUrl(yaml.getUrl())
            .branch(yaml.getBranch())
            .encryptedPassword(yaml.getPassword())
            .username(yaml.getUsername())
            .keyAuth(yaml.isKeyAuth())
            .sshSettingId(
                yaml.getSshKeyName() != null ? settingsService.getSSHSettingId(accountId, yaml.getSshKeyName()) : null)
            .authorName(yaml.getAuthorName())
            .authorEmailId(yaml.getAuthorEmailId())
            .commitMessage(yaml.getCommitMessage())
            .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
