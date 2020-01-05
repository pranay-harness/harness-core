package software.wings.service.impl.yaml.handler.setting.artifactserver;

import com.google.inject.Singleton;

import io.harness.exception.HarnessException;
import software.wings.beans.DockerConfig;
import software.wings.beans.DockerConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@Singleton
public class DockerRegistryConfigYamlHandler extends ArtifactServerYamlHandler<Yaml, DockerConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    DockerConfig dockerConfig = (DockerConfig) settingAttribute.getValue();
    Yaml yaml;
    if (dockerConfig.hasCredentials()) {
      yaml = Yaml.builder()
                 .harnessApiVersion(getHarnessApiVersion())
                 .type(dockerConfig.getType())
                 .url(dockerConfig.getDockerRegistryUrl())
                 .username(dockerConfig.getUsername())
                 .password(getEncryptedValue(dockerConfig, "password", false))
                 .build();
    } else {
      yaml = Yaml.builder()
                 .harnessApiVersion(getHarnessApiVersion())
                 .type(dockerConfig.getType())
                 .url(dockerConfig.getDockerRegistryUrl())
                 .build();
    }

    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<Yaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    DockerConfig config = DockerConfig.builder()
                              .accountId(accountId)
                              .dockerRegistryUrl(yaml.getUrl())
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
