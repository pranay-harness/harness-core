package software.wings.service.impl.yaml.handler.setting.artifactserver;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.GitConfig;
import software.wings.beans.GitConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@OwnedBy(CDC)
@Singleton
public class GitConfigYamlHandler extends ArtifactServerYamlHandler<Yaml, GitConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    GitConfig gitConfig = (GitConfig) settingAttribute.getValue();

    Yaml yaml = Yaml.builder()
                    .harnessApiVersion(getHarnessApiVersion())
                    .type(gitConfig.getType())
                    .url(gitConfig.getRepoUrl())
                    .username(gitConfig.getUsername())
                    .password(getEncryptedValue(gitConfig, "password", false))
                    .branch(gitConfig.getBranch())
                    .keyAuth(gitConfig.isKeyAuth())
                    .sshSettingId(gitConfig.getSshSettingId())
                    .description(gitConfig.getDescription())
                    .authorName(gitConfig.getAuthorName())
                    .authorEmailId(gitConfig.getAuthorEmailId())
                    .commitMessage(gitConfig.getCommitMessage())
                    .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  protected SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    GitConfig config = GitConfig.builder()
                           .accountId(accountId)
                           .repoUrl(yaml.getUrl())
                           .branch(yaml.getBranch())
                           .encryptedPassword(yaml.getPassword())
                           .username(yaml.getUsername())
                           .keyAuth(yaml.isKeyAuth())
                           .sshSettingId(yaml.getSshSettingId())
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
