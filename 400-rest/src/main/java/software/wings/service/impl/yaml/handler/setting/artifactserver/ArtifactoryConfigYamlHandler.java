package software.wings.service.impl.yaml.handler.setting.artifactserver;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.SettingAttribute;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.ArtifactoryConfigYaml;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@OwnedBy(CDC)
@Singleton
public class ArtifactoryConfigYamlHandler extends ArtifactServerYamlHandler<ArtifactoryConfigYaml, ArtifactoryConfig> {
  @Override
  public ArtifactoryConfigYaml toYaml(SettingAttribute settingAttribute, String appId) {
    ArtifactoryConfig artifactoryConfig = (ArtifactoryConfig) settingAttribute.getValue();
    String encryptedPassword = null;
    if (artifactoryConfig.hasCredentials()) {
      encryptedPassword =
          getEncryptedYamlRef(artifactoryConfig.getAccountId(), artifactoryConfig.getEncryptedPassword());
    }

    ArtifactoryConfigYaml yaml = ArtifactoryConfigYaml.builder()
                                     .harnessApiVersion(getHarnessApiVersion())
                                     .type(artifactoryConfig.getType())
                                     .url(artifactoryConfig.getArtifactoryUrl())
                                     .username(artifactoryConfig.getUsername())
                                     .password(encryptedPassword)
                                     .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<ArtifactoryConfigYaml> changeContext,
      List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    ArtifactoryConfigYaml yaml = changeContext.getYaml();
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
    return ArtifactoryConfigYaml.class;
  }
}
