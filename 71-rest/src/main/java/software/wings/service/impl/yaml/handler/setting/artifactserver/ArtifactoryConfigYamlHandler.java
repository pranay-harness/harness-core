package software.wings.service.impl.yaml.handler.setting.artifactserver;

import com.google.inject.Singleton;

import software.wings.beans.SettingAttribute;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.ArtifactoryConfig.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@Singleton
public class ArtifactoryConfigYamlHandler extends ArtifactServerYamlHandler<Yaml, ArtifactoryConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    ArtifactoryConfig artifactoryConfig = (ArtifactoryConfig) settingAttribute.getValue();
    String encryptedPassword = null;
    if (artifactoryConfig.getUsername() != null) {
      encryptedPassword = getEncryptedValue(artifactoryConfig, "password", false);
    }

    return Yaml.builder()
        .harnessApiVersion(getHarnessApiVersion())
        .type(artifactoryConfig.getType())
        .url(artifactoryConfig.getArtifactoryUrl())
        .username(artifactoryConfig.getUsername())
        .password(encryptedPassword)
        .build();
  }

  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<Yaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    ArtifactoryConfig config = ArtifactoryConfig.builder()
                                   .accountId(accountId)
                                   .artifactoryUrl(yaml.getUrl())
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
