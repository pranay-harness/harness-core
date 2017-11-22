package software.wings.service.impl.yaml.handler.setting.artifactserver;

import software.wings.beans.DockerConfig;
import software.wings.beans.DockerConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
public class DockerRegistryConfigYamlHandler extends ArtifactServerYamlHandler<Yaml, DockerConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    DockerConfig dockerConfig = (DockerConfig) settingAttribute.getValue();
    return new Yaml(dockerConfig.getType(), settingAttribute.getName(), dockerConfig.getDockerRegistryUrl(),
        dockerConfig.getUsername(), getEncryptedValue(dockerConfig, "password", false));
  }

  protected SettingAttribute setWithYamlValues(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    DockerConfig config = DockerConfig.builder()
                              .accountId(accountId)
                              .dockerRegistryUrl(yaml.getUrl())
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
