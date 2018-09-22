package software.wings.service.impl.yaml.handler.setting.artifactserver;

import com.google.inject.Singleton;

import software.wings.beans.GitConfig;
import software.wings.beans.GitConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@Singleton
public class GitConfigYamlHandler extends ArtifactServerYamlHandler<Yaml, GitConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    GitConfig gitConfig = (GitConfig) settingAttribute.getValue();

    return Yaml.builder()
        .harnessApiVersion(getHarnessApiVersion())
        .type(gitConfig.getType())
        .url(gitConfig.getRepoUrl())
        .username(gitConfig.getUsername())
        .password(getEncryptedValue(gitConfig, "password", false))
        .branch(gitConfig.getBranch())
        .build();
  }

  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<Yaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    GitConfig config = GitConfig.builder()
                           .accountId(accountId)
                           .repoUrl(yaml.getUrl())
                           .branch(yaml.getBranch())
                           .encryptedPassword(yaml.getPassword())
                           .username(yaml.getUsername())
                           .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
