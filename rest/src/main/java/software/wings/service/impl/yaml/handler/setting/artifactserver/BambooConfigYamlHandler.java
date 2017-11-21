package software.wings.service.impl.yaml.handler.setting.artifactserver;

import software.wings.beans.BambooConfig;
import software.wings.beans.BambooConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
public class BambooConfigYamlHandler extends ArtifactServerYamlHandler<Yaml, BambooConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    BambooConfig bambooConfig = (BambooConfig) settingAttribute.getValue();
    return new Yaml(bambooConfig.getType(), settingAttribute.getName(), bambooConfig.getBambooUrl(),
        bambooConfig.getUsername(), getEncryptedValue(bambooConfig, "password", false));
  }

  protected SettingAttribute setWithYamlValues(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    BambooConfig config = BambooConfig.builder()
                              .accountId(accountId)
                              .bambooUrl(yaml.getUrl())
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
