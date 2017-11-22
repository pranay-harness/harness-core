package software.wings.service.impl.yaml.handler.setting.artifactserver;

import software.wings.beans.config.NexusConfig;
import software.wings.beans.config.NexusConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
public class NexusConfigYamlHandler extends ArtifactServerYamlHandler<Yaml, NexusConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    NexusConfig nexusConfig = (NexusConfig) settingAttribute.getValue();
    return new Yaml(nexusConfig.getType(), settingAttribute.getName(), nexusConfig.getNexusUrl(),
        nexusConfig.getUsername(), getEncryptedValue(nexusConfig, "password", false));
  }

  protected SettingAttribute setWithYamlValues(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    NexusConfig config = NexusConfig.builder()
                             .accountId(accountId)
                             .nexusUrl(yaml.getUrl())
                             .password(null)
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
