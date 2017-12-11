package software.wings.service.impl.yaml.handler.setting.artifactserver;

import software.wings.beans.SettingAttribute;
import software.wings.beans.GitConfig;
import software.wings.beans.GitConfig.Yaml;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
public class GitConfigYamlHandler extends ArtifactServerYamlHandler<Yaml, GitConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    GitConfig gitConfig = (GitConfig) settingAttribute.getValue();
    return new Yaml(gitConfig.getType(), settingAttribute.getName(), gitConfig.getRepoUrl(), gitConfig.getUsername(),
        getEncryptedValue(gitConfig, "password", false), gitConfig.getBranch());
  }

  protected SettingAttribute setWithYamlValues(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    GitConfig config = GitConfig.builder()
                           .accountId(accountId)
                           .repoUrl(yaml.getUrl())
                           .branch(yaml.getBranch())
                           .password(yaml.getPassword().toCharArray())
                           .encryptedPassword(yaml.getPassword())
                           .username(yaml.getUsername())
                           .build();
    return buildSettingAttribute(accountId, yaml.getName(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
