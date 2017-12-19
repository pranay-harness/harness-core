package software.wings.service.impl.yaml.handler.setting.artifactserver;

import software.wings.beans.GitConfig;
import software.wings.beans.GitConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.io.IOException;
import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
public class GitConfigYamlHandler extends ArtifactServerYamlHandler<Yaml, GitConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    GitConfig gitConfig = (GitConfig) settingAttribute.getValue();
    return new Yaml(gitConfig.getType(), gitConfig.getRepoUrl(), gitConfig.getUsername(),
        getEncryptedValue(gitConfig, "password", false), gitConfig.getBranch());
  }

  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<Yaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    char[] decryptedPassword;
    try {
      decryptedPassword = secretManager.decryptYamlRef(yaml.getPassword());
    } catch (IllegalAccessException | IOException e) {
      throw new HarnessException("Exception while decrypting the password ref:" + yaml.getPassword());
    }

    GitConfig config = GitConfig.builder()
                           .accountId(accountId)
                           .repoUrl(yaml.getUrl())
                           .branch(yaml.getBranch())
                           .password(decryptedPassword)
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
