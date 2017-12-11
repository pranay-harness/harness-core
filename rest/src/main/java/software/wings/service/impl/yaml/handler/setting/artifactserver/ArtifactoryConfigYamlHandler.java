package software.wings.service.impl.yaml.handler.setting.artifactserver;

import software.wings.beans.SettingAttribute;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.ArtifactoryConfig.Yaml;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
public class ArtifactoryConfigYamlHandler extends ArtifactServerYamlHandler<Yaml, ArtifactoryConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    ArtifactoryConfig artifactoryConfig = (ArtifactoryConfig) settingAttribute.getValue();
    String encryptedPassword = null;
    if (artifactoryConfig.getUsername() != null) {
      encryptedPassword = getEncryptedValue(artifactoryConfig, "password", false);
    }

    return new Yaml(artifactoryConfig.getType(), settingAttribute.getName(), artifactoryConfig.getArtifactoryUrl(),
        artifactoryConfig.getUsername(), encryptedPassword);
  }

  protected SettingAttribute setWithYamlValues(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    ArtifactoryConfig config = ArtifactoryConfig.builder()
                                   .accountId(accountId)
                                   .artifactoryUrl(yaml.getUrl())
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
