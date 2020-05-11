package software.wings.service.impl.yaml.handler.setting.artifactserver;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.SettingAttribute;
import software.wings.beans.config.NexusConfig;
import software.wings.beans.config.NexusConfig.Yaml;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@OwnedBy(CDC)
@Singleton
public class NexusConfigYamlHandler extends ArtifactServerYamlHandler<Yaml, NexusConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    NexusConfig nexusConfig = (NexusConfig) settingAttribute.getValue();
    Yaml yaml;
    if (nexusConfig.hasCredentials()) {
      yaml = Yaml.builder()
                 .harnessApiVersion(getHarnessApiVersion())
                 .type(nexusConfig.getType())
                 .url(nexusConfig.getNexusUrl())
                 .username(nexusConfig.getUsername())
                 .password(getEncryptedValue(nexusConfig, "password", false))
                 .build();
    } else {
      yaml = Yaml.builder()
                 .harnessApiVersion(getHarnessApiVersion())
                 .type(nexusConfig.getType())
                 .url(nexusConfig.getNexusUrl())
                 .build();
    }
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  protected SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    NexusConfig config = NexusConfig.builder()
                             .accountId(accountId)
                             .nexusUrl(yaml.getUrl())
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
